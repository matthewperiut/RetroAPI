package com.periut.retroapi.mixin.movement.client;

import com.periut.retroapi.movement.client.SwimRenderState;
import com.periut.retroapi.movement.util.MathUtil;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HumanoidModel.setupAnim's swimming branch, ported one to one. Modern names map onto beta's as
 * xRot/yRot/zRot -> pitch/yaw/roll, walkAnimationPos -> limbAngle and swimAmount -> the blend
 * that fades the whole thing in as the body tips over.
 */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin extends EntityModel {
    @Shadow
    public ModelPart head;

    @Shadow
    public ModelPart hat;

    @Shadow
    public ModelPart rightArm;

    @Shadow
    public ModelPart leftArm;

    @Shadow
    public ModelPart rightLeg;

    @Shadow
    public ModelPart leftLeg;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void retroapi$swimAnimation(float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float scale, CallbackInfo ci) {
        float swimAmount = SwimRenderState.swimAmount();
        if (swimAmount <= 0.0F) {
            return;
        }

        // The head tips up towards the direction of travel as the body goes flat.
        this.head.pitch = MathUtil.rotLerpRad(swimAmount, this.head.pitch, (float) (-Math.PI / 4));
        this.hat.pitch = this.head.pitch;

        // Beta only ever swings the right arm, so that is the one an attack takes over from the
        // stroke; modern skips whichever arm is mid-swing the same way.
        float rightArmSwimAmount = this.handSwingProgress > 0.0F ? 0.0F : swimAmount;
        float leftArmSwimAmount = swimAmount;

        float swimPos = limbAngle % 26.0F;
        if (swimPos < 14.0F) {
            this.leftArm.pitch = MathUtil.rotLerpRad(leftArmSwimAmount, this.leftArm.pitch, 0.0F);
            this.rightArm.pitch = MathUtil.lerp(rightArmSwimAmount, this.rightArm.pitch, 0.0F);
            this.leftArm.yaw = MathUtil.rotLerpRad(leftArmSwimAmount, this.leftArm.yaw, (float) Math.PI);
            this.rightArm.yaw = MathUtil.lerp(rightArmSwimAmount, this.rightArm.yaw, (float) Math.PI);
            this.leftArm.roll = MathUtil.rotLerpRad(
                    leftArmSwimAmount, this.leftArm.roll,
                    (float) Math.PI + 1.8707964F * this.retroapi$quadraticArmUpdate(swimPos) / this.retroapi$quadraticArmUpdate(14.0F)
            );
            this.rightArm.roll = MathUtil.lerp(
                    rightArmSwimAmount, this.rightArm.roll,
                    (float) Math.PI - 1.8707964F * this.retroapi$quadraticArmUpdate(swimPos) / this.retroapi$quadraticArmUpdate(14.0F)
            );
        } else if (swimPos < 22.0F) {
            float internalSwimPos = (swimPos - 14.0F) / 8.0F;
            this.leftArm.pitch = MathUtil.rotLerpRad(leftArmSwimAmount, this.leftArm.pitch, (float) (Math.PI / 2) * internalSwimPos);
            this.rightArm.pitch = MathUtil.lerp(rightArmSwimAmount, this.rightArm.pitch, (float) (Math.PI / 2) * internalSwimPos);
            this.leftArm.yaw = MathUtil.rotLerpRad(leftArmSwimAmount, this.leftArm.yaw, (float) Math.PI);
            this.rightArm.yaw = MathUtil.lerp(rightArmSwimAmount, this.rightArm.yaw, (float) Math.PI);
            this.leftArm.roll = MathUtil.rotLerpRad(leftArmSwimAmount, this.leftArm.roll, 5.012389F - 1.8707964F * internalSwimPos);
            this.rightArm.roll = MathUtil.lerp(rightArmSwimAmount, this.rightArm.roll, 1.2707963F + 1.8707964F * internalSwimPos);
        } else if (swimPos < 26.0F) {
            float internalSwimPos = (swimPos - 22.0F) / 4.0F;
            this.leftArm.pitch = MathUtil.rotLerpRad(leftArmSwimAmount, this.leftArm.pitch, (float) (Math.PI / 2) - (float) (Math.PI / 2) * internalSwimPos);
            this.rightArm.pitch = MathUtil.lerp(rightArmSwimAmount, this.rightArm.pitch, (float) (Math.PI / 2) - (float) (Math.PI / 2) * internalSwimPos);
            this.leftArm.yaw = MathUtil.rotLerpRad(leftArmSwimAmount, this.leftArm.yaw, (float) Math.PI);
            this.rightArm.yaw = MathUtil.lerp(rightArmSwimAmount, this.rightArm.yaw, (float) Math.PI);
            this.leftArm.roll = MathUtil.rotLerpRad(leftArmSwimAmount, this.leftArm.roll, (float) Math.PI);
            this.rightArm.roll = MathUtil.lerp(rightArmSwimAmount, this.rightArm.roll, (float) Math.PI);
        }

        // Legs flutter-kick, out of phase with each other and slower than the arm stroke.
        this.leftLeg.pitch = MathUtil.lerp(swimAmount, this.leftLeg.pitch, 0.3F * MathHelper.cos(limbAngle * 0.33333334F + (float) Math.PI));
        this.rightLeg.pitch = MathUtil.lerp(swimAmount, this.rightLeg.pitch, 0.3F * MathHelper.cos(limbAngle * 0.33333334F));
    }

    @Unique
    private float retroapi$quadraticArmUpdate(float x) {
        return -65.0F * x + x * x;
    }
}
