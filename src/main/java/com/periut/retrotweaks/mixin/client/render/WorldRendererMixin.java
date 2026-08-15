package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.WorldRenderer;

import org.lwjgl.input.Mouse;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sizes the chunk render grid from RetroTweaks' render-distance slider instead of vanilla's four fixed
 * tiers. From UniTweaks' {@code mixin.tweaks.renderdistance.WorldRendererMixin}.
 *
 * <p>{@code WorldRenderer.reload()} is what actually allocates the chunk grid that gets drawn
 * ({@code chunkCountX}/{@code chunkCountZ}, from a {@code j} that vanilla derives from
 * {@code GameOptions.viewDistance} and clamps to 400 blocks), and {@code render()} re-triggers
 * {@code reload()} whenever {@code GameOptions.viewDistance} no longer matches the grid's own
 * {@code lastViewDistance}. Both are patched here, the same way UniTweaks does it:
 *
 * <ul>
 *   <li>{@code reload()}'s three field writes ({@code lastViewDistance}, {@code chunkCountX},
 *   {@code chunkCountZ}) are each overridden right after vanilla makes them - which is what steps
 *   around the 400-block clamp entirely, since that clamp has already run by the time these fire and
 *   nothing here has to touch it. See {@link ModOptions#MAX_RENDER_DISTANCE_CHUNKS} for the bound this
 *   stands in for it.
 *   <li>{@code render()}'s {@code GameOptions.viewDistance != lastViewDistance} check reads RetroTweaks'
 *   own chunk count while the mouse is up, so a reload actually fires once the slider settles on a new
 *   value, but reads back {@code lastViewDistance} itself while a mouse button is down so the check is
 *   never true - and no reload attempted - on every frame mid-drag.
 * </ul>
 */
@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

	@Shadow private int lastViewDistance;
	@Shadow private int chunkCountX;
	@Shadow private int chunkCountZ;

	@WrapOperation(method = "render", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
		target = "Lnet/minecraft/client/option/GameOptions;viewDistance:I"), require = 1)
	private int retrotweaks$fixRebuildCheck(GameOptions instance, Operation<Integer> original) {
		if (ModOptions.enabled(ModOptions.renderDistanceOption)) {
			return Mouse.isButtonDown(0) ? this.lastViewDistance : ModOptions.renderDistanceChunks();
		}
		return original.call(instance);
	}

	@Inject(method = "reload", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
		target = "Lnet/minecraft/client/render/WorldRenderer;lastViewDistance:I", shift = At.Shift.AFTER, ordinal = 0),
		require = 1)
	private void retrotweaks$overrideLastViewDistance(CallbackInfo ci) {
		if (ModOptions.enabled(ModOptions.renderDistanceOption)) {
			this.lastViewDistance = ModOptions.renderDistanceChunks();
		}
	}

	@Inject(method = "reload", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
		target = "Lnet/minecraft/client/render/WorldRenderer;chunkCountX:I", shift = At.Shift.AFTER, ordinal = 0),
		require = 1)
	private void retrotweaks$overrideChunkCountX(CallbackInfo ci) {
		if (ModOptions.enabled(ModOptions.renderDistanceOption)) {
			this.chunkCountX = ModOptions.renderDistanceChunks() * 2 + 1;
		}
	}

	@Inject(method = "reload", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
		target = "Lnet/minecraft/client/render/WorldRenderer;chunkCountZ:I", shift = At.Shift.AFTER, ordinal = 0),
		require = 1)
	private void retrotweaks$overrideChunkCountZ(CallbackInfo ci) {
		if (ModOptions.enabled(ModOptions.renderDistanceOption)) {
			this.chunkCountZ = ModOptions.renderDistanceChunks() * 2 + 1;
		}
	}
}
