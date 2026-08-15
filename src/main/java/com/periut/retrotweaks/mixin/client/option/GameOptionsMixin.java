package com.periut.retrotweaks.mixin.client.option;

import com.periut.retrotweaks.feature.options.ModOptions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Option;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Makes the added video settings behave like vanilla ones: readable, settable, labelled and saved.
 * From UniTweaks.
 *
 * <p>They are stored in a file of their own rather than in options.txt. Vanilla's loader throws on
 * a line it does not recognise and silently drops the rest of the file, so writing our keys there
 * would risk a player's whole options file the moment RetroTweaks was removed.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameOptions.class)
public class GameOptionsMixin {

	@Shadow protected Minecraft minecraft;

	@Unique
	private static final String RETROTWEAKS_OPTIONS_FILE = "retrotweaks-video.txt";

	@Inject(method = "setFloat", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$setFloat(Option option, float value, CallbackInfo ci) {
		if (!ModOptions.isModOption(option)) return;
		ci.cancel();
		if (!ModOptions.enabled(option)) return;

		if (option == ModOptions.fogDensityOption) ModOptions.fogDensity = value;
		else if (option == ModOptions.cloudHeightOption) ModOptions.cloudHeight = value;
		else if (option == ModOptions.fpsLimitOption) {
			ModOptions.fpsLimit = value;
			// Snap to the stop the label has been showing all along, same reasoning and same rounding
			// as the GUI scale slider below: released mid-drag, the handle should land on the value it
			// actually selected rather than sit between two stops.
			if (!Mouse.isButtonDown(0)) {
				ModOptions.fpsLimit = ModOptions.snapToFpsLimitStep(value);
			}
		}
		else if (option == ModOptions.renderDistanceOption) ModOptions.renderDistance = value;
		else if (option == ModOptions.brightnessOption) ModOptions.brightness = value;
		else if (option == ModOptions.fovOption) ModOptions.fov = value;
		else if (option == ModOptions.guiScaleOption) {
			ModOptions.guiScale = value;
			// Applied on release, not while dragging: re-laying out the screen every frame would
			// move the slider out from under the cursor.
			if (!Mouse.isButtonDown(0)) {
				// Snap to the step the label has been showing all along. The handle slides smoothly
				// under the cursor while dragging, then lands on the value it actually selected
				// instead of sitting between two steps. refreshScreen rebuilds the widgets, and a
				// rebuilt SliderWidget reads its position back from getFloat, so it moves with it.
				ModOptions.guiScale = ModOptions.snapToGuiScaleStep(value);
				ModOptions.realGuiScale = ModOptions.guiScale;
				ModOptions.refreshScreen(this.minecraft);
			}
		}
		retrotweaks$save();
	}

	@Inject(method = "setInt", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$setInt(Option option, int value, CallbackInfo ci) {
		if (!ModOptions.isModOption(option)) return;
		ci.cancel();
		if (option == ModOptions.cloudsOption && ModOptions.enabled(option)) {
			ModOptions.clouds = !ModOptions.clouds;
			retrotweaks$save();
		}
	}

	@Inject(method = "getFloat", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$getFloat(Option option, CallbackInfoReturnable<Float> cir) {
		if (!ModOptions.isModOption(option)) return;
		if (option == ModOptions.fogDensityOption) cir.setReturnValue(ModOptions.fogDensity);
		else if (option == ModOptions.cloudHeightOption) cir.setReturnValue(ModOptions.cloudHeight);
		else if (option == ModOptions.fpsLimitOption) cir.setReturnValue(ModOptions.fpsLimit);
		else if (option == ModOptions.renderDistanceOption) cir.setReturnValue(ModOptions.renderDistance);
		else if (option == ModOptions.brightnessOption) cir.setReturnValue(ModOptions.brightness);
		else if (option == ModOptions.guiScaleOption) cir.setReturnValue(ModOptions.guiScale);
		else if (option == ModOptions.fovOption) cir.setReturnValue(ModOptions.fov);
	}

	@Inject(method = "getBoolean", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$getBoolean(Option option, CallbackInfoReturnable<Integer> cir) {
		if (option == ModOptions.cloudsOption) cir.setReturnValue(ModOptions.clouds ? 1 : 0);
	}

	@Inject(method = "getString", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$getString(Option option, CallbackInfoReturnable<String> cir) {
		if (!ModOptions.isModOption(option)) return;
		String label = retrotweaks$label(option);
		if (option == ModOptions.cloudsOption) {
			cir.setReturnValue(label + (ModOptions.clouds ? "ON" : "OFF"));
		} else if (option == ModOptions.fpsLimitOption) {
			String value = switch (ModOptions.frameLimitMode()) {
				case VSYNC -> "VSync";
				case UNLIMITED -> "Unlimited";
				case CAPPED -> ModOptions.frameLimit() + " fps";
			};
			cir.setReturnValue(label + value);
		} else if (option == ModOptions.renderDistanceOption) {
			cir.setReturnValue(label + ModOptions.renderDistanceChunks() + " chunks");
		} else if (option == ModOptions.cloudHeightOption) {
			cir.setReturnValue(label + (int) ModOptions.cloudHeightBlocks());
		} else if (option == ModOptions.fogDensityOption) {
			cir.setReturnValue(label + Math.round(ModOptions.fogMultiplier() * 100.0F) + "%");
		} else if (option == ModOptions.brightnessOption) {
			cir.setReturnValue(label + (ModOptions.brightness == 0.0F ? "OFF" : Math.round(ModOptions.brightness * 100.0F) + "%"));
		} else if (option == ModOptions.guiScaleOption) {
			int steps = ModOptions.guiScaleDisplaySteps();
			cir.setReturnValue(label + (steps == 0 ? "Auto" : String.valueOf(steps)));
		} else if (option == ModOptions.fovOption) {
			float value = ModOptions.fov;
			String display = value == 0.0F ? "Normal" : value == 1.0F ? "Max" : String.valueOf(ModOptions.fovDegrees());
			cir.setReturnValue(label + display);
		}
	}

	@Unique
	private String retrotweaks$label(Option option) {
		return ModOptions.optionLabel(option);
	}

	@Inject(method = "load", at = @At("TAIL"))
	private void retrotweaks$load(CallbackInfo ci) {
		File file = retrotweaks$file();
		if (file == null || !file.exists()) return;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(":", 2);
				if (parts.length != 2) continue;
				switch (parts[0]) {
					case "fogDensity" -> ModOptions.fogDensity = Float.parseFloat(parts[1]);
					case "cloudHeight" -> ModOptions.cloudHeight = Float.parseFloat(parts[1]);
					case "fpsLimit" -> ModOptions.fpsLimit = Float.parseFloat(parts[1]);
					case "renderDistance" -> ModOptions.renderDistance = Float.parseFloat(parts[1]);
					case "brightness" -> ModOptions.brightness = Float.parseFloat(parts[1]);
					case "guiScale" -> ModOptions.guiScale = ModOptions.realGuiScale = Float.parseFloat(parts[1]);
					case "fov" -> ModOptions.fov = Float.parseFloat(parts[1]);
					case "clouds" -> ModOptions.clouds = Boolean.parseBoolean(parts[1]);
					default -> { /* an option from a newer version; ignore it rather than fail */ }
				}
			}
		} catch (Exception e) {
			com.periut.retrotweaks.RetroTweaks.LOGGER.warn("Could not read {}", RETROTWEAKS_OPTIONS_FILE, e);
		}
	}

	@Inject(method = "save", at = @At("TAIL"))
	private void retrotweaks$saveWithVanilla(CallbackInfo ci) {
		retrotweaks$save();
	}

	@Unique
	private void retrotweaks$save() {
		File file = retrotweaks$file();
		if (file == null) return;
		try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
			writer.println("fogDensity:" + ModOptions.fogDensity);
			writer.println("cloudHeight:" + ModOptions.cloudHeight);
			writer.println("fpsLimit:" + ModOptions.fpsLimit);
			writer.println("renderDistance:" + ModOptions.renderDistance);
			writer.println("brightness:" + ModOptions.brightness);
			writer.println("guiScale:" + ModOptions.realGuiScale);
			writer.println("fov:" + ModOptions.fov);
			writer.println("clouds:" + ModOptions.clouds);
		} catch (Exception e) {
			com.periut.retrotweaks.RetroTweaks.LOGGER.warn("Could not write {}", RETROTWEAKS_OPTIONS_FILE, e);
		}
	}

	@Unique
	private File retrotweaks$file() {
		if (this.minecraft == null) return null;
		return new File(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().toFile(), RETROTWEAKS_OPTIONS_FILE);
	}
}
