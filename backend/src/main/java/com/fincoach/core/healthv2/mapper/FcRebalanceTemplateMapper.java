package com.fincoach.core.healthv2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fincoach.core.healthv2.entity.FcRebalanceTemplateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FcRebalanceTemplateMapper extends BaseMapper<FcRebalanceTemplateEntity> {

    @Select("SELECT * FROM fc_rebalance_template WHERE enabled = 1 ORDER BY version DESC, published_at DESC LIMIT 1")
    FcRebalanceTemplateEntity selectActive();

    @Select("SELECT MAX(version) FROM fc_rebalance_template WHERE code = #{code}")
    Integer selectMaxVersion(String code);
}
