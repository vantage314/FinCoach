package com.fincoach.core.healthv2.controller.admin;

import com.fincoach.core.common.Result;
import com.fincoach.core.healthv2.debug.PortfolioDebugContextHolder;
import com.fincoach.core.healthv2.debug.PortfolioMarketDebugSnapshot;
import com.fincoach.core.security.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/admin/portfolio/market-debug")
@Tag(name = "Admin-Portfolio-Debug", description = "Portfolio Debug (Admin)")
@AdminOnly
public class AdminMarketDebugController {

    @GetMapping("/latest")
    @Operation(summary = "获取最新 Portfolio Market Debug 快照")
    public ResponseEntity<Result<PortfolioMarketDebugSnapshot>> latest() {
        PortfolioMarketDebugSnapshot snapshot = PortfolioDebugContextHolder.get();
        if (snapshot == null) {
            snapshot = new PortfolioMarketDebugSnapshot();
            snapshot.setGeneratedAt(Instant.now());
            snapshot.setWarnings(new ArrayList<>());
            snapshot.getWarnings().add("DEBUG_NO_CONTEXT");
        }
        return ResponseEntity.ok(Result.success(snapshot));
    }
}
