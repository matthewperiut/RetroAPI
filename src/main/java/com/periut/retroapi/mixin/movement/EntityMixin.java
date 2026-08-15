package com.periut.retroapi.mixin.movement;

import com.periut.retroapi.gamerule.RetroGameRules;
import com.periut.retroapi.movement.api.EntitySprinting;
import com.periut.retroapi.movement.api.EntitySwimming;
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

import static com.periut.retroapi.movement.MovementConstants.SPRINT_FLAG;
import static com.periut.retroapi.movement.MovementConstants.SWIM_AMOUNT_PER_TICK;
import static com.periut.retroapi.movement.MovementConstants.SWIM_FLAG;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntitySprinting, EntitySwimming {
    @Shadow
    protected abstract void setFlag(int index, boolean value);

    @Shadow
    protected abstract boolean getFlag(int index);

    @Shadow
    public World world;

    @Shadow
    @Final
    public Box boundingBox;

    @Shadow
    public double x;

    @Shadow
    public double y;


    @Shadow
    public float standingEyeHeight;

    @Shadow
    public double z;

    @Shadow
    public float width;

    @Shadow
    protected Random random;

    @Shadow
    public double velocityX;

    @Shadow
    public double velocityZ;

    @Shadow
    public Entity vehicle;

    @Shadow
    public abstract boolean isSubmergedInWater();

    @Unique
    private float retroapi$swimAmount;

    @Unique
    private float retroapi$prevSwimAmount;

    /**
     * Sprinting is off unless the world's {@code sprinting} game rule says otherwise. Gating the
     * flag itself - rather than each thing that reads it - means speed, FOV, particles, the model
     * and the network flag all fall back to plain beta behaviour together, with no second switch to
     * keep in agreement.
     */
    @Override
    public void setSprinting(boolean sprinting) {
        setFlag(SPRINT_FLAG, sprinting
            && RetroGameRules.getBoolean(RetroGameRules.SPRINTING)
            && (RetroGameRules.getBoolean(RetroGameRules.SWIMMING) || !retroapi$inWater()));
    }

    /**
     * Water refuses the sprint flag outright when swimming is off.
     *
     * <p>In modern there is no such thing as sprinting through water: entering it while sprinting turns
     * the sprint into a swim. With {@code sprinting} on and {@code swimming} off the sprint flag would
     * otherwise survive into the water and drive half of the swim - the speed and the particles - while
     * the stroke, the pose and the camera stayed behind. Refusing the flag is what makes the two rules
     * independent instead of one being a broken half of the other.
     */
    @Unique
    private boolean retroapi$inWater() {
        return isSubmergedInWater() || isUnderWater();
    }

    @Override
    public boolean isSprinting() {
        return getFlag(SPRINT_FLAG);
    }

    /** Likewise the {@code swimming} rule. */
    @Override
    public void setSwimming(boolean swimming) {
        setFlag(SWIM_FLAG, swimming
            && RetroGameRules.getBoolean(RetroGameRules.SWIMMING));
    }

    @Override
    public boolean isSwimming() {
        return getFlag(SWIM_FLAG);
    }

    @Override
    public boolean isVisuallySwimming() {
        return isSwimming();
    }

    @Override
    public float getSwimAmount(float tickDelta) {
        return retroapi$prevSwimAmount + (retroapi$swimAmount - retroapi$prevSwimAmount) * tickDelta;
    }

    /**
     * Entity.updateSwimming. Starting a swim needs the eyes under water, but once it is going,
     * merely touching water keeps it alive - that is what lets you skim along the surface.
     * Every side derives this from the (already synced) sprint flag, so no new packet is needed
     * for other players to be seen swimming.
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void retroapi$updateSwimming(CallbackInfo ci) {
        if (this.isSwimming()) {
            this.setSwimming(this.isSprinting() && this.isSubmergedInWater() && this.vehicle == null);
        } else {
            this.setSwimming(this.isSprinting() && this.isUnderWater() && this.vehicle == null
                    && this.world.getMaterial(MathHelper.floor(this.x), MathHelper.floor(this.boundingBox.minY), MathHelper.floor(this.z)) == Material.WATER);
        }

        // LivingEntity.updateSwimAmount
        this.retroapi$prevSwimAmount = this.retroapi$swimAmount;
        if (this.isVisuallySwimming()) {
            this.retroapi$swimAmount = Math.min(1.0F, this.retroapi$swimAmount + SWIM_AMOUNT_PER_TICK);
        } else {
            this.retroapi$swimAmount = Math.max(0.0F, this.retroapi$swimAmount - SWIM_AMOUNT_PER_TICK);
        }
    }

    /**
     * Vanilla asks "am I in water?" with {@code boundingBox.expand(0, -0.4, 0)}, which trims 0.4
     * off the top AND the bottom. That is fine for a 1.8 tall box and inverts a 0.6 one: the query
     * box ends up 0.2 tall upside down, and whether it still overlaps a water block then depends
     * on where the feet sit inside the block. A swimmer bobbing across that boundary flickers in
     * and out of "in water", which flickers the swim state, the pose, and the camera with it.
     * While in the swimming pose, ask about the whole box instead - which is what modern does.
     */
    @Inject(method = "checkWaterCollisions", at = @At("HEAD"), cancellable = true)
    private void retroapi$swimWaterCheck(CallbackInfoReturnable<Boolean> cir) {
        if (this.isVisuallySwimming()) {
            cir.setReturnValue(this.world.updateMovementInFluid(
                    this.boundingBox.contract(0.001, 0.001, 0.001), Material.WATER, (Entity) (Object) this));
        }
    }

    /**
     * isTouchingLava trims the box the same way, so it collapses the same way - which would let a
     * player crawling through a one-block gap sit in lava without catching fire.
     */
    @Inject(method = "isTouchingLava", at = @At("HEAD"), cancellable = true)
    private void retroapi$swimLavaCheck(CallbackInfoReturnable<Boolean> cir) {
        if (this.isVisuallySwimming()) {
            cir.setReturnValue(this.world.isMaterialInBox(
                    this.boundingBox.contract(0.1F, 0.001, 0.1F), Material.LAVA));
        }
    }

    @Inject(method = "baseTick", at = @At("HEAD"))
    public void addParticles(CallbackInfo ci) {
        int n4;
        int n3;
        int n2;
        int n;
        // Entity.canSpawnSprintParticle gates on !isInWater(), and isSubmergedInWater() is beta's
        // name for exactly that. The mod's old hand-rolled box test shrank the box by 0.4 on each
        // side, which inverts against the 0.6 swimming box and let sprint dust spawn mid-swim.
        if (this.isSprinting() && !this.isSubmergedInWater() && (n4 = this.world.getBlockId(n3 = MathHelper.floor(this.x), n2 = MathHelper.floor(this.y - (double) 0.2f - (double) this.standingEyeHeight), n = MathHelper.floor(this.z))) > 0) {
            this.world.addParticle("tilecrack_" + n4, this.x + ((double) this.random.nextFloat() - 0.5) * (double) this.width, this.boundingBox.minY + 0.1, this.z + ((double) this.random.nextFloat() - 0.5) * (double) this.width, -this.velocityX * 4.0, 1.5, -this.velocityZ * 4.0);
        }

    }

    /**
     * Entity.isUnderWater. Vanilla's own isInFluid() measures from {@code y + getEyeHeight()},
     * which is the head for a real player but only 0.12 above the feet for OtherPlayerEntity
     * (its eye height is pinned to 0 so network positions land on the feet). Measuring off the
     * top of the bounding box instead lands on the same block vanilla checks for the local
     * player and stays correct for everyone else, including while the swim pose is active.
     */
    @Override
    public boolean isUnderWater() {
        double eyeY = this.boundingBox.maxY - 0.06;
        int i = MathHelper.floor(this.x);
        int j = MathHelper.floor(eyeY);
        int k = MathHelper.floor(this.z);
        if (this.world.getMaterial(i, j, k) != Material.WATER) {
            return false;
        }

        float surface = j + 1 - (LiquidBlock.getFluidHeightFromMeta(this.world.getBlockMeta(i, j, k)) - 0.11111111F);
        return eyeY < surface;
    }

}
