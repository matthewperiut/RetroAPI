package com.periut.retroapi.config;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.network.RetroAPINetworking;

import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The client half of config sync: what the server tells this client, and the one thing an operator
 * may tell it back.
 *
 * <p>Client-only, referenced from {@code RetroAPIClient} and the config screen, so a dedicated server
 * never loads it.
 */
public final class ConfigNetworking {

	private ConfigNetworking() {
	}

	/** True once a payload has arrived this session, so the join timeout knows to stop waiting. */
	private static boolean answered;
	private static long joinedAt;
	private static boolean waiting;

	/** How long to wait for a server's answer before deciding it has no configs to send. */
	private static final long HANDSHAKE_TIMEOUT_MILLIS = 3000L;

	public static void registerClient() {
		ClientPlayNetworking.registerListener(RetroAPINetworking.CONFIG_CHANNEL, (context, buffer) -> {
			final boolean editable = buffer.readBoolean();
			final int count = buffer.readVarInt();
			final Map<String, String> encoded = new LinkedHashMap<>();
			for (int i = 0; i < count; i++) {
				encoded.put(buffer.readString(), buffer.readString());
			}

			context.ensureOnMainThread();
			answered = true;
			waiting = false;
			apply(encoded, editable);
		});
	}

	/**
	 * Turns the encoded pairs into values and hands them to {@link ConfigSync}. Decoding here rather
	 * than in the listener because an option that no longer exists, or a value this build cannot read,
	 * must be skipped rather than taken as "the server did not mention it".
	 */
	private static void apply(final Map<String, String> encoded, final boolean editable) {
		final Map<String, ConfigTree.Option> options = ConfigSync.worldOptions();
		final Map<String, Object> values = new LinkedHashMap<>();
		encoded.forEach((key, text) -> {
			final ConfigTree.Option option = options.get(key);
			if (option == null) {
				return;   // a mod the server has and this client does not
			}
			try {
				values.put(key, ConfigTree.decodeString(option, text));
			} catch (final RuntimeException e) {
				RetroAPI.LOGGER.warn("Ignoring the server's value for '{}': {}", key, e.getMessage());
			}
		});
		ConfigSync.applyServerConfig(values, editable);
	}

	/**
	 * Called when the player joins a remote world. World options stand down immediately and only come
	 * back if the server says what it is running, so there is never a window in which the client acts
	 * on rules the server has not confirmed.
	 */
	public static void onJoinServer() {
		ConfigSync.applyVanillaServer();
		answered = false;
		waiting = true;
		joinedAt = System.currentTimeMillis();
	}

	/** Called every client tick while connected, to give up on a server that will not answer. */
	public static void tick() {
		if (!waiting || answered) {
			return;
		}
		if (System.currentTimeMillis() - joinedAt < HANDSHAKE_TIMEOUT_MILLIS) {
			return;
		}
		waiting = false;
		RetroAPI.LOGGER.info("Server sent no config; world options stay at their defaults for this session.");
	}

	/** Called when the player leaves a world. */
	public static void onLeaveServer() {
		waiting = false;
		answered = false;
		ConfigSync.restoreLocal();
	}

	/**
	 * Sends an operator's edit of one server option.
	 *
	 * <p>The value is NOT applied locally first. A world option under server control reads the server's
	 * value by definition, so the honest sequence is: ask, the server checks the asker is an operator,
	 * and the answer comes back on the same channel as a broadcast to everyone - which is what moves
	 * the row. A client that is not entitled simply sees nothing change.
	 */
	public static void pushServerEdit(final RetroConfig config, final ConfigTree.Option option, final Object value) {
		if (!ClientPlayNetworking.isPlayReady(RetroAPINetworking.CONFIG_CHANNEL)) {
			return;
		}
		final String key = ConfigSync.keyOf(config, option);
		final String encoded = ConfigTree.encodeString(option, value);
		ClientPlayNetworking.send(RetroAPINetworking.CONFIG_CHANNEL, buffer -> {
			buffer.writeString(key);
			buffer.writeString(encoded);
		});
	}
}
