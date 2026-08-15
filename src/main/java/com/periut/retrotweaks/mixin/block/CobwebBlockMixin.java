package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.block.CobwebBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Shears (and swords) collect cobwebs instead of turning them into string.
 *
 * <p>From AnnoyanceFix. The tool used is not passed to {@code getDroppedItemId}, so it is recorded
 * when the block is broken and read back a moment later on the same thread.
 */
@Mixin(CobwebBlock.class)
public abstract class CobwebBlockMixin extends Block {

	protected CobwebBlockMixin(int id, Material material) {
		super(id, material);
	}

	@Unique
	private boolean retrotweaks$brokenByShears;

	@Override
	public void afterBreak(World world, PlayerEntity player, int x, int y, int z, int meta) {
		retrotweaks$brokenByShears = Config.BLOCKS.cobwebFixes && retrotweaks$holdingShears(player);
		super.afterBreak(world, player, x, y, z, meta);
	}

	@Inject(method = "getDroppedItemId", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dropCobweb(int blockMeta, Random random, CallbackInfoReturnable<Integer> cir) {
		if (!Config.BLOCKS.cobwebFixes || !retrotweaks$brokenByShears) return;
		retrotweaks$brokenByShears = false;
		cir.setReturnValue(this.id);
	}

	@Unique
	private static boolean retrotweaks$holdingShears(PlayerEntity player) {
		if (player == null || player.inventory == null) return false;
		ItemStack held = player.inventory.getSelectedItem();
		return held != null && held.itemId == Item.SHEARS.id;
	}
}
