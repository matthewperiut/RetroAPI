package com.periut.retroapi.mixin.register;

import com.llamalad7.mixinextras.sugar.Local;
import com.periut.retroapi.state.RetroBlockState;
import com.periut.retroapi.state.RetroStates;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Feeds the FULL flattened block-state index to the vanilla drop hooks so blocks with more than 16
 * states can emit metadata-preserving drops.
 *
 * <p>{@link Block#dropStacks(World, int, int, int, int, float)} hands the block's vanilla metadata
 * (the nibble, bits 0-3) to {@code getDroppedItemId(int)} / {@code getDroppedItemMeta(int)}. A RetroAPI
 * block whose flattened state index exceeds 15 stores the high bits in the chunk sidecar
 * ({@link RetroStates}), so the nibble alone can't reconstruct the state and the block can't decide
 * what metadata to drop. When such a block is being dropped we swap the nibble for the full index from
 * {@link RetroStates#get}, letting the block decode it with {@code RetroStates.fromIndex(this, meta)}.</p>
 *
 * <p>Only blocks with an explicit state definition wider than the nibble are affected; vanilla blocks,
 * implicit-meta blocks, and blocks with {@code <= 16} states keep receiving the plain nibble, so this is
 * a no-op for everything that worked before.</p>
 */
@Mixin(Block.class)
public class BlockDropStateMixin {

	@ModifyVariable(
		method = "dropStacks(Lnet/minecraft/world/World;IIIIF)V",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 3
	)
	private int retroapi$fullDropStateIndex(int meta,
			@Local(argsOnly = true) World world,
			@Local(argsOnly = true, ordinal = 0) int x,
			@Local(argsOnly = true, ordinal = 1) int y,
			@Local(argsOnly = true, ordinal = 2) int z) {
		Block self = (Block) (Object) this;
		if (!RetroStates.hasExplicitDefinition(self) || RetroStates.stateCount(self) <= 16) {
			return meta;
		}
		RetroBlockState state = RetroStates.get(world, x, y, z);
		return state == null ? meta : state.getIndex();
	}
}
