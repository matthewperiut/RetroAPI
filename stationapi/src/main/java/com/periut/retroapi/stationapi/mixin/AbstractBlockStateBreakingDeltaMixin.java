package com.periut.retroapi.stationapi.mixin;

import com.periut.retroapi.register.block.RetroDisguises;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.modificationstation.stationapi.api.block.AbstractBlockState;
import net.modificationstation.stationapi.api.block.BlockState;
import net.modificationstation.stationapi.api.entity.player.StationFlatteningPlayerEntity;

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
 * <p>Same split as {@link RetroDisguises#breakingDelta}, which is the no-StationAPI half of this: the worn
 * block decides the hardness, the frame is asked everything else. Asked through StationAPI's own
 * position-aware calls, so its tool events still fire and RetroAPI's rules reach them the usual way.
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
		float hardness = worn.getHardness();
		if (hardness < 0.0F) {
			cir.setReturnValue(0.0F);
			return;
		}
		BlockState frame = (BlockState) (Object) this;
		StationFlatteningPlayerEntity miner = (StationFlatteningPlayerEntity) player;
		cir.setReturnValue(miner.canHarvest(world, pos, frame)
			? miner.getBlockBreakingSpeed(world, pos, frame) / hardness / 30.0F
			: 1.0F / hardness / 100.0F);
	}
}
