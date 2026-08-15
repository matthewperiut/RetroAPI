package com.periut.retroapi.mixin.movement.client;

import com.periut.retroapi.movement.api.EntitySprinting;
import com.periut.retroapi.movement.api.EntitySwimming;
import net.minecraft.client.input.Input;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.periut.retroapi.movement.MovementConstants.FORWARD_IMPULSE;
import static com.periut.retroapi.movement.MovementConstants.SPRINT_TRIGGER_TIME;
import static com.periut.retroapi.movement.MovementConstants.WATER_SINK_NUDGE;

/**
 * LocalPlayer.aiStep's sprint block, which in modern Minecraft is also the swim block: a swim is
 * just a sprint that happened to start with the head under water.
 */
@Mixin(ClientPlayerEntity.class)
abstract public class ClientPlayerEntityMixin extends PlayerEntity {
    @Shadow
    public Input input;

    @Unique
    private int retroapi$sprintTriggerTime;

    /** Previous tick's values - what modern reads before it ticks the input. */
    @Unique
    private boolean retroapi$hadForwardImpulse;

    @Unique
    private boolean retroapi$wasSneaking;

    @Inject(
            method = "tickMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/input/Input;update(Lnet/minecraft/entity/player/PlayerEntity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void retroapi$tickSprintingAndSwimming(CallbackInfo ci) {
        if (this.retroapi$sprintTriggerTime > 0) {
            this.retroapi$sprintTriggerTime--;
        }

        boolean hasForwardImpulse = this.input.movementForward > FORWARD_IMPULSE;

        if (this.retroapi$wasSneaking || this.input.movementForward < 0.0F) {
            this.retroapi$sprintTriggerTime = 0;
        }

        if (this.retroapi$canStartSprinting(hasForwardImpulse)) {
            // Double tap: the window opens on the tap that starts the impulse, and a second tap
            // inside it starts the sprint.
            if (!this.retroapi$hadForwardImpulse) {
                if (this.retroapi$sprintTriggerTime > 0) {
                    this.retroapi$setSprinting(true);
                } else {
                    this.retroapi$sprintTriggerTime = SPRINT_TRIGGER_TIME;
                }
            }

            if (this.input.unused) {
                this.retroapi$setSprinting(true);
            }
        }

        if (this.retroapi$isSprinting()) {
            if (this.retroapi$isSwimming()) {
                if (this.retroapi$shouldStopSwimSprinting(hasForwardImpulse)) {
                    this.retroapi$setSprinting(false);
                }
            } else if (this.retroapi$shouldStopRunSprinting(hasForwardImpulse)) {
                this.retroapi$setSprinting(false);
            }
        }

        // LivingEntity.goDownInWater - sneak to sink, the counterpart to jumping to rise.
        if (this.isSubmergedInWater() && this.input.sneaking) {
            this.velocityY -= WATER_SINK_NUDGE;
        }

        this.retroapi$hadForwardImpulse = hasForwardImpulse;
        this.retroapi$wasSneaking = this.input.sneaking;
    }

    /**
     * LocalPlayer.aiStep sets crouching to false whenever the player is swimming: shift is the
     * dive control there, not a crouch. Without this the sneak pose would fight the swim pose on
     * the model and the crouch offset would be sent to everyone else.
     */
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void retroapi$noCrouchWhileSwimming(CallbackInfoReturnable<Boolean> cir) {
        if (((EntitySwimming) this).isVisuallySwimming()) {
            cir.setReturnValue(false);
        }
    }

    /**
     * LocalPlayer.suffocatesAt, which is what pushOutOfBlock's probes should have been. Every probe
     * in that method routes through this one helper, so correcting it here covers all of them.
     *
     * <p>Beta asks whether the block at the sample point OR THE ONE ABOVE IT suffocates - a player
     * is two blocks tall, so that is a fair shorthand. It stops being one at 0.6 tall: swim into a
     * one-block gap and the block above is the ceiling, so every probe reports suffocation and the
     * method shoves the player back out at 0.1/tick. That is the wall you hit swimming into a 1x1
     * hole. Modern tests a column bounded by the player's own {@code boundingBox.minY..maxY}
     * instead, so a ceiling the body does not reach is simply not part of the question; do the
     * same here. For a 1.8 tall pose both probes still overlap the box, so standing behaviour is
     * bit-for-bit what it was.
     */
    @Redirect(
            method = "shouldSuffocate",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;shouldSuffocate(III)Z")
    )
    private boolean retroapi$suffocatesWithinPose(World world, int x, int y, int z) {
        if (y + 1 <= this.boundingBox.minY || y >= this.boundingBox.maxY) {
            return false;
        }

        return world.shouldSuffocate(x, y, z);
    }

    @Unique
    private boolean retroapi$canStartSprinting(boolean hasForwardImpulse) {
        return !this.retroapi$isSprinting()
                && hasForwardImpulse
                && this.retroapi$isSprintingPossible(false)
                && (!this.retroapi$isMovingSlowly() || this.retroapi$isUnderWater());
    }

    /** LocalPlayer.shouldStopRunSprinting. Beta has no minor-collision concept, so any wall stops it. */
    @Unique
    private boolean retroapi$shouldStopRunSprinting(boolean hasForwardImpulse) {
        return !this.retroapi$isSprintingPossible(false) || !hasForwardImpulse || this.horizontalCollision;
    }

    /**
     * LocalPlayer.shouldStopSwimSprinting. Letting go of forward does not end a swim on its own -
     * it carries on while you sink or push off the bottom, which is what makes diving work.
     */
    @Unique
    private boolean retroapi$shouldStopSwimSprinting(boolean hasForwardImpulse) {
        return !this.isSubmergedInWater() || !hasForwardImpulse && !this.onGround && !this.input.sneaking;
    }

    /**
     * LocalPlayer.isSprintingPossible. The food and blindness halves have no beta equivalent, so
     * only the shallow-water rule survives: you cannot break into a sprint while wading.
     */
    @Unique
    private boolean retroapi$isSprintingPossible(boolean allowedInShallowWater) {
        return allowedInShallowWater || !this.retroapi$isInShallowWater();
    }

    @Unique
    private boolean retroapi$isInShallowWater() {
        return this.isSubmergedInWater() && !this.retroapi$isUnderWater();
    }

    /** LivingEntity.isMovingSlowly - crouching, or crawling through a gap too low to stand in. */
    @Unique
    private boolean retroapi$isMovingSlowly() {
        return this.input.sneaking
                || ((EntitySwimming) this).isVisuallySwimming() && !this.isSubmergedInWater();
    }

    @Unique
    private boolean retroapi$isSprinting() {
        return ((EntitySprinting) this).isSprinting();
    }

    @Unique
    private void retroapi$setSprinting(boolean sprinting) {
        ((EntitySprinting) this).setSprinting(sprinting);
    }

    @Unique
    private boolean retroapi$isSwimming() {
        return ((EntitySwimming) this).isSwimming();
    }

    @Unique
    private boolean retroapi$isUnderWater() {
        return ((EntitySwimming) this).isUnderWater();
    }

    // shove this down here because it is useless and just required by jvm
    public ClientPlayerEntityMixin(World world) {
        super(world);
    }
}
