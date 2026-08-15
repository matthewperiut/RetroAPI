package com.periut.retrotweaks.feature.auth.profile.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.periut.retrotweaks.feature.auth.profile.GameProfile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Example implementation of a MojangProfileProvider.
 * Fetches profile data from Mojang's public API.
 */
public class MojangProfileProvider implements ProfileProvider {

    @Override
    public Future<GameProfile> get(String username) {
        // Use supplyAsync to run this operation in a background thread.
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1) First fetch the user’s basic profile (UUID + name).
                String profileUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
                JsonObject basicProfile = readJsonFromUrl(profileUrl);

                // If the basic profile is null, Mojang did not find a user by that name.
                if (basicProfile == null || !basicProfile.has("id")) {
                    return null;  // or throw an exception if you prefer
                }

                String uuid = basicProfile.get("id").getAsString();
                String name = basicProfile.get("name").getAsString();

                // 2) Now fetch the detailed profile, which contains textures/signatures.
                // Use unsigned=false to get the signature as well.
                String sessionUrl = "https://sessionserver.mojang.com/session/minecraft/profile/"
                        + uuid + "?unsigned=false";
                JsonObject detailedProfile = readJsonFromUrl(sessionUrl);
                if (detailedProfile == null) {
                    return null;
                }

                // 3) Parse the "properties" array to get the texture info.
                JsonArray properties = detailedProfile.getAsJsonArray("properties");
                if (properties.size() == 0) {
                    // No properties found
                    return new GameProfile(uuid, name, null, null, null, null);
                }

                JsonObject textureProperty = properties.get(0).getAsJsonObject();
                String textureValue = textureProperty.get("value").getAsString();
                String textureSignature = textureProperty.get("signature").getAsString();

                // 4) Construct and return a GameProfile.
                return new GameProfile(uuid, name, textureValue, textureSignature, null, null);

            } catch (Exception e) {
                // In case of an error, complete with an exception.
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Helper method to fetch JSON from a given URL using HttpURLConnection.
     */
    private JsonObject readJsonFromUrl(String urlString) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "MojangFixStation/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                // For example, 204 (No Content) if user not found, 404, etc.
                return null;
            }

            // Read JSON response
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }

            // Parse the JSON string into a JsonObject
            return JsonParser.parseString(responseBuilder.toString()).getAsJsonObject();

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            // Clean up
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public String getProviderName() {
        return "MojangAPI";
    }
}
