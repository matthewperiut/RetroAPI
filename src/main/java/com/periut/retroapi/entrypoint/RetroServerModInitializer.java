package com.periut.retroapi.entrypoint;

/**
 * The {@code retroapi-server} entrypoint: the dedicated-server half of {@link RetroModInitializer}, run by
 * RetroAPI's own server initializer once the server-side platform (teleporters, GUI opener, id-sync and
 * entity-spawn channels) is in place.
 *
 * <p>Beta 1.7.3 has no integrated server, so this fires <em>only</em> in a headless server process, never
 * in singleplayer. Server-only types ({@code ServerPlayerEntity}, {@code ServerPlayNetworking}, …) belong
 * here and only here.
 *
 * <pre>
 * "entrypoints": { "retroapi-server": [ "com.example.example_mod.ExampleModServer" ] }
 * </pre>
 */
@FunctionalInterface
public interface RetroServerModInitializer {

	/** Registers this mod's dedicated-server content. Never runs in singleplayer. */
	void initRetroServer();
}
