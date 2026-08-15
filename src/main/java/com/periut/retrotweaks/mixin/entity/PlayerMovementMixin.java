package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Step assist and fence jumping. From UniTweaks.
 *
 * <p>Step height is set every tick rather than once, so toggling the option takes effect
 * immediately and sneaking still drops it back to vanilla - stepping up a full block while
 * crouching at a ledge is exactly what crouching is meant to prevent.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerMovementMixin extends Entity {

	protected PlayerMovementMixin(World world) {
		super(world);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void retrotweaks$stepAssist(CallbackInfo ci) {
		this.stepHeight = Config.GAMEPLAY.stepAssist && !this.isSneaking() ? 1.0F : 0.5F;
	}

	@Inject(method = "jump", at = @At("TAIL"))
	private void retrotweaks$fenceJump(CallbackInfo ci) {
		// Fences are a block and a half tall; a jump clears exactly 1.5 blocks less a rounding
		// error, so the smallest nudge that closes the gap is all this adds.
		if (Config.GAMEPLAY.fenceJumping) this.velocityY += 0.002F;
	}
}
