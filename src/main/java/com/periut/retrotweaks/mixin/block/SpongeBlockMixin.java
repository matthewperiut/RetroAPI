package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.SpongeBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Sponges actually soak up water. From NowObtainableRecipes.
 *
 * <p>b1.7.3's sponge walks a 5x5x5 cube looking for water and then does nothing with it - the body
 * of the loop is empty. This fills that loop in.
 */
@Mixin(SpongeBlock.class)
public class SpongeBlockMixin {

	@Inject(method = "onPlaced(Lnet/minecraft/world/World;III)V", at = @At("TAIL"))
	private void retrotweaks$soakUpWater(World world, int x, int y, int z, CallbackInfo ci) {
		if (!Config.WORLD.spongeSoaksUpWater) return;
		int radius = 2;
		for (int dx = x - radius; dx <= x + radius; dx++) {
			for (int dy = y - radius; dy <= y + radius; dy++) {
				for (int dz = z - radius; dz <= z + radius; dz++) {
					if (world.getMaterial(dx, dy, dz) == Material.WATER) {
						world.setBlock(dx, dy, dz, 0);
					}
				}
			}
		}
	}
}
