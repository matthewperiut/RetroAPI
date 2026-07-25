package com.periut.retroapi.entrypoint;

/**
 * The {@code retroapi-client} entrypoint: the client half of {@link RetroModInitializer}, run by
 * RetroAPI's own client initializer once the client-side platform (render types, model renderer, GUI
 * openers, networking listeners) is in place.
 *
 * <p>This is where renderers, screens, particle factories, block colors and client packet listeners
 * belong - everything that names a {@code net.minecraft.client} type. It never runs on a dedicated
 * server, so classes referenced from here are never loaded there.
 *
 * <pre>
 * "entrypoints": { "retroapi-client": [ "com.example.example_mod.ExampleModClient" ] }
 * </pre>
 */
@FunctionalInterface
public interface RetroClientModInitializer {

	/** Registers this mod's client-side content. Runs on the client only, after RetroAPI's client setup. */
	void initRetroClient();
}
