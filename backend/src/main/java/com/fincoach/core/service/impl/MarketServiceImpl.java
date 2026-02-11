package com.fincoach.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fincoach.core.repository.entity.MarketSecurity;
import com.fincoach.core.repository.mapper.MarketSecurityMapper;
import com.fincoach.core.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketServiceImpl implements MarketService {

    @Autowired
    private MarketSecurityMapper marketSecurityMapper;

    @Override
    public IPage<MarketSecurity> getSecurities(Page<MarketSecurity> page, String type, String keyword) {
        LambdaQueryWrapper<MarketSecurity> queryWrapper = new LambdaQueryWrapper<>();
        
        // 类型过滤
        if (StringUtils.hasText(type) && !"all".equals(type)) {
            queryWrapper.eq(MarketSecurity::getType, type);
        }
        
        // 关键词搜索 (匹配名称或代码)
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(MarketSecurity::getName, keyword)
                    .or()
                    .like(MarketSecurity::getCode, keyword));
        }
        
        // 默认按涨跌幅倒序 (模拟热门)
        queryWrapper.orderByDesc(MarketSecurity::getChangePercent);
        
        return marketSecurityMapper.selectPage(page, queryWrapper);
    }

    @Override
    public MarketSecurity getSecurityByCode(String code) {
        LambdaQueryWrapper<MarketSecurity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketSecurity::getCode, code);
        return marketSecurityMapper.selectOne(wrapper);
    }

    @Autowired
    private com.fincoach.core.repository.mapper.CompanyProfileMapper companyProfileMapper;
    @Autowired
    private com.fincoach.core.repository.mapper.CompanyNoticeMapper companyNoticeMapper;
    @Autowired
    private com.fincoach.core.repository.mapper.FinancialReportMapper financialReportMapper;
    @Autowired
    private com.fincoach.core.repository.mapper.FinancialNewsMapper financialNewsMapper;

    @Override
    public Object getCompanyProfile(String code) {
        LambdaQueryWrapper<com.fincoach.core.repository.entity.CompanyProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.fincoach.core.repository.entity.CompanyProfile::getStockCode, code);
        return companyProfileMapper.selectOne(wrapper);
    }

    @Override
    public Object getCompanyNotices(String code) {
        LambdaQueryWrapper<com.fincoach.core.repository.entity.CompanyNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.fincoach.core.repository.entity.CompanyNotice::getStockCode, code)
               .orderByDesc(com.fincoach.core.repository.entity.CompanyNotice::getPublishDate)
               .last("LIMIT 10");
        return companyNoticeMapper.selectList(wrapper);
    }

    @Override
    public Object getFinancialReports(String code) {
        LambdaQueryWrapper<com.fincoach.core.repository.entity.FinancialReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(com.fincoach.core.repository.entity.FinancialReport::getStockCode, code)
               .orderByDesc(com.fincoach.core.repository.entity.FinancialReport::getId)
               .last("LIMIT 5");
        return financialReportMapper.selectList(wrapper);
    }

    @Override
    public Object getNewsList(Integer limit) {
        LambdaQueryWrapper<com.fincoach.core.repository.entity.FinancialNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(com.fincoach.core.repository.entity.FinancialNews::getPublishTime)
               .last("LIMIT " + (limit != null ? limit : 20));
        return financialNewsMapper.selectList(wrapper);
    }
}
