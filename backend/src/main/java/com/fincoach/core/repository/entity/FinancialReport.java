package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@Data
@TableName("financial_report")
public class FinancialReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;
    private String reportName;
    private String revenue;
    private String revenueGrowth;
    private String netProfit;
    private String profitGrowth;
    private String eps;
    private String roe;
}
