package com.periut.retroapi.config;

import com.periut.retroapi.RetroAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every config any mod has registered, and the way to register one.
 *
 * <p>This is the whole entry point. Declare the options as annotated fields on an object (see
 * {@link Opt}, {@link Cat} and {@link RetroConfig}'s own doc for the shape), hand it over once at
 * init, and read the fields wherever the values are wanted:
 *
 * <pre>{@code
 * public static final MyConfig CONFIG = new MyConfig();
 *
 * @Override
 * public void initRetro() {
 *     RetroConfigs.register("mymod", "My Mod", CONFIG);
 * }
 * }</pre>
 *
 * <p>What that one call buys, all of it shared with RetroAPI's own settings rather than
 * reimplemented per mod:
 * <ul>
 *   <li>A page under <b>Options &gt; Configs... &gt; Mods</b>, with the right widget per field type,
 *       descriptions, search-free nesting, Defaults and per-page reset.</li>
 *   <li>{@code config/<id>.json}, written back after every change and re-written on load so an old
 *       file gains new options instead of being replaced.</li>
 *   <li>Server agreement for anything marked {@link Scope#WORLD}: forced to defaults on a server that
 *       does not run it, replaced by the server's own values on one that does, and editable in place
 *       by an operator - see {@link ConfigSync}.</li>
 *   <li>Standing down cleanly when another mod owns the same feature ({@link Opt#source()}), or when
 *       an optional API the option needs is missing ({@link Opt#requires()}).</li>
 * </ul>
 *
 * <p>Register from {@code initRetro()} (RetroAPI's own entrypoint) or any time before the first
 * screen is opened. Registering the same id twice returns the first registration rather than
 * replacing it, so a mod that initialises twice cannot end up with two trees over one file.
 */
public final class RetroConfigs {

	private RetroConfigs() {
	}

	private static final Map<String, RetroConfig> CONFIGS = new LinkedHashMap<>();

	/**
	 * Registers {@code root}'s annotated fields as the config for {@code modId}, loading
	 * {@code config/<modId>.json} into them immediately.
	 *
	 * @param modId       the owning mod's id, which is also the file name
	 * @param displayName what the screen calls it - the mod's own name, in the mod's own casing
	 * @param root        the object whose {@link Opt}/{@link Cat} fields are the options
	 */
	public static synchronized RetroConfig register(final String modId, final String displayName, final Object root) {
		final RetroConfig existing = CONFIGS.get(modId);
		if (existing != null) {
			RetroAPI.LOGGER.warn("Config '{}' is already registered; keeping the first registration", modId);
			return existing;
		}
		final RetroConfig config = new RetroConfig(modId, displayName, root);
		CONFIGS.put(modId, config);
		RetroAPI.LOGGER.debug("Registered config '{}' ({})", modId, displayName);
		return config;
	}

	/** Every registered config, in registration order. */
	public static synchronized List<RetroConfig> all() {
		return Collections.unmodifiableList(new ArrayList<>(CONFIGS.values()));
	}

	/** One config by mod id, or null. */
	public static synchronized RetroConfig get(final String modId) {
		return CONFIGS.get(modId);
	}

	/** Saves every registered config. Called when the config screen closes. */
	public static synchronized void saveAll() {
		for (final RetroConfig config : CONFIGS.values()) {
			config.save();
		}
	}
}
