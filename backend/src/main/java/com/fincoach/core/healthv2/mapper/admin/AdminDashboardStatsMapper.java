package com.fincoach.core.healthv2.mapper.admin;

import com.fincoach.core.healthv2.vo.admin.RecentAlertVO;
import com.fincoach.core.healthv2.vo.admin.RecentReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminDashboardStatsMapper {

    @Select("SELECT COUNT(*) FROM \"user\"")
    long countUsers();

    @Select("SELECT COUNT(*) FROM fc_health_report")
    long countReports();

    @Select("SELECT COUNT(*) FROM fc_alert_record WHERE created_at >= NOW() - make_interval(days => #{days})")
    long countAlertsInDays(@Param("days") int days);

    @Select("SELECT health_score FROM fc_health_report WHERE report_date >= NOW() - make_interval(days => #{days})")
    List<Integer> listHealthScoresInDays(@Param("days") int days);

    @Select("SELECT risk_score FROM fc_health_report WHERE report_date >= NOW() - make_interval(days => #{days})")
    List<Integer> listRiskScoresInDays(@Param("days") int days);

    @Select("SELECT id as reportId, user_id as userId, report_date as reportDate, " +
            "health_score as healthScore, risk_score as riskScore, behavior_score as behaviorScore " +
            "FROM fc_health_report ORDER BY report_date DESC LIMIT #{limit}")
    List<RecentReportVO> listRecentReports(@Param("limit") int limit);

    @Select("SELECT id, user_id as userId, rule_key as ruleKey, created_at as triggerAt, status, payload_json as payload " +
            "FROM fc_alert_record " +
            "WHERE created_at >= NOW() - make_interval(days => #{days}) " +
            "ORDER BY created_at DESC LIMIT #{limit}")
    List<RecentAlertVO> listRecentAlerts(@Param("days") int days, @Param("limit") int limit);
}
