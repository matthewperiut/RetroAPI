package com.periut.retroapi.config;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.network.RetroAPINetworking;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import java.util.Map;

/**
 * Server-only half of config sync: kept in its own class so a client environment never loads
 * {@code ServerPlayerEntity} / {@code ServerPlayNetworking} (mirrors {@code GameRuleSync}).
 *
 * <p>Sends every {@link Scope#WORLD} option to a joining player, accepts an operator's edit back, and
 * broadcasts what it accepted so every connected client - including the one that asked - ends up
 * reading the same value the server does.
 */
public final class ConfigSyncServer {

	private ConfigSyncServer() {
	}

	/** Registers the listener that accepts operator edits. Called once, from the server entry point. */
	public static void registerServer() {
		ServerPlayNetworking.registerListener(RetroAPINetworking.CONFIG_CHANNEL, (context, buffer) -> {
			final String key = buffer.readString();
			final String encoded = buffer.readString();
			context.ensureOnMainThread();
			applyEdit(context.player(), key, encoded);
		});
	}

	/** Everything, for a player who just joined. */
	public static void sendAll(final ServerPlayerEntity player) {
		final boolean editable = isOperator(player);
		final Map<String, ConfigTree.Option> options = ConfigSync.worldOptions();
		ServerPlayNetworking.send(player, RetroAPINetworking.CONFIG_CHANNEL, buffer -> {
			buffer.writeBoolean(editable);
			buffer.writeVarInt(countSyncable(options));
			options.forEach((key, option) -> {
				if (!ConfigTree.isSyncable(option)) {
					return;
				}
				buffer.writeString(key);
				buffer.writeString(ConfigTree.encodeString(option, option.get()));
			});
		});
	}

	/**
	 * Applies one edit, if the sender is entitled to make it.
	 *
	 * <p>The op check is done HERE and not on the client's say-so: the "may edit" flag in the join
	 * payload exists so the screen can draw a red asterisk, and a client is free to lie about it. A
	 * client that is not an operator is ignored silently - it was never shown an editable row, so
	 * anything arriving from one is either a stale screen or someone trying it on.
	 */
	private static void applyEdit(final ServerPlayerEntity player, final String key, final String encoded) {
		if (!isOperator(player)) {
			RetroAPI.LOGGER.warn("Ignoring a config edit of '{}' from {}, who is not an operator", key, player.name);
			return;
		}
		final RetroConfig config = ConfigSync.configOf(key);
		final ConfigTree.Option option = ConfigSync.writeWorldValue(key, encoded, true);
		if (config == null || option == null) {
			return;
		}
		config.save();
		RetroAPI.LOGGER.info("{} set '{}' to {}", player.name, key, encoded);
		broadcast(key, ConfigTree.encodeString(option, option.get()));
	}

	/** One option that just changed, to everyone who is listening. */
	private static void broadcast(final String key, final String encoded) {
		final MinecraftServer server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
		if (server == null || server.playerManager == null) {
			return;
		}
		for (final Object candidate : server.playerManager.players) {
			if (candidate instanceof ServerPlayerEntity player
				&& ServerPlayNetworking.isPlayReady(player, RetroAPINetworking.CONFIG_CHANNEL)) {
				ServerPlayNetworking.send(player, RetroAPINetworking.CONFIG_CHANNEL, buffer -> {
					buffer.writeBoolean(isOperator(player));
					buffer.writeVarInt(1);
					buffer.writeString(key);
					buffer.writeString(encoded);
				});
			}
		}
	}

	private static int countSyncable(final Map<String, ConfigTree.Option> options) {
		int count = 0;
		for (final ConfigTree.Option option : options.values()) {
			if (ConfigTree.isSyncable(option)) {
				count++;
			}
		}
		return count;
	}

	private static boolean isOperator(final ServerPlayerEntity player) {
		final MinecraftServer server = (MinecraftServer) FabricLoader.getInstance().getGameInstance();
		return server != null && server.playerManager != null && server.playerManager.isOperator(player.name);
	}
}
