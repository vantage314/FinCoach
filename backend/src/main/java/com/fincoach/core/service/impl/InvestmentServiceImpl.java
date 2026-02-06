package com.fincoach.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fincoach.core.repository.entity.CompanyProfile;
import com.fincoach.core.repository.entity.FinancialNews;
import com.fincoach.core.repository.entity.UserWatchlist;
import com.fincoach.core.repository.mapper.CompanyProfileMapper;
import com.fincoach.core.repository.mapper.FinancialNewsMapper;
import com.fincoach.core.repository.mapper.UserWatchlistMapper;
import com.fincoach.core.service.InvestmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestmentServiceImpl implements InvestmentService {

    private final FinancialNewsMapper financialNewsMapper;
    private final CompanyProfileMapper companyProfileMapper;
    private final UserWatchlistMapper userWatchlistMapper;

    @Override
    public List<FinancialNews> getLatestNews(int limit) {
        return financialNewsMapper.selectList(
            new LambdaQueryWrapper<FinancialNews>()
                .orderByDesc(FinancialNews::getPublishTime)
                .last("LIMIT " + limit)
        );
    }

    @Override
    public FinancialNews getNewsDetail(Long id) {
        return financialNewsMapper.selectById(id);
    }

    @Override
    public CompanyProfile getCompanyProfile(String code) {
        return companyProfileMapper.selectById(code);
    }

    @Override
    public List<String> getWatchlist(Long userId) {
        return userWatchlistMapper.selectList(
            new LambdaQueryWrapper<UserWatchlist>().eq(UserWatchlist::getUserId, userId)
        ).stream().map(UserWatchlist::getStockCode).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleWatchlist(Long userId, String code) {
        LambdaQueryWrapper<UserWatchlist> queryWrapper = new LambdaQueryWrapper<UserWatchlist>()
                .eq(UserWatchlist::getUserId, userId)
                .eq(UserWatchlist::getStockCode, code);
        
        Long count = userWatchlistMapper.selectCount(queryWrapper);
        if (count > 0) {
            userWatchlistMapper.delete(queryWrapper);
            return false; // Removed
        } else {
            UserWatchlist watchlist = new UserWatchlist();
            watchlist.setUserId(userId);
            watchlist.setStockCode(code);
            watchlist.setCreateTime(java.time.LocalDateTime.now());
            userWatchlistMapper.insert(watchlist);
            return true; // Added
        }
    }
}
