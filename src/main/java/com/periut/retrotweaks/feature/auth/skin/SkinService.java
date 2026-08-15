package com.periut.retrotweaks.feature.auth.skin;

import com.periut.retrotweaks.feature.auth.profile.GameProfile;
import com.periut.retrotweaks.feature.auth.profile.PlayerProfile;
import com.periut.retrotweaks.feature.auth.profile.provider.MineToolsProfileProvider;
import com.periut.retrotweaks.feature.auth.profile.provider.MojangProfileProvider;
import com.periut.retrotweaks.feature.auth.profile.provider.ProfileProvider;
import com.periut.retrotweaks.feature.auth.skin.data.PlayerEntitySkinData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class SkinService {
    public static final String STEVE_TEXTURE = "/assets/retrotweaks/mob/steve.png";
    public static final String ALEX_TEXTURE = "/assets/retrotweaks/mob/alex.png";

    private static final SkinService INSTANCE = new SkinService();
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Map<String, PlayerProfile> profiles = new HashMap<>();
    private final List<ProfileProvider> providers = List.of(new MineToolsProfileProvider(), new MojangProfileProvider());

    public static SkinService getInstance() {
        return INSTANCE;
    }

    private void updatePlayer(PlayerEntity player, PlayerProfile playerProfile) {
        PlayerEntitySkinData skinData = (PlayerEntitySkinData) player;

        if (playerProfile == null) {
            // Fallback to Steve skin and default model
            com.periut.retrotweaks.RetroTweaks.LOGGER.debug("Profile not found for player {}, using default skin.", player.name);
            skinData.setTextureModel("default");
        } else {
            // Apply fetched profile data
            skinData.setTextureModel(playerProfile.getTextureModel());
            player.skinUrl = playerProfile.getSkinUrl();
            // Capes are a separate option: some players want the modern skin but not the cape.
            if (com.periut.retrotweaks.config.Config.MULTIPLAYER.renderCapes) {
                player.capeUrl = player.playerCapeUrl = playerProfile.getCapeUrl();
            }
        }

        // Notify the world renderer to update the player entity
        ((Minecraft) FabricLoader.getInstance().getGameInstance()).worldRenderer.notifyEntityAdded(player);
    }

    private boolean updatePlayer(PlayerEntity player) {
        if (profiles.containsKey(player.name)) {
            PlayerProfile profile = profiles.get(player.name);
            updatePlayer(player, profile);
            return true;
        }

        return false;
    }

    private void initOffline(PlayerEntity player) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.name).getBytes(StandardCharsets.UTF_8));
        PlayerEntitySkinData skinData = (PlayerEntitySkinData) player;
        skinData.setTextureModel("default"); // Default texture model for offline players
    }

    /** Fetches and applies this player's skin and cape, unless the option is off. */
    public void init(PlayerEntity player) {
        if (!com.periut.retrotweaks.config.Config.MULTIPLAYER.fetchSkins) return;
        if (updatePlayer(player)) return;

        initOffline(player);

        (new Thread(() -> {
            try {
                init(player.name);
                updatePlayer(player);
            } catch (Exception e) {
                com.periut.retrotweaks.RetroTweaks.LOGGER.error("Error initializing profile for player {}", player.name, e);
                initOffline(player);
            }
        })).start();
    }

    public void init(String name) {
        if (!com.periut.retrotweaks.config.Config.MULTIPLAYER.fetchSkins) return;
        if (profiles.containsKey(name)) return;

        ReentrantLock lock = locks.computeIfAbsent(name, k -> new ReentrantLock());

        lock.lock();
        try {
            if (profiles.containsKey(name)) return;

            for (ProfileProvider provider : providers) {
                GameProfile gameProfile;

                try {
                    gameProfile = provider.get(name).get();
                } catch (Exception e) {
                    com.periut.retrotweaks.RetroTweaks.LOGGER.warn("Profile lookup failed using {} for player {}: {}", provider.getClass().getSimpleName(), name, e.getMessage());
                    continue;
                }

                try {
                    PlayerProfile playerProfile = new PlayerProfile(
                            UUID.fromString(gameProfile.getId()),
                            gameProfile.getSkinUrl() != null ? gameProfile.getSkinUrl() : STEVE_TEXTURE,
                            gameProfile.getCapeUrl(),
                            gameProfile.getModel() != null ? gameProfile.getModel() : "default"
                    );

                    com.periut.retrotweaks.RetroTweaks.LOGGER.debug("[{}] Downloaded profile: {} ({})", provider.getProviderName(), gameProfile.getName(), gameProfile.getId());
                    profiles.put(name, playerProfile);
                } catch (Exception e) {
                    com.periut.retrotweaks.RetroTweaks.LOGGER.error("[{}] Failed to map GameProfile to PlayerProfile for player {}: {}", provider.getProviderName(), name, e.getMessage());
                    continue;
                }

                return;
            }

            // No profile found; fallback to offline profile
            com.periut.retrotweaks.RetroTweaks.LOGGER.debug("No profile found for player {}, using offline fallback.", name);
            profiles.put(name, null);
        } finally {
            lock.unlock();
        }
    }
}
