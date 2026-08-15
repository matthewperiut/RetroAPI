package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.BlockView;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Beta 1.8 leaf shading: leaves are drawn dark regardless of the light reaching them. From UniTweaks.
 *
 * <p>Client-only: {@code Block.getLuminance} does not exist in the server jar.
 *
 * <p>Purely cosmetic - it makes tree canopies read as solid masses the way they did in 1.8 rather
 * than the brighter, flatter b1.7.3 look.
 */
@Environment(EnvType.CLIENT)
@Mixin(Block.class)
public class BlockLeafLightingMixin {

	@Shadow @Final public static int[] BLOCKS_LIGHT_LUMINANCE;
	@Shadow @Final public int id;

	@Inject(method = "getLuminance", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$darkenLeaves(BlockView blockView, int x, int y, int z, CallbackInfoReturnable<Float> cir) {
		if (!Config.WORLD.flora.beta18LeavesRendering) return;
		if (blockView.getMaterial(x, y, z) != Material.LEAVES) return;
		float luminance = blockView.getNaturalBrightness(x, y, z, BLOCKS_LIGHT_LUMINANCE[this.id]);
		cir.setReturnValue(Math.min(luminance, 0.2F));
	}
}
