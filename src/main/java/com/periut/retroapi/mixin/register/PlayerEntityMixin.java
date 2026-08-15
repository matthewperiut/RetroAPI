package com.periut.retroapi.mixin.register;

import com.periut.retroapi.tag.RetroMining;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Beta's two mining questions, answered by {@link RetroMining}. StationAPI asks the same two somewhere
 * else entirely and its module answers them from the same class, so a block mines identically either way.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

	@Unique
	private PlayerEntity retroapi$self() {
		return (PlayerEntity) (Object) this;
	}

	@Inject(method = "canHarvest", at = @At("HEAD"), cancellable = true)
	private void retroapi$canHarvest(Block block, CallbackInfoReturnable<Boolean> cir) {
		Boolean harvests = RetroMining.canHarvest(retroapi$self(), block);
		if (harvests != null) {
			cir.setReturnValue(harvests);
		}
	}

	@Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
	private void retroapi$effectiveTool(Block block, CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(RetroMining.breakingSpeed(retroapi$self(), block, cir.getReturnValue()));
	}
}
