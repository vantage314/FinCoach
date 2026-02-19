package com.fincoach.core.healthv2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fincoach.core.healthv2.entity.FcAdviceRuleSetEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FcAdviceRuleSetMapper extends BaseMapper<FcAdviceRuleSetEntity> {

    @Select("SELECT * FROM fc_advice_rule_set WHERE enabled = 1 ORDER BY version DESC, created_at DESC LIMIT 1")
    FcAdviceRuleSetEntity selectActive();

    @Select("SELECT MAX(version) FROM fc_advice_rule_set WHERE code = #{code}")
    Integer selectMaxVersion(String code);
}
