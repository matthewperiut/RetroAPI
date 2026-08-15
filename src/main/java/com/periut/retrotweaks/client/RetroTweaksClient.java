package com.periut.retrotweaks.client;

import com.periut.retrotweaks.feature.options.ModOptions;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side entry point. Almost everything is driven from the mixin that owns it, reading
 * {@link com.periut.retrotweaks.config.Config} directly; what is left here are the two
 * things that have to exist before anything else runs - the channel a server announces itself on, and
 * the added video settings.
 */
@Environment(EnvType.CLIENT)
public class RetroTweaksClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		// Before the first frame, not on the player's first visit to the options screen - see
		// ModOptions.ensureRegistered for what used to be silently inert until then.
		ModOptions.ensureRegistered();
	}
}
