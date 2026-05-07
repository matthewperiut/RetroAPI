package com.periut.retroapi.lang;

import com.periut.retroapi.mixin.client.LanguageAccessor;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.locale.Language;
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

	public static void loadTranslations() {
		Language language = Language.getInstance();
		Properties translations = ((LanguageAccessor) language).retroapi$getTranslations();

		if (!FabricLoader.getInstance().isModLoaded("stationapi")) {
			for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
				String modId = mod.getMetadata().getId();
				String langPath = "/assets/" + modId + "/lang/en_US.lang";

				try (InputStream is = LangLoader.class.getResourceAsStream(langPath)) {
					if (is != null) {
						loadLangFile(is, translations, modId);
					}
				} catch (IOException e) {
					LOGGER.error("Failed to load lang file for mod {}", modId, e);
				}
			}
		}

		injectDefaults(translations);
	}

	/**
	 * Inject default translations for RetroAPI blocks/items that don't have one.
	 * Uses the identifier path formatted as a title (e.g. "test_block" -> "Test Block").
	 */
	public static void injectDefaults(Properties translations) {
		int count = 0;
		for (BlockRegistration reg : RetroRegistry.getBlocks()) {
			String key = reg.getBlock().getTranslationKey() + ".name";
			if (!translations.containsKey(key)) {
				translations.setProperty(key, formatName(reg.getId().identifier()));
				count++;
			}
		}
		for (ItemRegistration reg : RetroRegistry.getItems()) {
			String key = reg.getItem().getTranslationKey() + ".name";
			if (!translations.containsKey(key)) {
				translations.setProperty(key, formatName(reg.getId().identifier()));
				count++;
			}
		}
		if (count > 0) {
			LOGGER.info("Injected {} default translations for unnamed blocks/items", count);
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
				String value = line.substring(eq + 1);
				translations.setProperty(key, value);
				count++;
			}
		}
		LOGGER.info("Loaded {} translations from mod {}", count, modId);
	}
}
