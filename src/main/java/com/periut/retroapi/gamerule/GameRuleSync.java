package com.periut.retroapi.gamerule;

import com.periut.retroapi.network.RetroAPINetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import java.util.Map;

/**
 * Server-only half of game-rule sync: kept in its own class so a client environment never loads
 * {@code ServerPlayerEntity} / {@code ServerPlayNetworking} (mirrors {@code BlockDataSyncServer}).
 * {@link RetroGameRules} only calls in here on a dedicated server.
 *
 * <p>The wire format is the same for a full set and a single change - a count, then that many
 * key/value pairs - so the client needs one listener and applies whichever it gets.
 */
public final class GameRuleSync {
    private GameRuleSync() {
    }

    /** Everything, for a player who just joined. */
    public static void sendAll(final ServerPlayerEntity player) {
        final Map<String, String> values = RetroGameRules.snapshot();
        ServerPlayNetworking.send(player, RetroAPINetworking.GAMERULE_CHANNEL, buffer -> {
            buffer.writeVarInt(values.size());
            for (final Map.Entry<String, String> entry : values.entrySet()) {
                buffer.writeString(entry.getKey());
                buffer.writeString(entry.getValue());
            }
        });
    }

    /** One rule that just changed, to everyone who is listening. */
    static void broadcast(final String key, final String value) {
        final MinecraftServer server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
        if (server == null || server.playerManager == null) {
            return;
        }

        for (final Object candidate : server.playerManager.players) {
            if (candidate instanceof ServerPlayerEntity player
                && ServerPlayNetworking.isPlayReady(player, RetroAPINetworking.GAMERULE_CHANNEL)) {
                ServerPlayNetworking.send(player, RetroAPINetworking.GAMERULE_CHANNEL, buffer -> {
                    buffer.writeVarInt(1);
                    buffer.writeString(key);
                    buffer.writeString(value);
                });
            }
        }
    }
}
