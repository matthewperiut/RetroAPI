package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SlabBlockItem;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The deliberate-click half of slab placement: clicking the top face of a slab with a matching
 * slab item merges the two into a double slab, since {@link com.periut.retrotweaks.mixin.block.SlabBlockMixin}
 * cancels vanilla's automatic merge-on-placement. Also carries the plant-replacement placement
 * fix, which the source mods folded into the same override, and a crash guard for a damaged slab
 * item whose meta exceeds {@link SlabBlock#names}.length.
 *
 * <p>An override rather than an inject: {@code SlabBlockItem} does not declare {@code useOnBlock},
 * it inherits it from {@code BlockItem}. From AnnoyanceFix and UniTweaksTelsAddons (the merge) and
 * WhatAreYouScoring (the crash guard).
 */
@Mixin(SlabBlockItem.class)
public abstract class SlabBlockItemMixin extends BlockItem {

	protected SlabBlockItemMixin(int id) {
		super(id);
	}

	@Override
	public boolean useOnBlock(ItemStack stack, PlayerEntity user, World world, int x, int y, int z, int side) {
		if (!Config.BLOCKS.slabPlacementFixes) return super.useOnBlock(stack, user, world, x, y, z, side);

		if (side == 1
				&& world.getBlockId(x, y, z) == Block.SLAB.id
				&& world.getBlockMeta(x, y, z) == stack.getDamage()) {
			if (stack.count == 0) {
				return false;
			} else {
				Block doubleSlab = Block.BLOCKS[Block.DOUBLE_SLAB.id];
				if (world.setBlock(x, y, z, Block.DOUBLE_SLAB.id, this.getPlacementMetadata(stack.getDamage()))) {
					Block.BLOCKS[Block.DOUBLE_SLAB.id].onPlaced(world, x, y, z, side);
					Block.BLOCKS[Block.DOUBLE_SLAB.id].onPlaced(world, x, y, z, user);
					if (Config.SOUNDS.slabMergeSound) world.playSound((double) ((float) x + 0.5F), (double) ((float) y + 0.5F), (double) ((float) z + 0.5F),
							doubleSlab.soundGroup.getSound(),
							(doubleSlab.soundGroup.getVolume() + 1.0F) / 2.0F,
							doubleSlab.soundGroup.getPitch() * 0.8F);
					--stack.count;
				}

				return true;
			}
		}

		int blockId = world.getBlockId(x, y, z);
		boolean replaceBlock = false;

		if (Config.BLOCKS.plantReplacementFixes
				&& (Block.GRASS.id == blockId || Block.DEAD_BUSH.id == blockId)) {
			side = 0;
			replaceBlock = true;
		} else if (Block.SNOW.id == blockId) {
			side = 0;
		} else {
			if (side == 0) --y;
			if (side == 1) ++y;
			if (side == 2) --z;
			if (side == 3) ++z;
			if (side == 4) --x;
			if (side == 5) ++x;
		}

		if (stack.count == 0) {
			return false;
		} else if (y == 127 && Block.BLOCKS[this.id].material.isSolid()) {
			return false;
		} else if (world.canPlace(this.id, x, y, z, false, side) || replaceBlock) {
			Block block = Block.BLOCKS[this.id];
			if (world.setBlock(x, y, z, this.id, this.getPlacementMetadata(stack.getDamage()))) {
				Block.BLOCKS[this.id].onPlaced(world, x, y, z, side);
				Block.BLOCKS[this.id].onPlaced(world, x, y, z, user);
				if (Config.SOUNDS.slabMergeSound) world.playSound((double) ((float) x + 0.5F), (double) ((float) y + 0.5F), (double) ((float) z + 0.5F),
						block.soundGroup.getSound(),
						(block.soundGroup.getVolume() + 1.0F) / 2.0F,
						block.soundGroup.getPitch() * 0.8F);
				--stack.count;
			}

			return true;
		} else {
			return false;
		}
	}

	@Environment(EnvType.CLIENT)
	@Inject(method = "getTranslationKey(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$preventCrash(ItemStack stack, CallbackInfoReturnable<String> cir) {
		if (stack.getDamage() >= SlabBlock.names.length) {
			cir.setReturnValue(null);
		}
	}
}
