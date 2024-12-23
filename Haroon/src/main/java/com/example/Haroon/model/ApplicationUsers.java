package com.example.Haroon.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;

public class ApplicationUsers {

    private String id;
    private ZonedDateTime created;
    private ZonedDateTime updated;
    private String username;
    private String name;
    private String description;
    private String type;
    private boolean commercial;
    private boolean ads;
    private String adsSystem;
    private String usageModel;
    private String tags;
    private String notes;
    private String howDidYouHear;
    private String preferredProtocol;
    private String preferredOutput;
    private String externalId;
    private String uri;
    private String status;
    private boolean isPackaged;
    private String oauthRedirectUri;
    private boolean subscription;
    private boolean isProductionSubscription;
    private String company;

    @JsonProperty("Organization_type")
    private String organization_type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isCommercial() {
        return commercial;
    }

    public void setCommercial(boolean commercial) {
        this.commercial = commercial;
    }

    public boolean isAds() {
        return ads;
    }

    public void setAds(boolean ads) {
        this.ads = ads;
    }

    public String getAdsSystem() {
        return adsSystem;
    }

    public void setAdsSystem(String adsSystem) {
        this.adsSystem = adsSystem;
    }

    public String getUsageModel() {
        return usageModel;
    }

    public void setUsageModel(String usageModel) {
        this.usageModel = usageModel;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getHowDidYouHear() {
        return howDidYouHear;
    }

    public void setHowDidYouHear(String howDidYouHear) {
        this.howDidYouHear = howDidYouHear;
    }

    public String getPreferredProtocol() {
        return preferredProtocol;
    }

    public void setPreferredProtocol(String preferredProtocol) {
        this.preferredProtocol = preferredProtocol;
    }

    public String getPreferredOutput() {
        return preferredOutput;
    }

    public void setPreferredOutput(String preferredOutput) {
        this.preferredOutput = preferredOutput;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPackaged() {
        return isPackaged;
    }

    public void setPackaged(boolean packaged) {
        isPackaged = packaged;
    }

    public String getOauthRedirectUri() {
        return oauthRedirectUri;
    }

    public void setOauthRedirectUri(String oauthRedirectUri) {
        this.oauthRedirectUri = oauthRedirectUri;
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

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getOrganization_type() {
        return organization_type;
    }

    public void setOrganization_type(String organization_type) {
        this.organization_type = organization_type;
    }
}
