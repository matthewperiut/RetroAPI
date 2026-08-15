package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tilling grass can turn up seeds, as it did before beta 1.8. From BetaTweaks and UniTweaks.
 *
 * <p>Only adds the drop; the tilling itself is left to vanilla, which runs straight after.
 */
@Mixin(HoeItem.class)
public class HoeItemMixin {

	@Inject(method = "useOnBlock", at = @At("HEAD"))
	private void retrotweaks$hoeGrassForSeeds(ItemStack stack, PlayerEntity player, World world,
			int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.GAMEPLAY.oldFeatures.hoeGrassForSeeds || world.isRemote) return;
		if (side == 0 || world.getBlockId(x, y + 1, z) != 0) return;
		if (world.getBlockId(x, y, z) != Block.GRASS_BLOCK.id) return;
		// Same one-in-eight chance as breaking tall grass.
		if (world.random.nextInt(8) != 0) return;

		float spread = 0.7F;
		double ox = world.random.nextFloat() * spread + (1.0F - spread) * 0.5;
		double oz = world.random.nextFloat() * spread + (1.0F - spread) * 0.5;
		ItemEntity seeds = new ItemEntity(world, x + ox, y + 1.2, z + oz, new ItemStack(Item.SEEDS, 1, 0));
		seeds.pickupDelay = 10;
		world.spawnEntity(seeds);
	}
}
