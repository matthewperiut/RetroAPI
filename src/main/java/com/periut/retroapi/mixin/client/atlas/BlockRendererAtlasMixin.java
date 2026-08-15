package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.BlockRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlockRenderManager.class)
@Environment(EnvType.CLIENT)
public class BlockRendererAtlasMixin {

	@ModifyConstant(
		method = {
			"renderBed", "renderRepeater",
			"renderPistonHeadYAxis", "renderPistonHeadZAxis", "renderPistonHeadXAxis",
			"renderLever", "renderFire", "renderRedstoneDust",
			"renderRail", "renderLadder",
			"renderTiltedTorch(Lnet/minecraft/block/Block;DDDDD)V",
			"renderCross(Lnet/minecraft/block/Block;IDDD)V",
			"renderCrop(Lnet/minecraft/block/Block;IDDD)V",
			"renderFluid",
			"renderBottomFace", "renderTopFace",
			"renderEastFace", "renderWestFace",
			"renderNorthFace", "renderSouthFace",
		},
		constant = @Constant(intValue = 240)
	)
	private int retroapi$fixRowMask(int original) {
		return -16;
	}

	@ModifyConstant(
		method = {
			"renderBed", "renderRepeater",
			"renderPistonHeadYAxis", "renderPistonHeadZAxis", "renderPistonHeadXAxis",
			"renderLever", "renderFire", "renderRedstoneDust",
			"renderRail", "renderLadder",
			"renderTiltedTorch(Lnet/minecraft/block/Block;DDDDD)V",
			"renderCross(Lnet/minecraft/block/Block;IDDD)V",
			"renderCrop(Lnet/minecraft/block/Block;IDDD)V",
			"renderFluid",
			"renderBottomFace", "renderTopFace",
			"renderEastFace", "renderWestFace",
			"renderNorthFace", "renderSouthFace",
		},
		constant = @Constant(doubleValue = 256.0)
	)
	private double retroapi$fixAtlasDivisorDouble(double original) {
		return AtlasExpander.terrainAtlasSize;
	}

	@ModifyConstant(
		method = {
			"renderBed", "renderRepeater",
			"renderPistonHeadYAxis", "renderPistonHeadZAxis", "renderPistonHeadXAxis",
			"renderLever", "renderFire", "renderRedstoneDust",
			"renderRail", "renderLadder",
			"renderTiltedTorch(Lnet/minecraft/block/Block;DDDDD)V",
			"renderCross(Lnet/minecraft/block/Block;IDDD)V",
			"renderCrop(Lnet/minecraft/block/Block;IDDD)V",
			"renderFluid",
			"renderBottomFace", "renderTopFace",
			"renderEastFace", "renderWestFace",
			"renderNorthFace", "renderSouthFace",
		},
		constant = @Constant(floatValue = 256.0F)
	)
	private float retroapi$fixAtlasDivisorFloat(float original) {
		return (float) AtlasExpander.terrainAtlasSize;
	}

	// --- Redstone dust's two UV offsets, both pixels/256 like the divisors above ---

	/**
	 * The step from the dust sprite to the overlay sprite one row below it, {@code 16/256}.
	 *
	 * <p>Redstone dust is drawn twice: the coloured pass from its own sprite, then a white pass from
	 * the sprite one atlas ROW down, which is where the second, uncoloured cross/line lives. One row is
	 * 16 pixels of the atlas, so on an expanded atlas the step shrinks with it - left at 1/16 it lands
	 * two rows down on a 512 sheet, on whatever modded texture happens to be there or on nothing at all.
	 *
	 * <p>Scoped to {@code renderRedstoneDust} on purpose: {@code 0.0625} appears all over this class as
	 * a plain one-sixteenth of a block, and every one of those is geometry that must not move.
	 */
	@ModifyConstant(method = "renderRedstoneDust", constant = @Constant(doubleValue = 0.0625))
	private double retroapi$fixDustOverlayRow(double original) {
		return 16.0 / AtlasExpander.terrainAtlasSize;
	}

	/** The 5-pixel inset the sprite gets on a side the dust does not connect to, {@code 5/256}. */
	@ModifyConstant(method = "renderRedstoneDust", constant = @Constant(doubleValue = 0.01953125))
	private double retroapi$fixDustInset(double original) {
		return 5.0 / AtlasExpander.terrainAtlasSize;
	}

	// --- Torch top face UV offsets (pixel / 256) ---

	@ModifyConstant(
		method = "renderTiltedTorch(Lnet/minecraft/block/Block;DDDDD)V",
		constant = @Constant(doubleValue = 0.02734375)
	)
	private double retroapi$fixTorchTopU1(double original) {
		return 7.0 / AtlasExpander.terrainAtlasSize;
	}

	@ModifyConstant(
		method = "renderTiltedTorch(Lnet/minecraft/block/Block;DDDDD)V",
		constant = @Constant(doubleValue = 0.0234375)
	)
	private double retroapi$fixTorchTopV1(double original) {
		return 6.0 / AtlasExpander.terrainAtlasSize;
	}

	@ModifyConstant(
		method = "renderTiltedTorch(Lnet/minecraft/block/Block;DDDDD)V",
		constant = @Constant(doubleValue = 0.03515625)
	)
	private double retroapi$fixTorchTopU2(double original) {
		return 9.0 / AtlasExpander.terrainAtlasSize;
	}

	@ModifyConstant(
		method = "renderTiltedTorch(Lnet/minecraft/block/Block;DDDDD)V",
		constant = @Constant(doubleValue = 0.03125)
	)
	private double retroapi$fixTorchTopV2(double original) {
		return 8.0 / AtlasExpander.terrainAtlasSize;
	}
}

