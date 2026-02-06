package com.fincoach.core.repository.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
@TableName("company_profile")
public class CompanyProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "stock_code")
    private String stockCode;

    private String companyName;
    private LocalDate establishmentDate;
    private LocalDate listingDate;
    private String chairman;
    private String website;
    private String businessScope;
    private String mainCompetitors;
    private Integer employees;
}
