package com.periut.retroapi.mixin.network;

import com.periut.retroapi.tag.RetroBreakTarget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dedicated-server half of the break-target capture that backs
 * {@link com.periut.retroapi.tag.RetroToolTier.Positional}. See {@code BreakTargetClientMixin}; separate
 * from {@code ServerPlayerInteractionManagerMixin} for the same StationAPI reason.
 */
@Mixin(ServerPlayerInteractionManager.class)
public class BreakTargetServerMixin {

	@Shadow private ServerWorld world;
	@Shadow public PlayerEntity player;

	@Unique
	private void retroapi$mark(int x, int y, int z) {
		if (this.player != null && this.world != null) {
			RetroBreakTarget.set(this.player, this.world, x, y, z);
		}
	}

	/** Fired each tick of an in-progress break, which is when breaking speed is asked for. */
	@Inject(method = "continueMining", at = @At("HEAD"))
	private void retroapi$markProgress(int x, int y, int z, CallbackInfo ci) {
		retroapi$mark(x, y, z);
	}

	/** The break itself, which is where harvest (and so drops) is decided. */
	@Inject(method = "tryBreakBlock", at = @At("HEAD"))
	private void retroapi$markBreak(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		retroapi$mark(x, y, z);
	}

	@Inject(method = "finishMining", at = @At("HEAD"))
	private void retroapi$markFinish(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		retroapi$mark(x, y, z);
	}
}
