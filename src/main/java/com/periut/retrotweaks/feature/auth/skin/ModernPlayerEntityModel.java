package com.periut.retrotweaks.feature.auth.skin;

import com.periut.retrotweaks.feature.auth.skin.data.ModelPartData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;

public class ModernPlayerEntityModel extends BipedEntityModel {
    public final ModelPart leftSleeve;
    public final ModelPart rightSleeve;
    public final ModelPart leftPantLeg;
    public final ModelPart rightPantLeg;
    public final ModelPart jacket;

    public ModernPlayerEntityModel(float scale, boolean thinArms) {
        super(scale);
        // Pivot y grows downwards, so the RAISED shoulder is the smaller number. 2.5 is where a
        // slim model's arms hang by default; 2.0 lifts them, which is what the option's name says.
        float slimShoulderHeight = com.periut.retrotweaks.config.Config.MULTIPLAYER.raiseSlimSkinShoulders ? 2.0F : 2.5F;

        if (thinArms) {
            this.leftArm = this.createModelPart(32, 48);
            this.leftArm.addCuboid(-1.0F, -2.0F, -2.0F, 3, 12, 4, scale);
            this.leftArm.setPivot(5.0F, slimShoulderHeight, 0.0F);
            this.rightArm = this.createModelPart(40, 16);
            this.rightArm.addCuboid(-2.0F, -2.0F, -2.0F, 3, 12, 4, scale);
            this.rightArm.setPivot(-5.0F, slimShoulderHeight, 0.0F);
            this.leftSleeve = this.createModelPart(48, 48);
            this.leftSleeve.addCuboid(-1.0F, -2.0F, -2.0F, 3, 12, 4, scale + 0.25F);
            this.leftSleeve.setPivot(5.0F, slimShoulderHeight, 0.0F);
            this.rightSleeve = this.createModelPart(40, 32);
            this.rightSleeve.addCuboid(-2.0F, -2.0F, -2.0F, 3, 12, 4, scale + 0.25F);
            this.rightSleeve.setPivot(-5.0F, slimShoulderHeight, 10.0F);
        } else {
            this.leftArm = this.createModelPart(32, 48);
            this.leftArm.addCuboid(-1.0F, -2.0F, -2.0F, 4, 12, 4, scale);
            this.leftArm.setPivot(5.0F, 2.0F, 0.0F);
            this.leftSleeve = this.createModelPart(48, 48);
            this.leftSleeve.addCuboid(-1.0F, -2.0F, -2.0F, 4, 12, 4, scale + 0.25F);
            this.leftSleeve.setPivot(5.0F, 2.0F, 0.0F);
            this.rightSleeve = this.createModelPart(40, 32);
            this.rightSleeve.addCuboid(-3.0F, -2.0F, -2.0F, 4, 12, 4, scale + 0.25F);
            this.rightSleeve.setPivot(-5.0F, 2.0F, 10.0F);
        }

        this.leftLeg = this.createModelPart(16, 48);
        this.leftLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
        this.leftLeg.setPivot(2.0F, 12.0F, 0.0F);
        this.leftPantLeg = this.createModelPart(0, 48);
        this.leftPantLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale + 0.25F);
        this.leftPantLeg.setPivot(1.9F, 12.0F, 0.0F);
        this.rightPantLeg = this.createModelPart(0, 32);
        this.rightPantLeg.addCuboid(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale + 0.25F);
        this.rightPantLeg.setPivot(-1.9F, 12.0F, 0.0F);
        this.jacket = this.createModelPart(16, 32);
        this.jacket.addCuboid(-4.0F, 0.0F, -2.0F, 8, 12, 4, scale + 0.25F);
        this.jacket.setPivot(0.0F, 0.0F, 0.0F);
    }

    private ModelPart createModelPart(int x, int y) {
        ModelPart modelRenderer = new ModelPart(x, y);
        ((ModelPartData) modelRenderer).setTextureHeight(64);
        return modelRenderer;
    }

    @Override
    public void render(float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float scale) {
        super.render(limbAngle, limbDistance, animationProgress, headYaw, headPitch, scale);

        this.leftPantLeg.render(scale);
        this.rightPantLeg.render(scale);
        this.leftSleeve.render(scale);
        this.rightSleeve.render(scale);
        this.jacket.render(scale);
    }

    public void copyPositionAndRotation(ModelPart from, ModelPart to) {
        to.setPivot(from.pivotX, from.pivotY, from.pivotZ);
        to.yaw = from.yaw;
        to.pitch = from.pitch;
        to.roll = from.roll;
    }

    @Override
    public void setAngles(float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float scale) {
        super.setAngles(limbAngle, limbDistance, animationProgress, headYaw, headPitch, scale);
        this.copyPositionAndRotation(this.leftLeg, this.leftPantLeg);
        this.copyPositionAndRotation(this.rightLeg, this.rightPantLeg);
        this.copyPositionAndRotation(this.leftArm, this.leftSleeve);
        this.copyPositionAndRotation(this.rightArm, this.rightSleeve);
        this.copyPositionAndRotation(this.body, this.jacket);
        this.copyPositionAndRotation(this.head, this.hat);
    }
}
