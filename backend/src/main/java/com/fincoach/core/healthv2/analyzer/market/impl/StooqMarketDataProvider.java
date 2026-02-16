package com.fincoach.core.healthv2.analyzer.market.impl;

import com.fincoach.core.healthv2.analyzer.market.MarketDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MVP implementation using Stooq (CSV download).
 * No API key required.
 * URL format: https://stooq.com/q/d/l/?s={symbol}&d1={start}&d2={end}&i=d
 */
@Slf4j
@Component
public class StooqMarketDataProvider implements MarketDataProvider {

    private final RestTemplate restTemplate;
    private static final String BASE_URL = "https://stooq.com/q/d/l/?s=%s&d1=%s&d2=%s&i=d";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter CSV_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StooqMarketDataProvider() {
        this.restTemplate = new RestTemplate();
    }
    
    // For testing
    public StooqMarketDataProvider(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Simple in-memory cache: Symbol -> Date -> Price
    // For MVP, just cache everything to avoid repeated calls in same session? 
    // Actually HealthReport is usually one-off. A static cache might be better but let's keep it instance for now or simple local map.
    // Or maybe just fetch on demand. Stooq might block if too frequent.
    // Let's implement a simple optimize: getDailySeries fetches range.

    @Override
    public BigDecimal getDailyClose(String symbol, LocalDate date) {
        // Inefficient to fetch single day via CSV usually, but Stooq supports it.
        // Better to use range.
        Map<LocalDate, BigDecimal> series = getDailySeries(symbol, date, date);
        return series.get(date);
    }

    @Override
    public Map<LocalDate, BigDecimal> getDailySeries(String symbol, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, BigDecimal> result = new TreeMap<>();
        String normSymbol = normalizeSymbol(symbol);
        String startStr = startDate.format(DATE_FMT);
        String endStr = endDate.format(DATE_FMT);
        String url = String.format(BASE_URL, normSymbol, startStr, endStr);

        try {
            log.info("[Stooq] Fetching {} from {} to {}", normSymbol, startStr, endStr);
            String csv = restTemplate.getForObject(url, String.class);
            if (csv == null || csv.isEmpty()) return result;

            try (BufferedReader br = new BufferedReader(new StringReader(csv))) {
                String line;
                // Header: Date,Open,High,Low,Close,Volume
                // Skip header
                br.readLine(); 
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length < 5) continue;
                    
                    // Date
                    String dateStr = parts[0];
                    // Close is usually index 4
                    String closeStr = parts[4];
                    
                    try {
                        LocalDate d = LocalDate.parse(dateStr, CSV_DATE_FMT);
                        BigDecimal close = new BigDecimal(closeStr);
                        result.put(d, close);
                    } catch (Exception e) {
                        // ignore bad lines
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Stooq] Failed to fetch data for {}: {}", normSymbol, e.getMessage());
            // Return empty, trigger fallback upstream
        }
        return result;
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null) return "Unknown";
        // Simple normalization
        if (symbol.endsWith(".US") || symbol.endsWith(".JP") || symbol.endsWith(".UK")) {
            return symbol;
        }
        // If no suffix, assume US for MVP? Or maybe Stooq needs specific codes.
        // Let's assume input symbol is already Stooq-friendly or add .US if 
        // it looks like a ticker (all alpha).
        if (symbol.matches("^[A-Z]+$")) {
            return symbol + ".US";
        }
        return symbol;
    }
}
