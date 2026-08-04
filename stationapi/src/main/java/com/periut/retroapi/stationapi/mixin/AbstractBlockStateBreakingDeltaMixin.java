package com.periut.retroapi.stationapi.mixin;

import com.periut.retroapi.register.block.RetroDisguises;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.modificationstation.stationapi.api.block.AbstractBlockState;
import net.modificationstation.stationapi.api.block.StationFlatteningBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * How fast a disguised block gives way, through StationAPI's own hook for it.
 *
 * <p>RetroAPI's native version redirects {@code Block.getHardness(PlayerEntity)} inside the interaction
 * manager, and StationAPI's flattening module redirects that same call first, to
 * {@code world.getBlockState(x, y, z).calcBlockBreakingDelta(player, world, pos)}. So under StationAPI
 * there is nothing left at the vanilla call site to bind to, and the native hook is disabled.
 *
 * <p>That redirect is not an obstacle, it is the supported extension point: breaking speed there is a
 * property of the block STATE, asked with the position in hand, which is exactly what a per-position
 * disguise needs and more than beta's own hook offers. Answering here rather than at the vanilla call
 * also means anything else routing through StationAPI's block states gets the same answer.
 *
 * <p>The delta folds together the block's hardness and how well the held tool bites it, so handing the
 * question to the worn block's state gets both: a frame clad in stone takes as long as stone and rewards
 * a pickaxe for it.
 */
@Mixin(AbstractBlockState.class)
public class AbstractBlockStateBreakingDeltaMixin {

	@Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true)
	private void retroapi$disguisedBreakingDelta(PlayerEntity player, BlockView world, BlockPos pos,
			CallbackInfoReturnable<Float> cir) {
		if (world == null || pos == null) {
			return;
		}
		Block worn = RetroDisguises.at(world, pos.x, pos.y, pos.z);
		if (worn == null) {
			return;
		}
		// The worn block's own state answers the same question. Reached through StationFlatteningBlock,
		// which is where flattening puts getDefaultState and which every Block implements there. It
		// cannot recurse: a block that wears another is never itself wearable.
		cir.setReturnValue(((StationFlatteningBlock) worn).getDefaultState()
			.calcBlockBreakingDelta(player, world, pos));
	}
}
