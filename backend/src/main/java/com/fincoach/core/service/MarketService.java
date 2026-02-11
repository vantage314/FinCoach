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

    /**
     * 根据代码查询单个证券详情
     * @param code 证券代码
     * @return 证券实体
     */
    MarketSecurity getSecurityByCode(String code);

    /**
     * 获取公司简介
     * @param code 证券代码
     */
    Object getCompanyProfile(String code);

    /**
     * 获取公司公告
     * @param code 证券代码
     */
    Object getCompanyNotices(String code);

    /**
     * 获取公司财报
     * @param code 证券代码
     */
    Object getFinancialReports(String code);

    /**
     * 获取新闻列表
     * @param limit 限制条数
     */
    Object getNewsList(Integer limit);
}
