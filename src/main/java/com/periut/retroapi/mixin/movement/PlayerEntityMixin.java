package com.periut.retroapi.mixin.movement;

import com.periut.retroapi.movement.api.EntitySprinting;
import com.periut.retroapi.movement.api.EntitySwimming;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.periut.retroapi.movement.MovementConstants.STANDING_HEIGHT;
import static com.periut.retroapi.movement.MovementConstants.STANDING_WIDTH;
import static com.periut.retroapi.movement.MovementConstants.SWIMMING_EYE_HEIGHT;
import static com.periut.retroapi.movement.MovementConstants.SWIMMING_HEIGHT;
import static com.periut.retroapi.movement.MovementConstants.SWIMMING_WIDTH;
import static com.periut.retroapi.movement.MovementConstants.SWIM_PITCH_PULL;
import static com.periut.retroapi.movement.MovementConstants.SWIM_PITCH_PULL_STEEP;

@Mixin(PlayerEntity.class)
abstract public class PlayerEntityMixin extends LivingEntity implements EntitySwimming {

    // shove this down here because it is useless and just required by jvm
    public PlayerEntityMixin(World world) {
        super(world);
    }

    @Shadow
    public float stepBobbingAmount;

    @Shadow
    public float prevStepBobbingAmount;

    /** True while the 0.6 x 0.6 swimming pose is applied, i.e. Pose.SWIMMING in modern terms. */
    @Unique
    private boolean retroapi$swimPose;

    /**
     * The eye height this player stands with. Always 1.62 for real players, but OtherPlayerEntity
     * pins it to 0 so that network positions land on the feet, and that has to survive a swim.
     */
    @Unique
    private float retroapi$standingEyeHeight = 1.62F;

    @Override
    public boolean isVisuallySwimming() {
        return this.retroapi$swimPose;
    }

    @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    public void onAttack(Entity target, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Check if the player is sprinting
        if (((EntitySprinting) (Object) player).isSprinting()) {
            // Apply sprinting effect: Increase knockback
            applyKnockback(player, target);

            // Optionally: Stop sprinting after the attack
            ((EntitySprinting) (Object) player).setSprinting(false);
        }
    }

    @Unique
    private void applyKnockback(PlayerEntity player, Entity target) {
        // Basic knockback calculation based on sprinting
        float knockbackStrength = 1.2F; // You can adjust this value for more/less knockback
        double xKnockback = -MathHelper.sin(player.yaw * ((float) Math.PI / 180F)) * knockbackStrength;
        double zKnockback = MathHelper.cos(player.yaw * ((float) Math.PI / 180F)) * knockbackStrength;

        // Apply knockback to the target entity
        target.addVelocity(xKnockback, 0.1, zKnockback);

        // Reduce player's velocity slightly (to simulate loss of momentum after sprinting attack)
        player.velocityX *= 0.6;
        player.velocityZ *= 0.6;
    }

    /**
     * Player.travel: the look direction pulls a swimmer up or down, which is what makes a swim
     * feel steered rather than skimmed. The upward half is held back until the head has water
     * above it so surfacing does not fling the player out of the pool.
     */
    @Inject(method = "travel", at = @At("HEAD"))
    private void retroapi$swimSteering(float sidewaysSpeed, float forwardSpeed, CallbackInfo ci) {
        if (!this.isSwimming()) {
            return;
        }

        double lookY = -MathHelper.sin(this.pitch * ((float) Math.PI / 180.0F));
        double pull = lookY < -0.2 ? SWIM_PITCH_PULL_STEEP : SWIM_PITCH_PULL;
        if (lookY <= 0.0 || this.jumping || this.retroapi$isWaterAbove()) {
            this.velocityY += (lookY - this.velocityY) * pull;
        }
    }

    @Unique
    private boolean retroapi$isWaterAbove() {
        int i = MathHelper.floor(this.x);
        int j = MathHelper.floor(this.boundingBox.minY + 1.0 - 0.1);
        int k = MathHelper.floor(this.z);
        Material material = this.world.getMaterial(i, j, k);
        return material == Material.WATER || material == Material.LAVA;
    }

