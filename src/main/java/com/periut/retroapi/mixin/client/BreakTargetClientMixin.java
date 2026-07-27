package com.periut.retroapi.mixin.client;

import com.periut.retroapi.tag.RetroBreakTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records the block the local player is breaking, so
 * {@link com.periut.retroapi.tag.RetroToolTier.Positional} can see its position and state. Beta's harvest
 * hooks are handed only the block TYPE; the interaction manager is where the coordinates still exist.
 *
 * <p>Its own mixin rather than a method on {@code ClientPlayerInteractionManagerMixin}, which targets the
 * same class: that one is disabled under StationAPI, and positional tiers must keep working there.
 */
@Environment(EnvType.CLIENT)
@Mixin(InteractionManager.class)
public class BreakTargetClientMixin {

	@Shadow protected Minecraft minecraft;

	@Unique
	private void retroapi$mark(int x, int y, int z) {
		if (this.minecraft != null && this.minecraft.player != null && this.minecraft.world != null) {
			RetroBreakTarget.set(this.minecraft.player, this.minecraft.world, x, y, z);
		}
	}

	/** Fired every tick the player holds the button down, which is when breaking speed is asked for. */
	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
	private void retroapi$markProgress(int x, int y, int z, int face, CallbackInfo ci) {
		retroapi$mark(x, y, z);
	}

	@Inject(method = "attackBlock", at = @At("HEAD"))
	private void retroapi$markAttack(int x, int y, int z, int face, CallbackInfo ci) {
		retroapi$mark(x, y, z);
	}

	/** The break itself, which is where harvest (and so drops) is decided. */
	@Inject(method = "breakBlock", at = @At("HEAD"))
	private void retroapi$markBreak(int x, int y, int z, int face, CallbackInfoReturnable<Boolean> cir) {
		retroapi$mark(x, y, z);
	}

	@Inject(method = "cancelBlockBreaking", at = @At("HEAD"))
	private void retroapi$clear(CallbackInfo ci) {
		if (this.minecraft != null) {
			RetroBreakTarget.clear(this.minecraft.player);
		}
	}
}
