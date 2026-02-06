package com.fincoach.core.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fincoach.core.repository.entity.MarketSecurity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 市场证券 Mapper
 */
@Mapper
public interface MarketSecurityMapper extends BaseMapper<MarketSecurity> {
}
