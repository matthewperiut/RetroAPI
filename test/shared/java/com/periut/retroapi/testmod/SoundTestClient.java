package com.periut.retroapi.testmod;

import com.periut.retroapi.sound.api.RetroSounds;
import net.fabricmc.loader.api.FabricLoader;
import net.ornithemc.osl.entrypoints.api.client.ClientModInitializer;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-side test harness for the RetroAPI sound autoloader.
 *
 * <p>Ships test OGG assets under both supported roots and, once the client is ready (by which point
 * {@code SoundManager.loadSounds} has run and the autoloader has scanned the classpath), logs the
 * event ids that the autoloader is expected to have registered so a human can trigger them in-game:</p>
 * <ul>
 *   <li>{@code assets/retroapi_test/sounds/sound/test/ping.ogg}            -> {@code retroapi_test:test.ping} (native root)</li>
 *   <li>{@code assets/retroapi_test/stationapi/sounds/sound/test/ping2.ogg} -> {@code retroapi_test:test.ping2} (StationAPI root)</li>
 *   <li>{@code .../stationapi/sounds/sound/test/random/call1.ogg + call2.ogg} -> collapsed to
 *       {@code retroapi_test:test.random.call} (the "sound" channel is random, so trailing digits are stripped)</li>
 * </ul>
 *
 * <p>Also exercises the public {@link RetroSounds#eventIdFor} helper. Runtime audibility (actually
 * hearing the sound via {@code world.playSound}) is a manual gate, out of scope for the build.</p>
 */
public class SoundTestClient implements ClientModInitializer {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI Test/Sound");

	@Override
	public void initClient() {
		// Derive the expected event ids via the public API (pure, no MC state required).
		String pingNative = RetroSounds.eventIdFor("retroapi_test", "test/ping.ogg");
		String pingStation = RetroSounds.eventIdFor("retroapi_test", "test/ping2.ogg");

		MinecraftClientEvents.READY.register(minecraft -> {
			boolean stationApi = FabricLoader.getInstance().isModLoaded("stationapi");
			LOGGER.info("Sound autoloader test (stationapi present = {}):", stationApi);
			LOGGER.info("  expected event id (native root)   : {}", pingNative);
			LOGGER.info("  expected event id (stationapi root): {}", pingStation);
			LOGGER.info("  expected random-collapsed event id : {}", "retroapi_test:test.random.call");
			LOGGER.info("  trigger any of the above with world.playSound(player, \"<id>\", 1f, 1f)");
		});
	}
}
