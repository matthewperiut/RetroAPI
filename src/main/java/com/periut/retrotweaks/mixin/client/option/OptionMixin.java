package com.periut.retrotweaks.mixin.client.option;

import com.periut.retrotweaks.feature.options.ModOptions;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.Option;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Appends RetroTweaks' video settings to vanilla's {@code Option} enum. From UniTweaks.
 *
 * <p>An enum cannot be extended, so the constants are built with the enum's own (synthetic)
 * constructor and written into its values array from inside its class initialiser - before anything
 * has had a chance to read it. Everything downstream then treats them as ordinary options.
 *
 * <p>They are always added, whatever the config says: the enum initialises once per launch, so
 * making membership conditional would put the "restart required" back that this was meant to
 * remove. What the config decides is whether each one is *shown* and *acted on*, which is checked
 * where those things happen.
 */
@Environment(EnvType.CLIENT)
@Mixin(Option.class)
public class OptionMixin {

	@Shadow @Final @Mutable
	private static Option[] f_13503015;

	@Invoker("<init>")
	private static Option retrotweaks$newOption(String name, int ordinal, String key, boolean slider, boolean toggle) {
		throw new AssertionError("Replaced by Mixin at runtime");
	}

	@Inject(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC,
		target = "Lnet/minecraft/client/option/Option;f_13503015:[Lnet/minecraft/client/option/Option;", shift = At.Shift.AFTER))
	private static void retrotweaks$addVideoOptions(CallbackInfo ci) {
		List<Option> options = new ArrayList<>(Arrays.asList(f_13503015));
		int next = options.size();

		ModOptions.fogDensityOption = retrotweaks$add(options, "RETROTWEAKS_FOG_DENSITY", next++, "options.fogDensity", true, false);
		ModOptions.cloudsOption = retrotweaks$add(options, "RETROTWEAKS_CLOUDS", next++, "options.clouds", false, true);
		ModOptions.cloudHeightOption = retrotweaks$add(options, "RETROTWEAKS_CLOUD_HEIGHT", next++, "options.cloudHeight", true, false);
		ModOptions.fpsLimitOption = retrotweaks$add(options, "RETROTWEAKS_FPS_LIMIT", next++, "options.fpsLimit", true, false);
		ModOptions.renderDistanceOption = retrotweaks$add(options, "RETROTWEAKS_RENDER_DISTANCE", next++, "options.renderDistanceSlider", true, false);
		ModOptions.brightnessOption = retrotweaks$add(options, "RETROTWEAKS_BRIGHTNESS", next++, "options.brightness", true, false);
		ModOptions.guiScaleOption = retrotweaks$add(options, "RETROTWEAKS_GUI_SCALE", next++, "options.guiScaleSlider", true, false);
		ModOptions.fovOption = retrotweaks$add(options, "RETROTWEAKS_FOV", next, "options.fov", true, false);

		f_13503015 = options.toArray(new Option[0]);
	}

	private static Option retrotweaks$add(List<Option> options, String name, int ordinal, String key, boolean slider, boolean toggle) {
		Option option = retrotweaks$newOption(name, ordinal, key, slider, toggle);
		options.add(option);
		return option;
	}
}
