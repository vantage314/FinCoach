package com.fincoach.core.healthv2.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/app/import/templates")
public class ImportTemplateController {

    @GetMapping("/assets.csv")
    public ResponseEntity<byte[]> assetsTemplate() {
        String csv = "type,name,amount,currency,riskLevel,asOfDate\n"
                + "CASH,SalaryCard,50000,CNY,LOW,2026-02-15\n";
        return csvResponse("assets.csv", csv);
    }

    @GetMapping("/liabilities.csv")
    public ResponseEntity<byte[]> liabilitiesTemplate() {
        String csv = "type,principal,interestRate,remainingMonths,monthlyPayment,prepayPenaltyJson\n"
                + "MORTGAGE,800000,0.042,300,4200,{\"fee\":0}\n";
        return csvResponse("liabilities.csv", csv);
    }

    @GetMapping("/cashflows.csv")
    public ResponseEntity<byte[]> cashflowsTemplate() {
        String csv = "month,income,fixedExpense,variableExpense,monthlyDebtPayment,notes\n"
                + "2026-02,20000,6000,2500,3000,regular\n";
        return csvResponse("cashflows.csv", csv);
    }

    private ResponseEntity<byte[]> csvResponse(String filename, String csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
