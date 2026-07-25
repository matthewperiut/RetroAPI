package com.periut.retroapi.mixin.register;

import com.periut.retroapi.state.RetroBlockState;
import com.periut.retroapi.state.RetroStates;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Restores a block's FULL state when its item is placed.
 *
 * <p>An item stack carries block data in its damage value, and vanilla feeds that straight into
 * {@code world.setBlock}, which only has the 4-bit metadata nibble to put it in. So a RetroAPI block with
 * more than 16 states - whose extra bits live in the region sidecar - lost everything above the nibble the
 * moment it was broken and replaced: it dropped a stack that knew its state (see
 * {@code BlockDropStateMixin}) and then placed a block that did not.
 *
 * <p>This closes the round trip: after vanilla places the block, the stack's damage is decoded as a
 * flattened state index and written back through {@link RetroStates}, nibble and sidecar together. Only
 * blocks with an explicit state definition are touched, so vanilla blocks (and RetroAPI blocks that just
 * use plain metadata) behave exactly as before.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemStateMixin {

	@Shadow public int blockId;

	@Unique private boolean retroapi$clickedReplaceable;

	@Inject(method = "useOnBlock", at = @At("HEAD"))
	private void retroapi$rememberReplaceable(ItemStack stack, PlayerEntity player, World world,
			int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		// Vanilla places INTO a snow layer instead of against it, so the placement position depends on
		// what was clicked. Sample it before the block is replaced.
		this.retroapi$clickedReplaceable = world.getBlockId(x, y, z) == Block.SNOW.id;
	}

	@Inject(method = "useOnBlock", at = @At("RETURN"))
	private void retroapi$applyPlacedState(ItemStack stack, PlayerEntity player, World world,
			int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		if (!Boolean.TRUE.equals(cir.getReturnValue())) {
			return; // nothing was placed
		}
		int index = stack.getDamage();
		if (index <= 0) {
			return; // the default state needs no repair
		}
		Block block = this.blockId > 0 && this.blockId < Block.BLOCKS.length ? Block.BLOCKS[this.blockId] : null;
		if (block == null || !RetroStates.hasExplicitDefinition(block)) {
			return; // vanilla metadata semantics: leave it alone
		}
		if (index >= RetroStates.stateCount(block)) {
			return; // not a state index for this block (a durability value, a stale stack)
		}

		int px = x, py = y, pz = z;
		if (!this.retroapi$clickedReplaceable) {
			switch (side) {
				case 0: py--; break;
				case 1: py++; break;
				case 2: pz--; break;
				case 3: pz++; break;
				case 4: px--; break;
				default: px++; break;
			}
		}
		if (world.getBlockId(px, py, pz) != this.blockId) {
			return; // something else ended up there; don't rewrite someone else's block
		}
		RetroBlockState state = RetroStates.fromIndex(block, index);
		if (state != null) {
			RetroStates.set(world, px, py, pz, state);
		}
	}
}
