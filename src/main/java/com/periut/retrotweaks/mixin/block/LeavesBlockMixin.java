package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import net.minecraft.block.LeavesBlock;
import net.minecraft.block.TransparentBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * Player-placed leaves stop decaying, and oak leaves can drop apples. From MiscTweaks.
 *
 * <p>Vanilla does not set a "player placed" bit itself - {@link
 * com.periut.retrotweaks.mixin.item.LeavesItemMixin} is what marks bit 0x4 on placement;
 * this mixin only reads it back. (Vanilla's own {@code LeavesBlockItem.getPlacementMetadata} ORs
 * in bit 0x8 instead, an unrelated decay-scan trigger flag consumed by {@code onTick}/{@code
 * onBreak} - not the same bit, and not persistence.)
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends TransparentBlock {

	protected LeavesBlockMixin(int id, int textureId, Material material, boolean transparent) {
		super(id, textureId, material, transparent);
	}

	/** Metadata bit marking a leaf block as placed by a player rather than grown. */
	@Unique
	private static final int RETROTWEAKS_PLACED_BIT = 0x4;

	/** Metadata bits holding the leaf type; 0 is oak, the only one that drops apples. */
	@Unique
	private static final int RETROTWEAKS_TYPE_MASK = 0x3;

	@Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$placedLeavesPersist(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		if (!Config.BLOCKS.playerPlacedLeafPersistence || world.isRemote) return;
		if ((world.getBlockMeta(x, y, z) & RETROTWEAKS_PLACED_BIT) != 0) ci.cancel();
	}

	@Inject(method = "onTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/LeavesBlock;breakLeaves(Lnet/minecraft/world/World;III)V"))
	private void retrotweaks$fastLeafDecay(World world, int x, int y, int z, Random random, CallbackInfo ci) {
		if (!Config.WORLD.flora.fastLeafDecay || world.isRemote) return;
		// When one leaf block decays, tell its neighbours to check themselves soon rather than
		// waiting for their own random tick - that is the whole trick, the decay rules are vanilla.
		int min = Config.WORLD.flora.minimumDecayTime;
		int max = Math.max(min + 1, Config.WORLD.flora.maximumDecayTime);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					world.scheduleBlockUpdate(x + dx, y + dy, z + dz, this.id, min + random.nextInt(max - min));
				}
			}
		}
	}

	@Inject(method = "breakLeaves", at = @At("HEAD"))
	private void retrotweaks$appleFromDecay(World world, int x, int y, int z, CallbackInfo ci) {
		retrotweaks$tryDropApple(world, x, y, z, world.getBlockMeta(x, y, z) & RETROTWEAKS_TYPE_MASK);
	}

	@Inject(method = "afterBreak", at = @At("HEAD"))
	private void retrotweaks$appleFromBreaking(World world, PlayerEntity player, int x, int y, int z, int meta, CallbackInfo ci) {
		// Shearing collects the leaf block itself, so it should not also shake an apple loose.
		ItemStack held = player == null ? null : player.getHand();
		if (held != null && held.itemId == Item.SHEARS.id) return;
		retrotweaks$tryDropApple(world, x, y, z, meta & RETROTWEAKS_TYPE_MASK);
	}

	@Unique
	private void retrotweaks$tryDropApple(World world, int x, int y, int z, int leafType) {
		float chance = Config.WORLD.flora.appleDropChance;
		if (chance <= 0.0F || leafType != 0 || world.isRemote) return;
		// The option is a percentage, so 0.5 means the modern 0.5% chance rather than one in two.
		if (world.random.nextFloat() >= chance / 100.0F) return;

		double spread = 0.7;
		double ox = world.random.nextFloat() * spread + (1.0 - spread) * 0.5;
		double oy = world.random.nextFloat() * spread + (1.0 - spread) * 0.5;
		double oz = world.random.nextFloat() * spread + (1.0 - spread) * 0.5;
		ItemEntity apple = new ItemEntity(world, x + ox, y + oy, z + oz, new ItemStack(Item.APPLE));
		apple.pickupDelay = 10;
		world.spawnEntity(apple);
	}
}
