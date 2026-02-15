package com.fincoach.core.healthv2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fincoach.core.healthv2.entity.FcScoreRuleVersionEntity;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FcScoreRuleVersionMapper extends BaseMapper<FcScoreRuleVersionEntity> {

    @Select("SELECT * FROM fc_score_rule_version WHERE status = 'PUBLISHED' LIMIT 1 FOR UPDATE")
    FcScoreRuleVersionEntity selectPublishedForUpdate();
}
