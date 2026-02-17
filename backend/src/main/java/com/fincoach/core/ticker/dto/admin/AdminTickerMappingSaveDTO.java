package com.fincoach.core.ticker.dto.admin;

public class AdminTickerMappingSaveDTO {
    private Long id;
    private String keyword;
    private String ticker;
    private String market;
    private Integer priority;
    private Integer enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
