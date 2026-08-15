package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The frame limit slider, replacing vanilla's three-step Performance setting. From UniTweaks
 * (mixin.tweaks.fpslimitslider.GameRendererMixin).
 *
 * <p>Vanilla's {@code onFrameUpdate} picks a target of 200, 120 or 40 fps into a local ("n")
 * depending on the Performance option, then paces {@code renderFrame} against it. This hooks the
 * *reads* that pick those tiers - the {@code CONSTANT 200} and the five {@code GameOptions.fpsLimit}
 * field reads - rather than the local itself, exactly as UniTweaks does. A bare {@code
 * @ModifyVariable(... ordinal = 5, require = 0)} on a method literally named {@code "render"} was
 * tried here before (finding #51): that method does not exist on {@code GameRenderer} at all - the
 * real method is {@code onFrameUpdate} - so the injection never applied and {@code require = 0}
 * hid the miss completely. Field/constant reads are also the more stable target UniTweaks itself
 * chose over a local ordinal.
 *
 * <p>Stands aside for RetroDragon, which owns frame pacing on its own backend - two limiters would
 * fight and the result would be worse than either. {@link ModOptions#fpsLimitActive()} folds that
 * check in alongside the config toggle so every handler below stands down together. This is the
 * numeric-cap half of the slider only; the VSync stop is handled entirely by {@code
 * FpsLimitSyncMixin} and, unlike this file, applies whether or not RetroDragon is present - see that
 * class for why a driver-level swap interval does not have the same fighting problem a sleep-based
 * cap does.
 *
 * <p>UniTweaks' own {@code getFpsLimitValue()} never returns a value that means "unlimited" (it
 * ranges 5-300), so it substitutes its slider's value into "n" unconditionally. RetroTweaks' slider
 * additionally has a VSync stop, at which {@link ModOptions#frameLimit()} reads 0 - feeding that into
 * "n" would divide by zero the moment a world loads, same as the old "unlimited means 0" case did.
 * {@code retrotweaks$modifyFpsTarget} therefore only substitutes {@link ModOptions#frameLimit()} in
 * {@link ModOptions.FrameLimitMode#CAPPED}; at the VSync and Unlimited stops it leaves vanilla's
 * target alone, and {@code FpsLimitSyncMixin} simply never calls {@code Display.sync} there either -
 * VSync paces frames through the driver instead, not through this sleep-based path.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class FpsLimitMixin {

	@ModifyExpressionValue(method = "onFrameUpdate",
		at = @At(value = "CONSTANT", args = "intValue=200", ordinal = 0), require = 1)
	private int retrotweaks$modifyFpsTarget(int original) {
		if (ModOptions.fpsLimitActive() && ModOptions.frameLimitMode() == ModOptions.FrameLimitMode.CAPPED) {
			return ModOptions.frameLimit();
		}
		return original;
	}

	@ModifyExpressionValue(method = "onFrameUpdate",
		at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
			target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 0), require = 1)
	private int retrotweaks$overridePerformanceLevel1(int original) {
		if (ModOptions.fpsLimitActive()) {
			return -1;
		}
		return original;
	}

	@ModifyExpressionValue(method = "onFrameUpdate",
		at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
			target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 1), require = 1)
	private int retrotweaks$overridePerformanceLevel2(int original) {
		if (ModOptions.fpsLimitActive()) {
			return -1;
		}
		return original;
	}

	@ModifyExpressionValue(method = "onFrameUpdate",
		at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
			target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 1), require = 1)
	private int retrotweaks$overridePerformanceLevel0(int original) {
		if (ModOptions.fpsLimitActive()) {
			return ModOptions.frameLimited() ? 2 : 0;
		}
		return original;
	}

	@ModifyExpressionValue(method = "onFrameUpdate",
		at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
			target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 3), require = 1)
	private int retrotweaks$nukeSleep(int original) {
		if (ModOptions.fpsLimitActive()) {
			return -1;
		}
		return original;
	}

	@ModifyExpressionValue(method = "onFrameUpdate",
		at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
			target = "Lnet/minecraft/client/option/GameOptions;fpsLimit:I", ordinal = 4), require = 1)
	private int retrotweaks$redirectMenuFpsLimit(int original) {
		if (ModOptions.fpsLimitActive()) {
			return ModOptions.frameLimited() ? 2 : 0;
		}
		return original;
	}
}
