package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.block.Block;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.GrassBlock;
import net.minecraft.client.color.world.GrassColors;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.world.BlockView;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * UniTweaks-derived fixes to the block renderer: fence bottom-face lighting, the grass block item's
 * tinted top, and the missing bottom face of a torch's flame quad.
 *
 * <p>Fences: the four getLuminance lookups that shade a fence's bottom-face vertices sample the
 * block below instead of the fence's own cell, so they're pushed up by one when the queried block
 * is a fence. From UniTweaks.
 *
 * <p>Everything in this class is a feature UniTweaks also ships, patched onto the same instructions
 * its own mixins use, so the whole class is skipped when UniTweaks is installed and its versions run
 * instead (see {@code RetroTweaksMixinPlugin.UNITWEAKS_SUPERSEDED_MIXINS}). RetroTweaks' own
 * rotated-log renderer must NOT stand down with them, which is why it lives in
 * {@link RotatedLogRenderMixin} rather than here.
 */
@Mixin(BlockRenderManager.class)
public abstract class BlockRenderManagerMixin {

	@Shadow public boolean inventoryColorEnabled;

	// ------------------------------------------------------------------ grass block item, top face
	//
	// The other half of "Grass Block Item Fix". GrassBlockMixin gives the item the grass texture on
	// top; without this it arrives grey, because the inventory renderer multiplies the whole block by
	// Block.getColor(meta) and GrassBlock - unlike LeavesBlock - never overrides that, so it answers
	// white. Overriding getColor on GrassBlock is not the fix: it is applied to all six faces at once
	// and would turn the dirt sides green too. The tint has to go on just before the top face and come
	// off again straight after, which is exactly what UniTweaks' own version of this fix does.
	//
	// Every face is its own startQuads()/renderXFace()/draw() sequence, and the GL colour is what the
	// tessellator reads at draw() - NOT at the point the face's vertices are handed to it. So the tint
	// goes on before renderTopFace and comes off after the draw() that follows it, tracked with a flag
	// rather than an ordinal: this method renders several block shapes and calls renderTopFace in four
	// of them, so counting draw() calls would be counting the wrong ones for every shape but the cube.
	//
	// The colour is scaled the same way vanilla scales its own: by brightness when inventoryColorEnabled
	// is set, and not at all when it is not. That flag is NOT a condition for tinting - ItemRenderer
	// clears it for the inventory icon (it means "the item has a custom display colour", which a grass
	// block does not), and gating on it is what left the icon grey.

	@Unique
	private boolean retrotweaks$grassTopTinted;

	@Inject(method = "render(Lnet/minecraft/block/Block;IF)V", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/block/BlockRenderManager;renderTopFace(Lnet/minecraft/block/Block;DDDI)V"))
	private void retrotweaks$tintGrassTop(Block block, int meta, float brightness, CallbackInfo ci) {
		if (!Config.BUGFIXES.grassBlockItemFix || !(block instanceof GrassBlock)) return;
		int color = GrassColors.getColor(0.5, 1.0);
		float scale = retrotweaks$itemColorScale(brightness);
		GL11.glColor4f((color >> 16 & 255) / 255.0F * scale,
			(color >> 8 & 255) / 255.0F * scale,
			(color & 255) / 255.0F * scale, 1.0F);
		retrotweaks$grassTopTinted = true;
	}

	@Inject(method = "render(Lnet/minecraft/block/Block;IF)V", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/Tessellator;draw()V", shift = At.Shift.AFTER))
	private void retrotweaks$untintAfterGrassTop(Block block, int meta, float brightness, CallbackInfo ci) {
		if (!retrotweaks$grassTopTinted) return;
		retrotweaks$grassTopTinted = false;
		// Back to the neutral the remaining faces expect: white, scaled exactly as the tint was.
		float scale = retrotweaks$itemColorScale(brightness);
		GL11.glColor4f(scale, scale, scale, 1.0F);
	}

	@Unique
	private float retrotweaks$itemColorScale(float brightness) {
		return this.inventoryColorEnabled ? brightness : 1.0F;
	}

	// This is to reduce the amount of instanceof checks from 4 to 1
	@Unique
	private boolean retrotweaks$isFence = false;

	@WrapOperation(method = "renderSmooth", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getLuminance(Lnet/minecraft/world/BlockView;III)F", ordinal = 7))
	private float retrotweaks$fenceLuminance1(Block block, BlockView blockView, int x, int y, int z, Operation<Float> original) {
		retrotweaks$isFence = block instanceof FenceBlock && Config.BUGFIXES.fenceLightingFix;

		if (retrotweaks$isFence) {
			return original.call(block, blockView, x, y + 1, z);
		}
		return original.call(block, blockView, x, y, z);
	}

	@WrapOperation(method = "renderSmooth", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getLuminance(Lnet/minecraft/world/BlockView;III)F", ordinal = 8))
	private float retrotweaks$fenceLuminance2(Block block, BlockView blockView, int x, int y, int z, Operation<Float> original) {
		if (retrotweaks$isFence) {
			return original.call(block, blockView, x, y + 1, z);
		}
		return original.call(block, blockView, x, y, z);
	}

	@WrapOperation(method = "renderSmooth", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getLuminance(Lnet/minecraft/world/BlockView;III)F", ordinal = 9))
	private float retrotweaks$fenceLuminance3(Block block, BlockView blockView, int x, int y, int z, Operation<Float> original) {
		if (retrotweaks$isFence) {
			return original.call(block, blockView, x, y + 1, z);
		}
		return original.call(block, blockView, x, y, z);
	}

	@WrapOperation(method = "renderSmooth", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getLuminance(Lnet/minecraft/world/BlockView;III)F", ordinal = 10))
	private float retrotweaks$fenceLuminance4(Block block, BlockView blockView, int x, int y, int z, Operation<Float> original) {
		if (retrotweaks$isFence) {
			return original.call(block, blockView, x, y + 1, z);
		}
		return original.call(block, blockView, x, y, z);
	}

	/**
	 * The tilted-torch flame quad (the top diamond of a wall/standing torch) has no bottom face,
	 * so looking up at a wall torch from below shows a gap straight through it. Adds the missing
	 * quad after the flame's fourth vertex. From UniTweaks.
	 */
	@Inject(method = "renderTiltedTorch", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/Tessellator;vertex(DDDDD)V", ordinal = 3, shift = At.Shift.AFTER))
	private void retrotweaks$torchBottomFace(Block block, double x, double y, double z, double xTilt, double zTilt, CallbackInfo ci,
			@Local Tessellator tessellator,
			@Local(ordinal = 13) double l,
			@Local(ordinal = 5) double d,
			@Local(ordinal = 6) double e,
			@Local(ordinal = 7) double f17,
			@Local(ordinal = 8) double g18
	) {
		if (!Config.BUGFIXES.torchBottomFaceFix) return;

		// UniTweaks reads this from StationAPI's block atlas; RetroTweaks has no dynamic atlas, so
		// use vanilla terrain.png's fixed height (the value Util.atlasHeight also defaults to).
		float shift = 4F / 256F;

		tessellator.vertex(x + l + xTilt, y, z - l + zTilt, f17, e + shift);
		tessellator.vertex(x + l + xTilt, y, z + l + zTilt, f17, g18 + shift);
		tessellator.vertex(x - l + xTilt, y, z + l + zTilt, d, g18 + shift);
		tessellator.vertex(x - l + xTilt, y, z - l + zTilt, d, e + shift);
	}
}
