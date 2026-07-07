package com.periut.retroapi.lang;

import com.periut.retroapi.mixin.client.LanguageAccessor;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.resource.language.TranslationStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class LangLoader {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/LangLoader");

	private static boolean modLangLoaded = false;

	/**
	 * Load every mod's lang files into TranslationStorage. Idempotent. Called from {@code RetroAPI.init}
	 * (common, both sides) BEFORE the registration callbacks fire, and lazily from
	 * {@link com.periut.retroapi.achievement.RetroAchievements#register} - vanilla {@code Achievement}
	 * translates eagerly in its constructor, so the lang must already be present when mods
	 * construct their achievements. The lazy trigger matters because Fabric invokes {@code init}
	 * entrypoints in mod LOAD order (alphabetical), not dependency order, so a dependent mod
	 * (e.g. "aether" &lt; "retroapi") can register achievements before RetroAPI's own init runs.
	 * No-op under StationAPI (it loads lang itself).
	 */
	public static void loadModLangFiles() {
		if (modLangLoaded) return;
		modLangLoaded = true;
		if (FabricLoader.getInstance().isModLoaded("stationapi")) return;

		TranslationStorage language = TranslationStorage.getInstance();
		Properties translations = ((LanguageAccessor) language).retroapi$getTranslations();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			String modId = mod.getMetadata().getId();
			// Support both the StationAPI-compatible path (default) and the RetroAPI-native path,
			// so achievement/lang assets laid out for StationAPI migrate with zero file moves.
			loadLangPath(translations, modId, "/assets/" + modId + "/stationapi/lang/en_US.lang");
			loadLangPath(translations, modId, "/assets/" + modId + "/lang/en_US.lang");
		}
	}

	/**
	 * Client-init entry: inject default display names for registered content without a translation.
	 * The lang FILES themselves are loaded much earlier by {@link #loadModLangFiles} (see its doc);
	 * this part stays here because it needs the registries populated.
	 */
	public static void loadTranslations() {
		TranslationStorage language = TranslationStorage.getInstance();
		Properties translations = ((LanguageAccessor) language).retroapi$getTranslations();
		injectDefaults(translations);
	}

	private static void loadLangPath(Properties translations, String modId, String langPath) {
		try (InputStream is = LangLoader.class.getResourceAsStream(langPath)) {
			if (is != null) {
				loadLangFile(is, translations, modId);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load lang file {} for mod {}", langPath, modId, e);
		}
	}

	/**
	 * Inject default translations for RetroAPI blocks/items that don't have one.
	 * Uses the identifier path formatted as a title (e.g. "test_block" -> "Test Block").
	 */
	public static void injectDefaults(Properties translations) {
		java.util.List<String> autoNamed = new java.util.ArrayList<>();
		for (BlockRegistration reg : RetroRegistry.getBlocks()) {
			String key = reg.getBlock().getTranslationKey() + ".name";
			if (!translations.containsKey(key)) {
				String name = formatName(reg.getId().identifier());
				translations.setProperty(key, name);
				autoNamed.add(reg.getId() + " -> \"" + name + "\"");
			}
		}
		for (ItemRegistration reg : RetroRegistry.getItems()) {
			String key = reg.getItem().getTranslationKey() + ".name";
			if (!translations.containsKey(key)) {
				String name = formatName(reg.getId().identifier());
				translations.setProperty(key, name);
				autoNamed.add(reg.getId() + " -> \"" + name + "\"");
			}
		}
		if (!autoNamed.isEmpty()) {
			// Name them, not just count them: a reminder of exactly which content is riding an
			// AUTO-GENERATED display name (no lang entry). Add a lang line to silence any of these.
			int shown = Math.min(autoNamed.size(), 20);
			LOGGER.info("Using AUTO-GENERATED names for {} unnamed block(s)/item(s) (add a lang entry to override): {}{}",
				autoNamed.size(), String.join(", ", autoNamed.subList(0, shown)),
				autoNamed.size() > shown ? ", ... (+" + (autoNamed.size() - shown) + " more)" : "");
		}
	}

	/**
	 * Format an identifier path as a human-readable name.
	 * "test_block" -> "Test Block", "crate" -> "Crate"
	 */
	private static String formatName(String path) {
		StringBuilder sb = new StringBuilder();
		for (String word : path.split("_")) {
			if (!word.isEmpty()) {
				if (sb.length() > 0) sb.append(' ');
				sb.append(Character.toUpperCase(word.charAt(0)));
				sb.append(word.substring(1));
			}
		}
		return sb.toString();
	}

	private static void loadLangFile(InputStream is, Properties translations, String modId) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		String line;
		int count = 0;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			int eq = line.indexOf('=');
			if (eq > 0) {
				String key = line.substring(0, eq).replace("@", modId);
				String value = unescape(line.substring(eq + 1));
				translations.setProperty(key, value);
				count++;
			}
		}
		LOGGER.info("Loaded {} translations from mod {}", count, modId);
	}

	/**
	 * Decode .properties-style escapes in a lang value. StationAPI-era lang files write color codes
	 * as the literal six characters {@code \}{@code u00A7a}; {@code Properties.load} would decode
	 * them to the section-sign color code, but this manual parser must do it itself - otherwise
	 * "Book of Lore" shows the raw backslash sequence instead of rendering green.
	 */
	private static String unescape(String s) {
		if (s.indexOf('\\') < 0) return s;
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\' && i + 1 < s.length()) {
				char n = s.charAt(++i);
				switch (n) {
					case 'u':
						if (i + 4 < s.length()) {
							try {
								sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
								i += 4;
								continue;
							} catch (NumberFormatException ignored) {
								// fall through to literal
							}
						}
						sb.append('\\').append('u');
						continue;
					case 'n':
						sb.append('\n');
						continue;
					case 't':
						sb.append('\t');
						continue;
					case '\\':
						sb.append('\\');
						continue;
					default:
						sb.append('\\').append(n);
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
