package com.periut.retroapi.stationapi.mixin;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.modificationstation.stationapi.api.block.AbstractBlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Creative breaks everything in one tick here too, through StationAPI's own hook for breaking speed.
 *
 * <p>RetroAPI's native answer injects into {@code Block.getHardness(PlayerEntity)}. Under StationAPI
 * that method is still there and still injected into - it is simply never CALLED: the flattening module
 * redirects every call site to {@code BlockState.calcBlockBreakingDelta(player, world, pos)} instead. An
 * injector that applies cleanly and then never runs is the quietest way for a feature to disappear, and
 * it is why creative went back to chipping away at stone with StationAPI installed.
 *
 * <p>So the same rule is stated again at the hook that does get called. Both halves say what modern says
 * - a player with infinite materials is all the way through, whatever the block - so the two agree no
 * matter which one a given setup ends up asking.
 */
@Mixin(AbstractBlockState.class)
public class CreativeBreakingDeltaMixin {

	@Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true)
	private void retroapi$creativeBreaksInstantly(PlayerEntity player, BlockView world, BlockPos pos,
			CallbackInfoReturnable<Float> cir) {
		if (player != null && RetroGameModes.get(player) == RetroGameMode.CREATIVE) {
			cir.setReturnValue(1.0F);
		}
	}
}
