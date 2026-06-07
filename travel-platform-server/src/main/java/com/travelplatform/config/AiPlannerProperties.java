package com.travelplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.planner")
public class AiPlannerProperties {

    private boolean enabled = true;

    private String baseUrl = "https://api.openai.com/v1";

    private String apiKey;

    private String model = "gpt-4.1-mini";

    private int timeoutSeconds = 20;

    private int candidateLimit = 18;

    private int maxAttractionsPerDay = 3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getCandidateLimit() {
        return candidateLimit;
    }

    public void setCandidateLimit(int candidateLimit) {
        this.candidateLimit = candidateLimit;
    }

    public int getMaxAttractionsPerDay() {
        return maxAttractionsPerDay;
    }

    public void setMaxAttractionsPerDay(int maxAttractionsPerDay) {
        this.maxAttractionsPerDay = maxAttractionsPerDay;
    }
}
