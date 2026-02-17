package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.healthv2.dto.admin.AdminScoreRuleParamDTO;
import com.fincoach.core.healthv2.dto.admin.AdminScoreRulePublishRequest;
import com.fincoach.core.healthv2.dto.admin.AdminScoreRuleSaveParamsRequest;
import com.fincoach.core.healthv2.dto.admin.AdminScoreRuleSetDTO;
import com.fincoach.core.healthv2.entity.FcScoreRuleParamEntity;
import com.fincoach.core.healthv2.entity.FcScoreRuleSetEntity;
import com.fincoach.core.healthv2.rules.ScoreRuleParamValue;
import com.fincoach.core.healthv2.rules.ScoreRuleSnapshot;
import com.fincoach.core.healthv2.service.ScoreRuleSetService;
import com.fincoach.core.rbac.RbacPermissionCodes;
import com.fincoach.core.security.AdminOnly;
import com.fincoach.core.security.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/score-rules")
@Tag(name = "Admin-ScoreRule", description = "评分规则管理")
@AdminOnly
public class ScoreRuleController {

    private final ScoreRuleSetService scoreRuleSetService;

    public ScoreRuleController(ScoreRuleSetService scoreRuleSetService) {
        this.scoreRuleSetService = scoreRuleSetService;
    }

    @GetMapping("/active")
    @Operation(summary = "当前生效规则集 + 参数")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_VIEW)
    public Result<AdminScoreRuleSetDTO> getActive() {
        ScoreRuleSnapshot snapshot = scoreRuleSetService.getActiveSnapshot();
        FcScoreRuleSetEntity active = scoreRuleSetService.getActiveRuleSet();
        List<FcScoreRuleParamEntity> params = active == null ? new ArrayList<>() : scoreRuleSetService.listParams(active.getId());
        AdminScoreRuleSetDTO dto = toDto(active, snapshot, params);
        return Result.success(dto);
    }

    @PostMapping("/save-params")
    @Operation(summary = "批量保存参数（可自动创建新版本）")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_EDIT)
    public Result<Long> saveParams(@RequestBody AdminScoreRuleSaveParamsRequest request) {
        Long ruleSetId = request == null ? null : request.getRuleSetId();
        if (ruleSetId == null) {
            FcScoreRuleSetEntity draft = scoreRuleSetService.draftNewVersion(
                    request == null ? null : request.getFromVersion());
            ruleSetId = draft == null ? null : draft.getId();
        }
        if (ruleSetId == null) {
            return Result.error(400, "ruleSetId 不能为空");
        }
        List<FcScoreRuleParamEntity> entities = toEntities(ruleSetId, request == null ? null : request.getParams());
        scoreRuleSetService.upsertParams(ruleSetId, entities);
        return Result.success(ruleSetId);
    }

    @PostMapping("/publish")
    @Operation(summary = "发布规则集（立即生效）")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_PUBLISH)
    public Result<String> publish(@RequestBody AdminScoreRulePublishRequest request) {
        if (request == null || request.getRuleSetId() == null) {
            return Result.error(400, "ruleSetId 不能为空");
        }
        scoreRuleSetService.publish(request.getRuleSetId());
        return Result.success("published");
    }

    @PostMapping("/reload")
    @Operation(summary = "手动重载规则集快照")
    @Permission(RbacPermissionCodes.ADMIN_SCORE_RULE_EDIT)
    public Result<String> reload() {
        scoreRuleSetService.reload();
        return Result.success("reloaded");
    }

    private AdminScoreRuleSetDTO toDto(FcScoreRuleSetEntity active,
                                       ScoreRuleSnapshot snapshot,
                                       List<FcScoreRuleParamEntity> params) {
        AdminScoreRuleSetDTO dto = new AdminScoreRuleSetDTO();
        if (active != null) {
            dto.setId(active.getId());
            dto.setCode(active.getCode());
            dto.setName(active.getName());
            dto.setVersion(active.getVersion());
            dto.setEnabled(active.getEnabled());
            dto.setPublishedAt(active.getPublishedAt());
        } else {
            dto.setCode(snapshot.getCode());
            dto.setVersion(snapshot.getVersion());
            dto.setEnabled(0);
        }
        dto.setSource(snapshot.getSource());
        dto.setMissingParams(snapshot.getMissingParams());
        dto.setWarnings(snapshot.getWarnings());
        dto.setParams(mergeParams(snapshot.getParams(), params));
        return dto;
    }

    private List<AdminScoreRuleParamDTO> mergeParams(Map<String, ScoreRuleParamValue> snapshotParams,
                                                     List<FcScoreRuleParamEntity> entities) {
        List<AdminScoreRuleParamDTO> result = new ArrayList<>();
        if (snapshotParams == null) return result;
        for (ScoreRuleParamValue value : snapshotParams.values()) {
            AdminScoreRuleParamDTO dto = new AdminScoreRuleParamDTO();
            dto.setParamKey(value.getKey());
            dto.setParamValue(value.getRawValue());
            dto.setValueType(value.getValueType() == null ? null : value.getValueType().name());
            dto.setMinValue(value.getMinValue() == null ? null : value.getMinValue().toPlainString());
            dto.setMaxValue(value.getMaxValue() == null ? null : value.getMaxValue().toPlainString());
            dto.setDescription(value.getDescription());
            dto.setSource(value.getSource());
            result.add(dto);
        }
        if (entities != null) {
            for (FcScoreRuleParamEntity e : entities) {
                for (AdminScoreRuleParamDTO dto : result) {
                    if (dto.getParamKey() != null && dto.getParamKey().equals(e.getParamKey())) {
                        dto.setId(e.getId());
                        dto.setRuleSetId(e.getRuleSetId());
                        dto.setUpdatedAt(e.getUpdatedAt());
                    }
                }
            }
        }
        return result;
    }

    private List<FcScoreRuleParamEntity> toEntities(Long ruleSetId, List<AdminScoreRuleParamDTO> params) {
        List<FcScoreRuleParamEntity> result = new ArrayList<>();
        if (params == null) return result;
        for (AdminScoreRuleParamDTO p : params) {
            if (p == null || p.getParamKey() == null) continue;
            FcScoreRuleParamEntity entity = new FcScoreRuleParamEntity();
            entity.setRuleSetId(ruleSetId);
            entity.setParamKey(p.getParamKey());
            entity.setParamValue(p.getParamValue());
            entity.setValueType(p.getValueType());
            entity.setMinValue(p.getMinValue());
            entity.setMaxValue(p.getMaxValue());
            entity.setDescription(p.getDescription());
            result.add(entity);
        }
        return result;
    }
}
