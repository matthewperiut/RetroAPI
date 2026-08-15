package com.periut.retroapi.mixin.movement;

import com.periut.retroapi.movement.api.EntitySprinting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.periut.retroapi.movement.MovementConstants.BASE_SWIM_SPEED;
import static com.periut.retroapi.movement.MovementConstants.SPRINT_WATER_SLOW_DOWN;
import static com.periut.retroapi.movement.MovementConstants.WATER_VERTICAL_SLOW_DOWN;

@Mixin(LivingEntity.class)
abstract public class LivingEntityMixin extends Entity {

    @Shadow
    public float walkAnimationSpeed;

    @Shadow
    public float lastWalkAnimationSpeed;

    @Shadow
    public float walkAnimationProgress;

    public LivingEntityMixin(World world) {
        super(world);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void increaseMovementSpeed(CallbackInfo info) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // In water the sprint boost comes out of the reduced drag in retroapi$travelInWater
        // instead, the way modern Minecraft does it - stacking this on top would overshoot.
        if (((EntitySprinting) (Object) entity).isSprinting() && !this.isSubmergedInWater()) {

            if (Math.sqrt(Math.pow(velocityX, 2) + Math.pow(velocityZ, 2)) < 0.25) {
                float f = yaw * ((float) Math.PI / 180);
                this.velocityX -= (double) (MathHelper.sin(f) * 0.035f);
                this.velocityZ += (double) (MathHelper.cos(f) * 0.035f);
            }
        }
    }

    /**
     * LivingEntity.travelInWater, for the sprinting case only. Modern differs from b1.7.3 in
     * exactly two places while sprinting: horizontal drag relaxes from 0.8 to 0.9, and the
     * gravity term is skipped entirely (getFluidFallingAdjustedMovement returns early when
     * sprinting), which is why a sprint-swimmer holds their depth instead of sinking.
     * Everything else - the 0.02 base speed, the 0.8 vertical drag, the ledge hop - is the same
     * in both versions, so ordinary swimming keeps vanilla b1.7.3 behaviour untouched.
     */
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void retroapi$travelInWater(float sidewaysSpeed, float forwardSpeed, CallbackInfo ci) {
        if (!this.isSubmergedInWater() || !((EntitySprinting) (Object) this).isSprinting()) {
            return;
        }

        double oldY = this.y;
        this.moveNonSolid(sidewaysSpeed, forwardSpeed, BASE_SWIM_SPEED);
        this.move(this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= SPRINT_WATER_SLOW_DOWN;
        this.velocityY *= WATER_VERTICAL_SLOW_DOWN;
        this.velocityZ *= SPRINT_WATER_SLOW_DOWN;
        if (this.horizontalCollision && this.getEntitiesInside(this.velocityX, this.velocityY + 0.6F - this.y + oldY, this.velocityZ)) {
            this.velocityY = 0.3F;
        }

        this.retroapi$updateWalkAnimation();
        ci.cancel();
    }

    /** The tail of LivingEntity.travel, which the cancelled call would otherwise have reached. */
    @Unique
    private void retroapi$updateWalkAnimation() {
        this.lastWalkAnimationSpeed = this.walkAnimationSpeed;
        double dx = this.x - this.prevX;
        double dz = this.z - this.prevZ;
        float speed = MathHelper.sqrt(dx * dx + dz * dz) * 4.0F;
        if (speed > 1.0F) {
            speed = 1.0F;
        }

        this.walkAnimationSpeed = this.walkAnimationSpeed + (speed - this.walkAnimationSpeed) * 0.4F;
        this.walkAnimationProgress = this.walkAnimationProgress + this.walkAnimationSpeed;
    }

    @Inject(method = "jump", at = @At("TAIL"), cancellable = true)
    void addedSpeedOnSprintJump(CallbackInfo ci) {
        if (((EntitySprinting) (Object) this).isSprinting()) {
            float f = yaw * ((float) Math.PI / 180);
            this.velocityX -= (double) (MathHelper.sin(f) * 0.2f);
            this.velocityZ += (double) (MathHelper.cos(f) * 0.2f);
        }
        velocityModified = true;
    }
}
