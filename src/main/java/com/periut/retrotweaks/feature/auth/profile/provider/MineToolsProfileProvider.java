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
 * A ProfileProvider implementation that uses the MineTools API.
 * It first calls /uuid/{username} to get basic profile info (UUID & name),
 * then calls /profile/{uuid} to fetch texture data (skin/cape URLs, signature, etc.).
 */
public class MineToolsProfileProvider implements ProfileProvider {

    // Endpoint to fetch basic profile data (UUID & name).
    private static final String UUID_ENDPOINT = "https://api.minetools.eu/uuid/";

    // Endpoint to fetch texture/signature data.
    private static final String PROFILE_ENDPOINT = "https://api.minetools.eu/profile/";

    @Override
    public Future<GameProfile> get(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // STEP 1: Fetch the user's basic profile from /uuid/{username}.
                String uuidUrl = UUID_ENDPOINT + username;
                JsonObject basicProfileJson = readJsonFromUrl(uuidUrl);

                // Validate the basic profile response.
                if (basicProfileJson == null || !basicProfileJson.has("status")) {
                    System.out.println("No valid response from " + uuidUrl);
                    return null;
                }
                String status = basicProfileJson.get("status").getAsString();
                if (!"OK".equalsIgnoreCase(status)) {
                    System.out.println("Status not OK for " + username + ": " + status);
                    return null;
                }

                // Extract the UUID and name.
                String uuid = basicProfileJson.get("id").getAsString();
                String name = basicProfileJson.get("name").getAsString();

                // STEP 2: Fetch detailed profile (texture data) from /profile/{uuid}.
                String textureUrl = PROFILE_ENDPOINT + uuid;
                JsonObject detailJson = readJsonFromUrl(textureUrl);
                if (detailJson == null || !detailJson.has("raw")) {
                    System.out.println("No valid response from " + textureUrl);
                    return new GameProfile(uuid, name, null, null, null, null);
                }

                /*
                 * The MineTools JSON structure for /profile/{uuid}:
                 * {
                 *   "decoded": {
                 *     "profileId": "853c80ef3c3749fdaa49938b674adae6",
                 *     "profileName": "jeb_",
                 *     "signatureRequired": true,
                 *     "textures": {
                 *       "CAPE": {"url": "..."},
                 *       "SKIN": {"url": "..."}
                 *     },
                 *     "timestamp": 1521401553373
                 *   },
                 *   "raw": {
                 *     "id": "...",
                 *     "name": "...",
                 *     "properties": [
                 *       {
                 *         "name": "textures",
                 *         "signature": "...",
                 *         "value": "..."
                 *       }
                 *     ],
                 *     "status": "OK"
                 *   }
                 * }
                 */

                // Extract textureValue and textureSignature from raw -> properties array.
                JsonObject raw = detailJson.getAsJsonObject("raw");
                if (!raw.has("properties")) {
                    // No texture properties
                    return new GameProfile(uuid, name, null, null, null, null);
                }

                JsonArray properties = raw.getAsJsonArray("properties");
                if (properties.size() == 0) {
                    // No properties found
                    return new GameProfile(uuid, name, null, null, null, null);
                }

                JsonObject textureProperty = properties.get(0).getAsJsonObject();
                String textureValue = textureProperty.get("value").getAsString();
                String textureSignature = textureProperty.get("signature").getAsString();

                // Extract the skin and cape URLs from decoded -> textures.
                JsonObject decoded = detailJson.getAsJsonObject("decoded");
                JsonObject textures = decoded.getAsJsonObject("textures");

                String skinUrl = null;
                if (textures.has("SKIN")) {
                    JsonObject skinObj = textures.getAsJsonObject("SKIN");
                    if (skinObj.has("url")) {
                        skinUrl = skinObj.get("url").getAsString();
                    }
                }

                String capeUrl = null;
                if (textures.has("CAPE")) {
                    JsonObject capeObj = textures.getAsJsonObject("CAPE");
                    if (capeObj.has("url")) {
                        capeUrl = capeObj.get("url").getAsString();
                    }
                }

                // Finally, construct and return the GameProfile.
                return new GameProfile(uuid, name, textureValue, textureSignature, capeUrl, skinUrl);

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
            conn.setRequestProperty("User-Agent", "MineToolsProfileProvider/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                // For example, 404 if user not found, 500, etc.
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
        return "MineToolsAPI";
    }
}
