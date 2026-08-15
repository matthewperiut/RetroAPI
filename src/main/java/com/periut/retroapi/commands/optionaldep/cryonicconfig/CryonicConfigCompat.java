package com.periut.retroapi.commands.optionaldep.cryonicconfig;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Cryonic Config accessed purely by reflection, so retrocommands carries no compile-time or
 * published dependency on it - nothing that depends on retrocommands inherits it.
 *
 * <p>Everything resolves once in the static initialiser. If Cryonic Config is absent, or is a
 * version whose API no longer matches, {@link #isAvailable()} reports false and every accessor
 * degrades to its default instead of throwing.
 */
public final class CryonicConfigCompat {
	private static final String CONFIG = "com.periut.cryonicconfig.CryonicConfig";
	private static final String UTILITY = "com.periut.cryonicconfig.UtilityCryonicConfig";
	private static final String STORAGE = "com.periut.cryonicconfig.ConfigStorage";

	/** {@code ConfigStorage CryonicConfig.getConfig(String modId)} */
	private static final MethodHandle GET_CONFIG;
	/** {@code String ConfigStorage.getString(String key, String fallback)} */
	private static final MethodHandle GET_STRING;
	/** {@code void UtilityCryonicConfig.init(String dir)} */
	private static final MethodHandle INIT;

	static {
		MethodHandle getConfig = null;
		MethodHandle getString = null;
		MethodHandle init = null;

		try {
			ClassLoader loader = CryonicConfigCompat.class.getClassLoader();
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();

			Class<?> config = Class.forName(CONFIG, false, loader);
			Class<?> utility = Class.forName(UTILITY, false, loader);
			Class<?> storage = Class.forName(STORAGE, false, loader);

			getConfig = lookup.findStatic(config, "getConfig",
				MethodType.methodType(storage, String.class));
			getString = lookup.findVirtual(storage, "getString",
				MethodType.methodType(String.class, String.class, String.class));
			init = lookup.findStatic(utility, "init",
				MethodType.methodType(void.class, String.class));
		} catch (Throwable ignored) {
			// Not installed, or an incompatible version - isAvailable() stays false.
			getConfig = null;
			getString = null;
			init = null;
		}

		GET_CONFIG = getConfig;
		GET_STRING = getString;
		INIT = init;
	}

	private CryonicConfigCompat() {
	}

	/** True only if Cryonic Config is present <em>and</em> every method we use resolved. */
	public static boolean isAvailable() {
		return GET_CONFIG != null && GET_STRING != null && INIT != null;
	}

	/** Reads a string from {@code modId}'s config, or returns {@code fallback}. */
	public static String getString(String modId, String key, String fallback) {
		if (!isAvailable()) {
			return fallback;
		}

		try {
			Object storage = GET_CONFIG.invoke(modId);
			if (storage == null) {
				return fallback;
			}

			String value = (String) GET_STRING.invoke(storage, key, fallback);
			return value == null ? fallback : value;
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	/** Re-reads the config files under {@code dir}. Returns false if the reload did not happen. */
	public static boolean reload(String dir) {
		if (!isAvailable()) {
			return false;
		}

		try {
			INIT.invoke(dir);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}
}
