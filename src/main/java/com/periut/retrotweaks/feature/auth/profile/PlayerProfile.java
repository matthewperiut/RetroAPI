package com.periut.retrotweaks.feature.auth.profile;

import java.time.Instant;
import java.util.UUID;

public class PlayerProfile {
    private final UUID uuid;
    private final String skinUrl;
    private final String capeUrl;
    private final String textureModel; // Changed to a String for flexibility
    private Instant lastFetched = Instant.now();

    public PlayerProfile(UUID uuid, String skinUrl, String capeUrl, String textureModel) {
        this.uuid = uuid;
        this.skinUrl = skinUrl;
        this.capeUrl = capeUrl;
        this.textureModel = textureModel;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getSkinUrl() {
        return skinUrl;
    }

    public String getCapeUrl() {
        return capeUrl;
    }

    public String getTextureModel() {
        return textureModel;
    }

    public Instant getLastFetched() {
        return lastFetched;
    }

    public void setLastFetched(Instant lastFetched) {
        this.lastFetched = lastFetched;
    }
}
