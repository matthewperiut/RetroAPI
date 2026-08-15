package com.periut.retrotweaks.feature.auth.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Base64;
import java.util.Objects;

public class GameProfile {
    private final String id;
    private final String name;
    private final String textureValue;
    private final String textureSignature;
    private final String capeUrl;    // NEW FIELD
    private final String skinUrl;    // NEW FIELD

    public GameProfile(String id, String name, String textureValue, String textureSignature,
                       String capeUrl, String skinUrl) {
        this.id = id;
        this.name = name;
        this.textureValue = textureValue;
        this.textureSignature = textureSignature;
        this.capeUrl = capeUrl;
        this.skinUrl = skinUrl;
    }

    private static String addDashesToUUID(String uuid32) {
        return uuid32.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                "$1-$2-$3-$4-$5"
        );
    }

    public String getId() {
        return addDashesToUUID(id);
    }

    public String getName() {
        return name;
    }

    public String getTextureValue() {
        return textureValue;
    }

    public String getTextureSignature() {
        return textureSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameProfile that = (GameProfile) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(textureValue, that.textureValue) &&
                Objects.equals(textureSignature, that.textureSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, textureValue, textureSignature);
    }

    @Override
    public String toString() {
        return "GameProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", textureValue='" + textureValue + '\'' +
                ", textureSignature='" + textureSignature + '\'' +
                '}';
    }

    /**
     * Parses the texture data to extract the skin model.
     */
    public String getModel() {
        JsonObject textures = parseTextureData();
        if (textures != null && textures.has("SKIN")) {
            JsonObject skin = textures.getAsJsonObject("SKIN");
            if (skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")) {
                return skin.getAsJsonObject("metadata").get("model").getAsString();
            }
        }
        return "default"; // Default model if none specified
    }

    /**
     * Extracts the skin URL from the texture data.
     */
    public String getSkinUrl() {
        if (skinUrl != null)
            return skinUrl;

        JsonObject textures = parseTextureData();
        if (textures != null && textures.has("SKIN")) {
            return textures.getAsJsonObject("SKIN").get("url").getAsString();
        }
        return null; // No skin URL available
    }

    /**
     * Extracts the cape URL from the texture data.
     */
    public String getCapeUrl() {
        if (capeUrl != null)
            return capeUrl;

        JsonObject textures = parseTextureData();
        if (textures != null && textures.has("CAPE")) {
            return textures.getAsJsonObject("CAPE").get("url").getAsString();
        }
        return null; // No cape URL available
    }

    /**
     * Helper method to parse the Base64-encoded texture value.
     */
    private JsonObject parseTextureData() {
        try {
            String decodedData = new String(Base64.getDecoder().decode(textureValue));
            JsonObject json = JsonParser.parseString(decodedData).getAsJsonObject();
            return json.getAsJsonObject("textures");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
