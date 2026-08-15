package com.periut.retroapi.gamemode;

import com.periut.retroapi.network.RetroAPINetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server-only half of game-mode sync, in its own class so a client environment never loads
 * {@code ServerPlayerEntity} (mirrors {@code GameRuleSync}).
 *
 * <p>Every client is told about every player, not just itself: rendering a spectator as invisible is
 * a decision each client makes about somebody else.
 */
public final class GameModeSync {
    private GameModeSync() {
    }

    /** Everything, for a player who just joined - and that player's arrival, for everyone else. */
    public static void sendAll(final ServerPlayerEntity player) {
        final MinecraftServer server = server();
        if (server == null) {
            return;
        }

        final List<String> names = new ArrayList<>();
        for (final Object candidate : server.playerManager.players) {
            if (candidate instanceof ServerPlayerEntity online) {
                names.add(online.name);
            }
        }

        final Map<String, RetroGameMode> modes = RetroGameModes.snapshot(names);
        ServerPlayNetworking.send(player, RetroAPINetworking.GAMEMODE_CHANNEL, buffer -> {
            buffer.writeVarInt(modes.size());
            for (final Map.Entry<String, RetroGameMode> entry : modes.entrySet()) {
                buffer.writeString(entry.getKey());
                buffer.writeVarInt(entry.getValue().getId());
            }
        });

        // And tell everyone else what mode the newcomer is in, so they render correctly from the start.
        broadcast(player.name, RetroGameModes.get(player.name));

        // Flight is one-life data the server has been holding since this player last left, and their
        // own client is what keeps them in the air, so it has to be told - after the modes, because
        // the client will not accept flight for a player it still believes is in survival.
        sendFlight(player);
    }

    /**
     * Tells one player whether they may fly and whether they are flying. Nobody else needs either: they
     * drive that player's own physics rather than anything anyone renders.
     */
    public static void sendFlight(final ServerPlayerEntity player) {
        if (player == null || !ServerPlayNetworking.isPlayReady(player, RetroAPINetworking.FLIGHT_CHANNEL)) {
            return;
        }
        final boolean mayFly = RetroGameModes.mayFly(player.name);
        final boolean flying = RetroGameModes.isFlying(player.name);
        ServerPlayNetworking.send(player, RetroAPINetworking.FLIGHT_CHANNEL, buffer -> {
            buffer.writeBoolean(mayFly);
            buffer.writeBoolean(flying);
        });
    }

    /** By name, for callers in common code that only have one - see RetroGameModes.notifyFlight. */
    public static void sendFlight(final String playerName) {
        final MinecraftServer server = server();
        if (server == null || playerName == null) {
            return;
        }
        for (final Object candidate : server.playerManager.players) {
            if (candidate instanceof ServerPlayerEntity online && playerName.equals(online.name)) {
                sendFlight(online);
                return;
            }
        }
    }

    static void broadcast(final String playerName, final RetroGameMode mode) {
        final MinecraftServer server = server();
        if (server == null) {
            return;
        }

        for (final Object candidate : server.playerManager.players) {
            if (candidate instanceof ServerPlayerEntity online
                && ServerPlayNetworking.isPlayReady(online, RetroAPINetworking.GAMEMODE_CHANNEL)) {
                ServerPlayNetworking.send(online, RetroAPINetworking.GAMEMODE_CHANNEL, buffer -> {
                    buffer.writeVarInt(1);
                    buffer.writeString(playerName);
                    buffer.writeVarInt(mode.getId());
                });
            }
        }
    }

    private static MinecraftServer server() {
        final Object game = FabricLoader.getInstance().getGameInstance();
        return game instanceof MinecraftServer server && server.playerManager != null ? server : null;
    }
}
