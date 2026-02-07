package com.fincoach.core.service;

import com.fincoach.core.repository.entity.CompanyProfile;
import com.fincoach.core.repository.entity.FinancialNews;
import com.fincoach.core.repository.entity.FinancialReport;
import com.fincoach.core.repository.entity.CompanyNotice;
import java.util.List;

public interface InvestmentService {
    List<FinancialNews> getLatestNews(int limit);
    FinancialNews getNewsDetail(Long id);
    CompanyProfile getCompanyProfile(String code);
    List<String> getWatchlist(Long userId);
    boolean toggleWatchlist(Long userId, String code);
    
    // 新增：财务报表和公告查询
    List<FinancialReport> getFinancialReports(String code);
    List<CompanyNotice> getCompanyNotices(String code);
}
