package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.PumpkinBlock;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets pumpkins and jack o' lanterns be placed like any other block.
 *
 * <p>Vanilla additionally requires a suffocating block underneath, which is why a pumpkin cannot be
 * put on a fence, a slab or a chest. From AnnoyanceFix and UniTweaks.
 */
@Mixin(PumpkinBlock.class)
public class PumpkinBlockMixin {

	@Inject(method = "canPlaceAt", at = @At("RETURN"), cancellable = true)
	private void retrotweaks$placeAnywhere(World world, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.BLOCKS.pumpkinPlacementFixes) return;
		int blockId = world.getBlockId(x, y, z);
		cir.setReturnValue(blockId == 0 || Block.BLOCKS[blockId].material.isReplaceable());
	}
}
