package com.periut.retrotweaks.compat;

import com.periut.retrotweaks.RetroTweaks;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Makes a co-installed UniTweaks behave like RetroTweaks would.
 *
 * <p>When UniTweaks is present RetroTweaks disables the features it shares with it (that happens in
 * {@link com.periut.retrotweaks.config.ConfigManager}, driven by
 * {@link com.periut.retroapi.config.Opt#source()}). Two UniTweaks features were dropped
 * from RetroTweaks entirely rather than ported - the main menu panorama and the in-game version text -
 * so a fresh UniTweaks install would still switch them on and the pair would not look like one mod.
 * The armor-slot icons fix ({@code armorIconsFix}) gets the same treatment for a different reason:
 * it is on by default in UniTweaks but is not a look RetroTweaks wants out of the box, so a fresh
 * install starts with it off. In every case the row stays fully editable in UniTweaks' own screen -
 * this only picks the STARTING value.
 *
 * <p>Only a <i>fresh</i> UniTweaks config is touched. If its config already existed when the game
 * started, the user has settings there and RetroTweaks leaves them completely alone.
 */
public final class UniTweaksCompat {

	private UniTweaksCompat() {}

	/**
	 * Keys to force to {@code false} in a newly generated UniTweaks config. "enablePanorma" is
	 * UniTweaks' own spelling; "armorIconsFix" is the armor-slot icons fix (see the class doc).
	 */
	private static final String[] KEYS_TO_DISABLE = { "enablePanorma", "showVersionTextIngame", "armorIconsFix" };

	private static boolean configWasMissingAtStartup;
	private static boolean patchAttempted;

	public static void onInitialize() {
		if (!Mods.HAS_UNITWEAKS) return;
		configWasMissingAtStartup = findConfigFiles().isEmpty();
	}

	/**
	 * Applies the patch if UniTweaks has since written its config. Safe to call repeatedly; it does
	 * its work at most once. Called from the title screen, which is the first moment every mod's
	 * initialisation is guaranteed to have finished.
	 */
	public static void patchIfNeeded() {
		if (patchAttempted || !configWasMissingAtStartup || !Mods.HAS_UNITWEAKS) return;
		List<Path> files = findConfigFiles();
		if (files.isEmpty()) return; // not written yet; try again on the next title screen
		patchAttempted = true;
		for (Path path : files) patch(path);
	}

	private static List<Path> findConfigFiles() {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		if (!Files.isDirectory(configDir)) return List.of();
		// GCAPI has used more than one layout over its life (config/unitweaks.json,
		// config/unitweaks/*.json, *.yaml). Rather than guess, take every regular file whose path
		// mentions unitweaks and let the key search decide what is actually a config.
		try (Stream<Path> walk = Files.walk(configDir, 3)) {
			return walk.filter(Files::isRegularFile)
				.filter(p -> configDir.relativize(p).toString().toLowerCase().contains(Mods.UNITWEAKS))
				.toList();
		} catch (IOException e) {
			RetroTweaks.LOGGER.warn("Could not scan the config directory for UniTweaks", e);
			return List.of();
		}
	}

	private static void patch(Path path) {
		try {
			String original = Files.readString(path, StandardCharsets.UTF_8);
			String patched = original;
			for (String key : KEYS_TO_DISABLE) {
				// Format-agnostic on purpose: matches `"key": true`, `key: true` and `key = true`,
				// so it works whichever serialiser GCAPI happens to be using.
				Pattern pattern = Pattern.compile("(\"?" + Pattern.quote(key) + "\"?\\s*[:=]\\s*)true\\b");
				patched = pattern.matcher(patched).replaceAll("$1false");
			}
			if (patched.equals(original)) return;
			Files.writeString(path, patched, StandardCharsets.UTF_8);
			RetroTweaks.LOGGER.info("Turned the panorama, version text and armor icons off in the new UniTweaks config ({})", path.getFileName());
		} catch (IOException e) {
			RetroTweaks.LOGGER.warn("Could not adjust the UniTweaks config at {}", path, e);
		}
	}
}