    /**
     * AbstractClientPlayer.updateBob gates the walk bob on {@code !isSwimming()} in modern - a
     * swimmer hugging the seabed counts as onGround, and bobbing along it like a footstep looks
     * wrong. Beta computes the bob at the end of tickMovement from the same onGround test, so
     * redo the step here with a target of zero: prevStepBobbingAmount is the pre-tick value, and
     * vanilla's update is {@code bob += (target - bob) * 0.4}.
     */
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void retroapi$noBobWhileSwimming(CallbackInfo ci) {
        if (this.isSwimming()) {
            this.stepBobbingAmount = this.prevStepBobbingAmount * 0.6F;
        }
    }

    /**
     * Player.updatePlayerPose, minus the crouching pose beta does not have. The swim pose is
     * anchored at the feet so shrinking to 0.6 drops the camera rather than lifting the body,
     * and a player who cannot stand back up stays horizontal - modern's crawling.
     */
    @Inject(method = "tick", at = @At("TAIL"))
    private void retroapi$updatePlayerPose(CallbackInfo ci) {
        // Dying and sleeping resize the box themselves (0.2 cubes), so leave those alone.
        if (this.health <= 0 || ((PlayerEntity) (Object) this).isSleeping()) {
            return;
        }

        if (!this.retroapi$fitsWhileSwimming()) {
            return;
        }

        boolean swimPose = this.isSwimming();
        if (!swimPose && !this.retroapi$fitsWhileStanding()) {
            swimPose = true;
        }

        if (swimPose != this.retroapi$swimPose) {
            this.retroapi$setSwimPose(swimPose);
        }

        // Beta dips the camera by up to 0.2 while the sneak key is held. Sneak is the dive control
        // while swimming, and modern has no such dip, so keep the swimming eye where it belongs.
        if (this.retroapi$swimPose && this.cameraOffset != 0.0F) {
            this.cameraOffset = 0.0F;
            this.y = this.boundingBox.minY + this.standingEyeHeight;
        }
    }

    @Unique
    private void retroapi$setSwimPose(boolean swimPose) {
        if (swimPose) {
            this.retroapi$standingEyeHeight = this.standingEyeHeight;
        }

        this.retroapi$swimPose = swimPose;

        float width = swimPose ? SWIMMING_WIDTH : STANDING_WIDTH;
        float height = swimPose ? SWIMMING_HEIGHT : STANDING_HEIGHT;
        // OtherPlayerEntity stands with an eye height of 0; scaling to 0.4 would push its
        // bounding box below its feet, so a player without an eye height keeps it.
        float eyeHeight = swimPose && this.retroapi$standingEyeHeight > 0.0F
                ? SWIMMING_EYE_HEIGHT
                : this.retroapi$standingEyeHeight;

        double feetY = this.boundingBox.minY;
        this.setBoundingBoxSpacing(width, height);
        this.standingEyeHeight = eyeHeight;
        this.boundingBox.set(this.x - width / 2.0F, feetY, this.z - width / 2.0F,
                this.x + width / 2.0F, feetY + height, this.z + width / 2.0F);
        // Entity.move keeps y in step with the box this way; do the same so the eye does not drift.
        this.y = this.boundingBox.minY + this.standingEyeHeight - this.cameraOffset;
    }

    @Unique
    private boolean retroapi$fitsWhileSwimming() {
        return this.retroapi$fits(SWIMMING_WIDTH, SWIMMING_HEIGHT);
    }

    @Unique
    private boolean retroapi$fitsWhileStanding() {
        return this.retroapi$fits(STANDING_WIDTH, STANDING_HEIGHT);
    }

    /** Player.canPlayerFitWithinBlocksAndEntitiesWhen, for a pose of the given size. */
    @Unique
    private boolean retroapi$fits(float width, float height) {
        double feetY = this.boundingBox.minY;
        double half = width / 2.0F;
        Box box = Box.createCached(this.x - half, feetY, this.z - half,
                this.x + half, feetY + height, this.z + half).contract(1.0E-7, 1.0E-7, 1.0E-7);
        return this.world.getEntityCollisions((PlayerEntity) (Object) this, box).isEmpty();
    }
}
