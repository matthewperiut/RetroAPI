package com.periut.retroapi.sound.impl;

import com.periut.retroapi.sound.api.CustomSoundMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Client-side sound autoloader. Mirrors StationAPI's {@code station-audio-loader-v0} scanner but with
 * a clean classpath walk ({@link RetroResourceWalker}) in the {@code LangLoader} style.
 *
 * <p>Scans every loaded mod for files under
 * {@code assets/<modid>/{stationapi/sounds,sounds}/{sound,streaming,music}/...} and registers each one
 * as a named sound event via the {@link CustomSoundMap} duck cast on vanilla's per-channel
 * {@code Sounds} holders. The path->event id derivation lives in vanilla's own
 * {@code Sounds.load(String, File)} clone (see {@code SoundsMixin#retroapi$putSound}).</p>
 *
 * <p>Conventions (Wave-0 C3): scan BOTH the StationAPI layout {@code stationapi/sounds/} (so Aether's
 * assets need zero moves) AND the RetroAPI-native {@code sounds/} layout, preferring the StationAPI
 * layout. A sound appearing under both roots is registered once (de-duped by derived event id).</p>
 *
 * <p>This subsystem is purely client-side: no networking, no persistence, no id assignment, and it
 * never writes to the world folder.</p>
 */
public final class SoundAutoLoader {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/Sound");

	/**
	 * Asset roots, in precedence order. The StationAPI-compat layout is scanned first so Aether's
	 * existing {@code assets/aether/stationapi/sounds/...} assets work unchanged; the native
	 * {@code sounds/} layout lets future RetroAPI-only mods drop the {@code stationapi} segment.
	 */
	private static final String[] ROOTS = {"stationapi/sounds", "sounds"};

	private static final String[] CHANNELS = {"sound", "streaming", "music"};

	/** Keep .ogg/.wav only - vanilla's paulscode codecs are OGG + WAV (no mp3 codec). */
	private static final Predicate<String> AUDIO =
		n -> n.endsWith(".ogg") || n.endsWith(".wav");

	private SoundAutoLoader() {
	}

	/**
	 * Scan all mods and register every discovered sound into the three channel maps.
	 *
	 * @param soundChannel     the non-streaming "sound" channel (random-variant collapse enabled)
	 * @param streamingChannel the "streaming"/records channel (no collapse)
	 * @param musicChannel     the "music" channel
	 */
	public static void loadAll(CustomSoundMap soundChannel, CustomSoundMap streamingChannel,
	                           CustomSoundMap musicChannel) {
		// Belt-and-suspenders: when StationAPI is present, station-audio-loader-v0 owns this.
		// (The mixin that calls us is also disabled by RetroAPIMixinPlugin in that case.)
		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			return;
		}

		int total = 0;
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			String modId = mod.getMetadata().getId();
			total += scan(modId, "sound", soundChannel);
			total += scan(modId, "streaming", streamingChannel);
			total += scan(modId, "music", musicChannel);
		}
		LOGGER.info("Autoloaded {} sound(s) from {} mod(s)", total,
			FabricLoader.getInstance().getAllMods().size());
	}

	/** Sink for the StationAPI bridge: receives a derived event id and the source URL. */
	@FunctionalInterface
	public interface SoundSink {
		void put(String id, URL url);
	}

	/**
	 * Scan one mod's <em>native</em> {@code sounds/<channel>} root only and feed each discovered sound
	 * to {@code sink}. This is the StationAPI-present path: {@code station-audio-loader-v0} already scans
	 * the {@code stationapi/sounds/<channel>} root for every mod, so RetroAPI only needs to contribute
	 * the native {@code sounds/} layout. Any file that also exists under {@code stationapi/sounds/} is
	 * skipped here (StationAPI already registered it) to avoid a duplicate weighted entry.
	 *
	 * <p>Event ids are derived identically to {@link #scan} (and to StationAPI's loader): the channel
	 * folder is the split point, slashes become dots at insert time, lowercased. Called from
	 * {@code StationSoundBridgeMixin}; sink writes go through StationAPI's own {@code CustomSoundMap}.</p>
	 *
	 * @return the number of sounds handed to {@code sink}
	 */
	public static int scanNative(String modId, String channel, SoundSink sink) {
		String base = "/assets/" + modId + "/sounds/" + channel;
		if (RetroResourceWalker.class.getResource(base) == null) {
			return 0;
		}
		String saBase = "/assets/" + modId + "/stationapi/sounds/" + channel;
		boolean saRootExists = RetroResourceWalker.class.getResource(saBase) != null;
		int count = 0;
		for (URL url : RetroResourceWalker.walk(base, AUDIO)) {
			String rel = RetroResourceWalker.relativize(url, channel);
			if (rel == null || rel.isEmpty()) {
				continue;
			}
			// Skip files StationAPI's loader already registered under stationapi/sounds/<channel>.
			if (saRootExists && RetroResourceWalker.class.getResource(saBase + "/" + rel) != null) {
				continue;
			}
			String id = (modId + ":" + rel).toLowerCase();
			try {
				sink.put(id, url);
				count++;
				LOGGER.debug("Bridged sound {} -> {} (StationAPI)", id, url);
			} catch (RuntimeException e) {
				LOGGER.error("Failed to bridge sound {} ({})", id, url, e);
			}
		}
		return count;
	}

	private static int scan(String modId, String channel, CustomSoundMap target) {
		int count = 0;
		// De-dup by derived event id ("modid:relpath") so a sound shipped under both roots
		// registers only once. ROOTS order gives StationAPI layout precedence.
		Set<String> seen = new HashSet<>();
		for (String root : ROOTS) {
			String base = "/assets/" + modId + "/" + root + "/" + channel;
			if (RetroResourceWalker.class.getResource(base) == null) {
				continue;
			}
			for (URL url : RetroResourceWalker.walk(base, AUDIO)) {
				String rel = RetroResourceWalker.relativize(url, channel);
				if (rel == null || rel.isEmpty()) {
					continue;
				}
				String id = (modId + ":" + rel).toLowerCase();
				if (!seen.add(id)) {
					continue;
				}
				try {
					target.retroapi$putSound(id, url);
					count++;
					LOGGER.debug("Registered sound {} -> {}", id, url);
				} catch (RuntimeException e) {
					LOGGER.error("Failed to register sound {} ({})", id, url, e);
				}
			}
		}
		return count;
	}
}
