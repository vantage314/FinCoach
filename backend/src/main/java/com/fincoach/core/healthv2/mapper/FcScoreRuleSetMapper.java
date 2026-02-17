package com.fincoach.core.healthv2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fincoach.core.healthv2.entity.FcScoreRuleSetEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FcScoreRuleSetMapper extends BaseMapper<FcScoreRuleSetEntity> {

    @Select("SELECT * FROM fc_score_rule_set WHERE enabled = 1 ORDER BY version DESC, published_at DESC LIMIT 1")
    FcScoreRuleSetEntity selectActive();

    @Select("SELECT MAX(version) FROM fc_score_rule_set WHERE code = #{code}")
    Integer selectMaxVersion(String code);
}
