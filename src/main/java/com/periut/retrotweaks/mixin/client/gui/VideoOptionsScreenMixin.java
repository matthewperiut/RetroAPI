package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.feature.options.ModOptions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.option.Option;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Replaces three of vanilla's own video-options entries with RetroTweaks' finer-grained versions.
 * From UniTweaks.
 *
 * <p>Rebuilt on every {@code init} rather than once in a static block, so switching an option on in
 * the RetroTweaks screen shows it here immediately - which is what "no restart required" means.
 *
 * <p>Render distance, FPS limit and GUI scale REPLACE a vanilla entry rather than being appended:
 * each is a finer-grained version of vanilla's own fixed-step control for the same thing, and having
 * both on screen would let the player set two values that disagree. Every other RetroTweaks video
 * setting b1.7.3 never had at all (fog density, clouds, cloud height, brightness, FOV) - previously
 * appended here too - now lives only on the Graphics tab ({@code ConfigScreen}'s "Graphics tab
 * extras" section) as a real slider/toggle, so the vanilla screen shows nothing vanilla itself never
 * offered.
 */
@Environment(EnvType.CLIENT)
@Mixin(VideoOptionsScreen.class)
public class VideoOptionsScreenMixin {

	@Shadow @Mutable
	private static Option[] VIDEO_OPTIONS;

	@Unique
	private static Option[] retrotweaks$vanillaOptions;

	@Inject(method = "init", at = @At("HEAD"))
	private void retrotweaks$addVideoOptions(CallbackInfo ci) {
		if (retrotweaks$vanillaOptions == null) retrotweaks$vanillaOptions = VIDEO_OPTIONS.clone();

		List<Option> options = new ArrayList<>(Arrays.asList(retrotweaks$vanillaOptions));
		retrotweaks$replace(options, Option.RENDER_DISTANCE, ModOptions.renderDistanceOption);
		retrotweaks$replace(options, Option.FRAMERATE_LIMIT, ModOptions.fpsLimitOption);
		retrotweaks$replace(options, Option.GUI_SCALE, ModOptions.guiScaleOption);

		VIDEO_OPTIONS = options.toArray(new Option[0]);
	}

	@Unique
	private static void retrotweaks$replace(List<Option> options, Option vanilla, Option replacement) {
		if (!ModOptions.enabled(replacement)) return;
		int index = options.indexOf(vanilla);
		if (index >= 0) options.set(index, replacement);
	}
}
