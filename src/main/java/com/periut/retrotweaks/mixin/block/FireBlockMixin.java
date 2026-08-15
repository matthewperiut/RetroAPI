package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.block.FireBlock;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Fire spread rate, endless fire, and fire scorching grass. From BetaTweaks and MiscTweaks.
 *
 * <p>Fire ages by one every tick; at age 15 it dies. Forcing newly spread fire back to age 0 is
 * what makes it burn forever.
 *
 * <p>Reworked: BetaTweaks redirected the block placement and returned {@code false} unconditionally,
 * so with "infinite fire spread" switched OFF the redirect swallowed the call and fire stopped
 * spreading at all. Here the original call always happens; only the metadata changes.
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin extends Block {

	protected FireBlockMixin(int id, Material material) {
		super(id, material);
	}

	@ModifyConstant(method = "getTickRate", constant = @Constant(intValue = 40), require = 0)
	private int retrotweaks$fireSpreadTickRate(int vanilla) {
		// 40 is the beta 1.6+ rate; alpha used 10, which spreads far more aggressively.
		return Config.WORLD.fire.fireSpreadTickRate;
	}

	@WrapOperation(method = "onTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlock(IIIII)Z"))
	private boolean retrotweaks$spreadForever(World world, int x, int y, int z, int id, int meta, Operation<Boolean> original) {
		if (Config.WORLD.fire.infiniteFireSpread && id == Block.FIRE.id) {
			return original.call(world, x, y, z, id, 0);
		}
		return original.call(world, x, y, z, id, meta);
	}

	@Override
	public void onBreak(World world, int x, int y, int z) {
		super.onBreak(world, x, y, z);
		if (!Config.WORLD.fire.fireTurnsGrassIntoDirt) return;
		if (world.getBlockId(x, y - 1, z) == Block.GRASS_BLOCK.id) {
			world.setBlock(x, y - 1, z, Block.DIRT.id);
		}
	}
}
