package com.fincoach.core.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.repository.entity.MarketSecurity;

/**
 * 市场数据服务接口
 */
public interface MarketService {
    
    /**
     * 分页查询证券数据
     * @param page 分页对象
     * @param type 证券类型
     * @param keyword 搜索关键词
     * @return 分页结果
     */
    IPage<MarketSecurity> getSecurities(Page<MarketSecurity> page, String type, String keyword);
}
