package com.periut.retroapi.entrypoint;

import com.periut.retroapi.RetroAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Invokes the {@code retroapi} / {@code retroapi-client} / {@code retroapi-server} entrypoints.
 *
 * <p>Each stage runs exactly once (a second call is a no-op, so a mod that also lists RetroAPI's
 * initializer classes cannot double-register), in mod-load order, and a failure names the mod that
 * caused it instead of surfacing as an anonymous stack trace inside RetroAPI.
 */
public final class RetroEntrypoints {

	/** The {@code fabric.mod.json} entrypoint keys, mirroring OSL's {@code init}/{@code client-init}/{@code server-init}. */
	public static final String COMMON = "retroapi";
	public static final String CLIENT = "retroapi-client";
	public static final String SERVER = "retroapi-server";

	private static boolean commonDone;
	private static boolean clientDone;
	private static boolean serverDone;

	private RetroEntrypoints() {}

	/** Runs every mod's {@code retroapi} entrypoint. Called from {@link RetroAPI#init()}. */
	public static void invokeCommon() {
		if (commonDone) return;
		commonDone = true;
		invoke(COMMON, RetroModInitializer.class, RetroModInitializer::initRetro);
	}

	/** Runs every mod's {@code retroapi-client} entrypoint. Called from RetroAPI's client initializer. */
	public static void invokeClient() {
		if (clientDone) return;
		clientDone = true;
		invoke(CLIENT, RetroClientModInitializer.class, RetroClientModInitializer::initRetroClient);
	}

	/** Runs every mod's {@code retroapi-server} entrypoint. Called from RetroAPI's server initializer. */
	public static void invokeServer() {
		if (serverDone) return;
		serverDone = true;
		invoke(SERVER, RetroServerModInitializer.class, RetroServerModInitializer::initRetroServer);
	}

	private static <T> void invoke(String key, Class<T> type, Consumer<T> call) {
		List<EntrypointContainer<T>> containers =
			FabricLoader.getInstance().getEntrypointContainers(key, type);
		if (containers.isEmpty()) {
			return;
		}
		RetroAPI.LOGGER.debug("Running {} '{}' entrypoint(s)", containers.size(), key);
		for (EntrypointContainer<T> container : containers) {
			String modId = container.getProvider().getMetadata().getId();
			try {
				call.accept(container.getEntrypoint());
			} catch (Throwable t) {
				// Naming the mod here is the whole point: a registration failure deep inside RetroAPI is
				// otherwise indistinguishable from a RetroAPI bug.
				throw new RuntimeException("Mod '" + modId + "' failed in its '" + key + "' entrypoint", t);
			}
		}
	}
}
