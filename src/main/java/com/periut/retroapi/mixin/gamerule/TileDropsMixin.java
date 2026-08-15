package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code doTileDrops}: a broken block yields nothing.
 *
 * <p>Both {@code dropStacks} overloads are covered - the one that takes a drop chance and the one
 * that does not - because a block picks whichever suits it and missing either would leak drops.
 */
@Mixin(Block.class)
public class TileDropsMixin {

	@Inject(method = "dropStacks(Lnet/minecraft/world/World;IIIIF)V", at = @At("HEAD"), cancellable = true)
	private void retroapi$doTileDropsWithChance(World world, int x, int y, int z, int meta, float chance, CallbackInfo ci) {
		if (!RetroGameRules.getBoolean(RetroGameRules.DO_TILE_DROPS)) {
			ci.cancel();
		}
	}

	@Inject(method = "dropStacks(Lnet/minecraft/world/World;IIII)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$doTileDrops(World world, int x, int y, int z, int meta, CallbackInfo ci) {
		if (!RetroGameRules.getBoolean(RetroGameRules.DO_TILE_DROPS)) {
			ci.cancel();
		}
	}
}
