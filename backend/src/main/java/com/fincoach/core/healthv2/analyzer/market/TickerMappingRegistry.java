package com.fincoach.core.healthv2.analyzer.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fincoach.core.ticker.entity.FcTickerMappingEntity;
import com.fincoach.core.ticker.service.TickerMappingDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class TickerMappingRegistry implements InitializingBean {

    public static final String TICKER_MAPPING_SOURCE_DB = "TICKER_MAPPING_SOURCE_DB";
    public static final String TICKER_MAPPING_SOURCE_FILE = "TICKER_MAPPING_SOURCE_FILE";
    public static final String TICKER_MAPPING_DB_UNAVAILABLE_FALLBACK_FILE = "TICKER_MAPPING_DB_UNAVAILABLE_FALLBACK_FILE";

    private final AtomicReference<MappingSnapshot> snapshotRef = new AtomicReference<>(new MappingSnapshot());

    @Autowired(required = false)
    private TickerMappingDbService tickerMappingDbService;

    @Value("${fincoach.ticker-mapping.db.enabled:true}")
    private boolean dbEnabled = true;

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    public void init() {
        reload();
    }

    public void reload() {
        MappingSnapshot snapshot = new MappingSnapshot();
        snapshot.config = loadYamlConfig();
        snapshot.dbBestByKeyword = new HashMap<>();
        snapshot.dbAvailable = false;

        if (dbEnabled) {
            if (tickerMappingDbService == null) {
                log.warn("Ticker mapping DB service not available, fallback to file mapping.");
            } else {
                try {
                    List<FcTickerMappingEntity> enabled = tickerMappingDbService.listEnabled();
                    snapshot.dbAvailable = true;
                    for (FcTickerMappingEntity e : enabled) {
                        if (e.getKeyword() == null) continue;
                        String key = normalizeInputKey(e.getKeyword());
                        FcTickerMappingEntity existing = snapshot.dbBestByKeyword.get(key);
                        if (existing == null) {
                            snapshot.dbBestByKeyword.put(key, e);
                        } else {
                            if (comparePriority(e, existing) > 0) {
                                snapshot.dbBestByKeyword.put(key, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Ticker mapping DB unavailable, fallback to file mapping: {}", e.getMessage());
                }
            }
        }
        snapshotRef.set(snapshot);
        log.info("Ticker mapping registry reloaded: dbAvailable={}, dbMappings={}, aliases={}",
                snapshot.dbAvailable,
                snapshot.dbBestByKeyword.size(),
                snapshot.config != null && snapshot.config.getAliases() != null ? snapshot.config.getAliases().size() : 0);
    }

    public ResolutionResult resolve(String input) {
        ResolutionResult result = new ResolutionResult();
        result.setOriginal(input);

        List<String> warnings = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            result.setResolvedTicker(null);
            result.setSource("NONE");
            result.setWarnings(warnings);
            return result;
        }

        String normalizedInput = normalizeInputKey(input);
        result.setNormalizedInput(normalizedInput);
        MappingSnapshot snapshot = snapshotRef.get();
        TickerMappingConfig config = snapshot != null && snapshot.config != null ? snapshot.config : new TickerMappingConfig();

        // 0. DB Mapping (Primary)
        if (dbEnabled) {
            if (snapshot == null || !snapshot.dbAvailable) {
                addWarning(warnings, TICKER_MAPPING_DB_UNAVAILABLE_FALLBACK_FILE);
            } else {
                FcTickerMappingEntity dbHit = snapshot.dbBestByKeyword.get(normalizedInput);
                if (dbHit != null) {
                    String resolved = normalizeResolvedTicker(dbHit.getTicker(), warnings, config);
                    result.setResolvedTicker(resolved);
                    result.setSource("DB");
                    addWarning(warnings, TICKER_MAPPING_SOURCE_DB);
                    result.setWarnings(warnings);
                    return result;
                }
            }
        }

        // 1. File Overrides
        if (config.getOverrides() != null && config.getOverrides().containsKey(normalizedInput)) {
            result.setResolvedTicker(config.getOverrides().get(normalizedInput));
            result.setSource("FILE_OVERRIDE");
            addWarning(warnings, TICKER_MAPPING_SOURCE_FILE);
            addWarning(warnings, "TICKER_MAPPING_HIT_OVERRIDE");
            result.setWarnings(warnings);
            return result;
        }

        // 2. File Aliases
        if (config.getAliases() != null && config.getAliases().containsKey(normalizedInput)) {
            result.setResolvedTicker(config.getAliases().get(normalizedInput));
            result.setSource("FILE_ALIAS");
            addWarning(warnings, TICKER_MAPPING_SOURCE_FILE);
            addWarning(warnings, "TICKER_MAPPING_HIT_ALIAS");
            result.setWarnings(warnings);
            return result;
        }

        // 3. Heuristics
        String heuristic = applyHeuristics(normalizedInput, warnings, config);
        if (heuristic != null) {
            result.setResolvedTicker(heuristic);
            result.setSource("HEURISTIC");
            // warnings added inside applyHeuristics
        } else {
            result.setResolvedTicker(null);
            result.setSource("NONE");
            addWarning(warnings, "POSITION_TICKER_UNRESOLVED");
        }
        
        result.setWarnings(warnings);
        return result;
    }

    public String normalizeInputKey(String input) {
        if (input == null) return "";
        // Trim, remove wide spaces, uppercase
        return input.trim().replace((char) 12288, ' ').toUpperCase();
    }

    private String normalizeResolvedTicker(String ticker, List<String> warnings, TickerMappingConfig config) {
        if (ticker == null) return null;
        String normalized = normalizeInputKey(ticker);
        String heuristic = applyHeuristics(normalized, warnings, config);
        return heuristic != null ? heuristic : normalized;
    }

    private String applyHeuristics(String input, List<String> warnings, TickerMappingConfig config) {
        // Existing suffix check
        if (input.endsWith(".US") || input.endsWith(".SS") || input.endsWith(".SZ") || input.endsWith(".HK")) {
            return input;
        }

        // Numeric CN
        if (config.getRules().isEnableNumericCnHeuristics() && input.matches("^\\d{6}$")) {
            if (input.startsWith("6")) {
                addWarning(warnings, "MARKET_DATA_SYMBOL_NORMALIZED");
                return input + ".SS";
            }
            if (input.startsWith("0") || input.startsWith("3")) {
                addWarning(warnings, "MARKET_DATA_SYMBOL_NORMALIZED");
                return input + ".SZ";
            }
        }

        // Alpha US
        if (config.getRules().isEnableAlphaUsHeuristics() && input.matches("^[A-Z]+$")) {
            addWarning(warnings, "MARKET_DATA_SYMBOL_NORMALIZED");
            addWarning(warnings, "TICKER_MAPPING_DEFAULT_MARKET_ASSUMED");
            return input + ".US";
        }

        return null;
    }

    public static class TickerMappingConfig {
        private int version = 1;
        private Defaults defaults = new Defaults();
        private Map<String, String> aliases = new HashMap<>();
        private Map<String, String> overrides = new HashMap<>();
        private Rules rules = new Rules();

        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
        public Defaults getDefaults() { return defaults; }
        public void setDefaults(Defaults defaults) { this.defaults = defaults; }
        public Map<String, String> getAliases() { return aliases; }
        public void setAliases(Map<String, String> aliases) { this.aliases = aliases; }
        public Map<String, String> getOverrides() { return overrides; }
        public void setOverrides(Map<String, String> overrides) { this.overrides = overrides; }
        public Rules getRules() { return rules; }
        public void setRules(Rules rules) { this.rules = rules; }

        public static class Defaults {
            private String market = "US";
            private String usSuffix = ".US";
            private CnSuffix cnSuffix = new CnSuffix();
            
            public String getMarket() { return market; }
            public void setMarket(String market) { this.market = market; }
            public String getUsSuffix() { return usSuffix; }
            public void setUsSuffix(String usSuffix) { this.usSuffix = usSuffix; }
            public CnSuffix getCnSuffix() { return cnSuffix; }
            public void setCnSuffix(CnSuffix cnSuffix) { this.cnSuffix = cnSuffix; }
        }

        public static class CnSuffix {
            private String sh = ".SS";
            private String sz = ".SZ";
            
            public String getSh() { return sh; }
            public void setSh(String sh) { this.sh = sh; }
            public String getSz() { return sz; }
            public void setSz(String sz) { this.sz = sz; }
        }
        
        public static class Rules {
            private boolean enableNumericCnHeuristics = true;
            private boolean enableAlphaUsHeuristics = true;
            
            public boolean isEnableNumericCnHeuristics() { return enableNumericCnHeuristics; }
            public void setEnableNumericCnHeuristics(boolean enableNumericCnHeuristics) { this.enableNumericCnHeuristics = enableNumericCnHeuristics; }
            public boolean isEnableAlphaUsHeuristics() { return enableAlphaUsHeuristics; }
            public void setEnableAlphaUsHeuristics(boolean enableAlphaUsHeuristics) { this.enableAlphaUsHeuristics = enableAlphaUsHeuristics; }
        }
    }

    public static class ResolutionResult {
        private String original;
        private String normalizedInput;
        private String resolvedTicker;
        private String source;
        private List<String> warnings = new ArrayList<>();

        public String getOriginal() { return original; }
        public void setOriginal(String original) { this.original = original; }
        public String getNormalizedInput() { return normalizedInput; }
        public void setNormalizedInput(String normalizedInput) { this.normalizedInput = normalizedInput; }
        public String getResolvedTicker() { return resolvedTicker; }
        public void setResolvedTicker(String resolvedTicker) { this.resolvedTicker = resolvedTicker; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    public void setTickerMappingDbService(TickerMappingDbService tickerMappingDbService) {
        this.tickerMappingDbService = tickerMappingDbService;
    }

    public void setDbEnabled(boolean dbEnabled) {
        this.dbEnabled = dbEnabled;
    }

    private void addWarning(List<String> warnings, String warning) {
        if (!warnings.contains(warning)) {
            warnings.add(warning);
        }
    }

    private int comparePriority(FcTickerMappingEntity a, FcTickerMappingEntity b) {
        int pa = a.getPriority() == null ? 0 : a.getPriority();
        int pb = b.getPriority() == null ? 0 : b.getPriority();
        if (pa != pb) {
            return Integer.compare(pa, pb);
        }
        if (a.getUpdatedAt() != null && b.getUpdatedAt() != null) {
            return a.getUpdatedAt().compareTo(b.getUpdatedAt());
        }
        return 0;
    }

    private TickerMappingConfig loadYamlConfig() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            ClassPathResource resource = new ClassPathResource("config/ticker-mapping.yml");
            if (resource.exists()) {
                TickerMappingConfig cfg = mapper.readValue(resource.getInputStream(), TickerMappingConfig.class);
                log.info("Loaded ticker-mapping.yml: {} aliases", cfg.getAliases().size());
                return cfg;
            }
            log.warn("ticker-mapping.yml not found, using defaults");
        } catch (IOException e) {
            log.error("Failed to load ticker-mapping.yml", e);
        }
        return new TickerMappingConfig();
    }

    private static class MappingSnapshot {
        private TickerMappingConfig config = new TickerMappingConfig();
        private Map<String, FcTickerMappingEntity> dbBestByKeyword = new HashMap<>();
        private boolean dbAvailable = false;
    }
}
