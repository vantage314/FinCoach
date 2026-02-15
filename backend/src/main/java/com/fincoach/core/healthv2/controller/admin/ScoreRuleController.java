package com.fincoach.core.healthv2.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fincoach.core.common.Result;
import com.fincoach.core.common.UserContext;
import com.fincoach.core.healthv2.entity.FcScoreRuleVersionEntity;
import com.fincoach.core.healthv2.mapper.FcScoreRuleVersionMapper;
import com.fincoach.core.healthv2.service.AuditService;
import com.fincoach.core.healthv2.util.ConfigJsonHelper;
import com.fincoach.core.healthv2.util.HealthV2ConfigDefaults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/score-rules")
@Tag(name = "Admin-ScoreRule", description = "评分规则版本管理")
public class ScoreRuleController {

    @Autowired
    private FcScoreRuleVersionMapper ruleMapper;
    @Autowired
    private AuditService auditService;
    @Autowired
    private ConfigJsonHelper configJsonHelper;

    @GetMapping
    @Operation(summary = "全部版本列表")
    public Result<List<FcScoreRuleVersionEntity>> list() {
        List<FcScoreRuleVersionEntity> list = ruleMapper.selectList(
                new LambdaQueryWrapper<FcScoreRuleVersionEntity>()
                        .orderByDesc(FcScoreRuleVersionEntity::getCreatedAt));
        Long actorUserId = UserContext.getCurrentUserId();
        for (FcScoreRuleVersionEntity entity : list) {
            entity.setWeightsJson(configJsonHelper.parseOrDefault(
                    entity.getWeightsJson(),
                    HealthV2ConfigDefaults.DEFAULT_SCORE_RULE_WEIGHTS_JSON,
                    actorUserId,
                    "SCORE_RULE",
                    entity.getId(),
                    "weightsJson"));
            entity.setThresholdsJson(configJsonHelper.parseOrDefault(
                    entity.getThresholdsJson(),
                    HealthV2ConfigDefaults.DEFAULT_SCORE_RULE_THRESHOLDS_JSON,
                    actorUserId,
                    "SCORE_RULE",
                    entity.getId(),
                    "thresholdsJson"));
        }
        return Result.success(list);
    }

    @GetMapping("/published")
    @Operation(summary = "当前发布版本")
    public Result<FcScoreRuleVersionEntity> getPublished() {
        FcScoreRuleVersionEntity entity = ruleMapper.selectOne(
                new LambdaQueryWrapper<FcScoreRuleVersionEntity>()
                        .eq(FcScoreRuleVersionEntity::getStatus, "PUBLISHED")
                        .last("LIMIT 1"));
        if (entity != null) {
            Long actorUserId = UserContext.getCurrentUserId();
            entity.setWeightsJson(configJsonHelper.parseOrDefault(
                    entity.getWeightsJson(),
                    HealthV2ConfigDefaults.DEFAULT_SCORE_RULE_WEIGHTS_JSON,
                    actorUserId,
                    "SCORE_RULE",
                    entity.getId(),
                    "weightsJson"));
            entity.setThresholdsJson(configJsonHelper.parseOrDefault(
                    entity.getThresholdsJson(),
                    HealthV2ConfigDefaults.DEFAULT_SCORE_RULE_THRESHOLDS_JSON,
                    actorUserId,
                    "SCORE_RULE",
                    entity.getId(),
                    "thresholdsJson"));
        }
        return Result.success(entity);
    }

    @PostMapping
    @Operation(summary = "创建新版本(DRAFT)")
    public Result<FcScoreRuleVersionEntity> create(@RequestBody FcScoreRuleVersionEntity dto) {
        Result<String> jsonCheck = validateScoreRuleJson(dto);
        if (jsonCheck != null) return Result.error(jsonCheck.getCode(), jsonCheck.getMessage());
        dto.setId(null);
        dto.setStatus("DRAFT");
        dto.setCreatedBy(UserContext.getCurrentUserId());
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        ruleMapper.insert(dto);
        auditService.log(UserContext.getCurrentUserId(), "CREATE_SCORE_RULE", "SCORE_RULE", dto.getId(), null, dto);
        return Result.success(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑版本")
    public Result<String> update(@PathVariable Long id, @RequestBody FcScoreRuleVersionEntity dto) {
        FcScoreRuleVersionEntity existing = ruleMapper.selectById(id);
        if (existing == null) return Result.error(404, "版本不存在");
        if ("PUBLISHED".equals(existing.getStatus())) return Result.error(400, "已发布版本不可直接编辑，请创建新版本");

        Result<String> jsonCheck = validateScoreRuleJson(dto);
        if (jsonCheck != null) return jsonCheck;
        dto.setId(id);
        dto.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(dto);
        auditService.log(UserContext.getCurrentUserId(), "UPDATE_SCORE_RULE", "SCORE_RULE", id, existing, dto);
        return Result.success("更新成功");
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布版本（自动下线旧版）")
    @Transactional
    public Result<String> publish(@PathVariable Long id) {
        FcScoreRuleVersionEntity target = ruleMapper.selectById(id);
        if (target == null) return Result.error(404, "版本不存在");

        // 锁住当前发布记录，避免并发发布
        ruleMapper.selectPublishedForUpdate();

        LocalDateTime now = LocalDateTime.now();
        // 将所有 PUBLISHED 改为 DRAFT
        ruleMapper.update(
                null,
                new LambdaUpdateWrapper<FcScoreRuleVersionEntity>()
                        .eq(FcScoreRuleVersionEntity::getStatus, "PUBLISHED")
                        .set(FcScoreRuleVersionEntity::getStatus, "DRAFT")
                        .set(FcScoreRuleVersionEntity::getUpdatedAt, now));

        target.setStatus("PUBLISHED");
        target.setUpdatedAt(now);
        ruleMapper.updateById(target);

        Long publishedCount = ruleMapper.selectCount(
                new LambdaQueryWrapper<FcScoreRuleVersionEntity>()
                        .eq(FcScoreRuleVersionEntity::getStatus, "PUBLISHED"));
        if (publishedCount == null || publishedCount != 1) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(409, "发布冲突，请重试");
        }

        auditService.log(UserContext.getCurrentUserId(), "PUBLISH_SCORE_RULE", "SCORE_RULE", id, null, target);
        log.info("[Admin] 评分规则发布: version={}, by={}", target.getVersion(), UserContext.getCurrentUserId());
        return Result.success("版本 " + target.getVersion() + " 已发布");
    }

    @PostMapping("/{id}/rollback")
    @Operation(summary = "回滚到指定版本")
    @Transactional
    public Result<String> rollback(@PathVariable Long id) {
        return publish(id); // 回滚即重新发布
    }

    private Result<String> validateScoreRuleJson(FcScoreRuleVersionEntity dto) {
        try {
            configJsonHelper.validateJsonOrThrow(dto.getWeightsJson(), "weightsJson");
            configJsonHelper.validateJsonOrThrow(dto.getThresholdsJson(), "thresholdsJson");
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
        return null;
    }
}
