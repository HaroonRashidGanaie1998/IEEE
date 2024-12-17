package com.example.Haroon.model;

import java.time.ZonedDateTime;
import java.util.List;

public class PackageUsers {

    private String id;
    private String apikey;
    private String secret;
    private int rateLimitCeiling;
    private boolean rateLimitExempt;
    private int qpsLimitCeiling;
    private boolean qpsLimitExempt;
    private String status;
    private ZonedDateTime created;
    private ZonedDateTime updated;
    private String expires;
    private List<Limit> limits;
    private boolean oauthRateLimitExempt;
    private int oauthRateLimitCeiling;
    private boolean oauthQpsLimitExempt;
    private int oauthQpsLimitCeiling;
    private int defaultOauthRateLimitCeiling;
    private int defaultOauthQpsLimitCeiling;
    private boolean subscription;
    private boolean isProductionSubscription;
    private String period;
    private String source;
    private int ceiling;

    // Getters and Setters
    private List<ApplicationUsers> applications; // This will hold the Application Users for the package

    // Getter and Setter for applications
    public List<ApplicationUsers> getApplications() {
        return applications;
    }

    public void setApplications(List<ApplicationUsers> applications) {
        this.applications = applications;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getRateLimitCeiling() {
        return rateLimitCeiling;
    }

    public void setRateLimitCeiling(int rateLimitCeiling) {
        this.rateLimitCeiling = rateLimitCeiling;
    }

    public boolean isRateLimitExempt() {
        return rateLimitExempt;
    }

    public void setRateLimitExempt(boolean rateLimitExempt) {
        this.rateLimitExempt = rateLimitExempt;
    }

    public int getQpsLimitCeiling() {
        return qpsLimitCeiling;
    }

    public void setQpsLimitCeiling(int qpsLimitCeiling) {
        this.qpsLimitCeiling = qpsLimitCeiling;
    }

    public boolean isQpsLimitExempt() {
        return qpsLimitExempt;
    }

    public void setQpsLimitExempt(boolean qpsLimitExempt) {
        this.qpsLimitExempt = qpsLimitExempt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public ZonedDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(ZonedDateTime updated) {
        this.updated = updated;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public List<Limit> getLimits() {
        return limits;
    }

    public void setLimits(List<Limit> limits) {
        this.limits = limits;
    }

    public boolean isOauthRateLimitExempt() {
        return oauthRateLimitExempt;
    }

    public void setOauthRateLimitExempt(boolean oauthRateLimitExempt) {
        this.oauthRateLimitExempt = oauthRateLimitExempt;
    }

    public int getOauthRateLimitCeiling() {
        return oauthRateLimitCeiling;
    }

    public void setOauthRateLimitCeiling(int oauthRateLimitCeiling) {
        this.oauthRateLimitCeiling = oauthRateLimitCeiling;
    }

    public boolean isOauthQpsLimitExempt() {
        return oauthQpsLimitExempt;
    }

    public void setOauthQpsLimitExempt(boolean oauthQpsLimitExempt) {
        this.oauthQpsLimitExempt = oauthQpsLimitExempt;
    }

    public int getOauthQpsLimitCeiling() {
        return oauthQpsLimitCeiling;
    }

    public void setOauthQpsLimitCeiling(int oauthQpsLimitCeiling) {
        this.oauthQpsLimitCeiling = oauthQpsLimitCeiling;
    }

    public int getDefaultOauthRateLimitCeiling() {
        return defaultOauthRateLimitCeiling;
    }

    public void setDefaultOauthRateLimitCeiling(int defaultOauthRateLimitCeiling) {
        this.defaultOauthRateLimitCeiling = defaultOauthRateLimitCeiling;
    }

    public int getDefaultOauthQpsLimitCeiling() {
        return defaultOauthQpsLimitCeiling;
    }

    public void setDefaultOauthQpsLimitCeiling(int defaultOauthQpsLimitCeiling) {
        this.defaultOauthQpsLimitCeiling = defaultOauthQpsLimitCeiling;
    }

    public boolean isSubscription() {
        return subscription;
    }

    public void setSubscription(boolean subscription) {
        this.subscription = subscription;
    }

    public boolean isProductionSubscription() {
        return isProductionSubscription;
    }

    public void setProductionSubscription(boolean productionSubscription) {
        isProductionSubscription = productionSubscription;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getCeiling() {
        return ceiling;
    }

    public void setCeiling(int ceiling) {
        this.ceiling = ceiling;
    }

    // Limit class definition
    public static class Limit {
        private int value;
        private String type;

        // Getters and Setters for Limit
        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
