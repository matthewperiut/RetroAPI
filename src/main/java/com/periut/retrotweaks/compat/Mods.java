package com.periut.retrotweaks.compat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Mod ids RetroTweaks reacts to, and cached answers to "is it here?".
 *
 * <p>RetroTweaks depends on nothing but the loader. Every other mod named here is optional: when it is
 * present RetroTweaks either steps aside (so two mods never patch the same behaviour) or uses the
 * better facility it provides. The ids are compile-time constants so they can be used in
 * {@link com.periut.retroapi.config.Opt#source()} annotations.
 *
 * <p>The flags are resolved once at class-init. Mod lists cannot change while the game is running,
 * and these are read from mixins on hot paths.
 */
public final class Mods {

	private Mods() {}

	/** DanyGames2014's UniTweaks. Everything RetroTweaks inherited from it stands down when present. */
	public static final String UNITWEAKS = "unitweaks";

	/** RetroAuth. Supersedes the session, skin and cape features RetroTweaks inherited from MojangFix. */
	public static final String RETROAUTH = "retroauth";

	/** RetroDragon (LWJGL 3). Owns numeric frame pacing, so the FPS slider's caps defer to it - but
	 * not its VSync stop, which is a driver swap interval and applies under RetroDragon too. */
	public static final String RETRODRAGON = "retrodragon";

	/** RetroAPI - the other half of this same mod, so always present. Kept because {@link
	 *  com.periut.retroapi.config.Opt#requires()} names mods by id and an id that is always satisfied
	 *  is still the honest thing to write there. */
	public static final String RETROAPI = "retroapi";

	/** StationAPI. Not used, but it also rewrites recipes, so recipe work checks for it. */
	public static final String STATIONAPI = "stationapi";

	public static final boolean HAS_UNITWEAKS = isLoaded(UNITWEAKS);
	public static final boolean HAS_RETROAUTH = isLoaded(RETROAUTH);
	public static final boolean HAS_RETRODRAGON = isLoaded(RETRODRAGON);
	public static final boolean HAS_RETROAPI = isLoaded(RETROAPI);
	public static final boolean HAS_STATIONAPI = isLoaded(STATIONAPI);

	public static boolean isLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}
}
