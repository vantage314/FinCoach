package com.fincoach.core.healthv2.service.admin;

public class CrawlerRunRequest {
    private final String scriptPath;

    public CrawlerRunRequest(String scriptPath) {
        this.scriptPath = scriptPath;
    }

    public String getScriptPath() {
        return scriptPath;
    }
}
