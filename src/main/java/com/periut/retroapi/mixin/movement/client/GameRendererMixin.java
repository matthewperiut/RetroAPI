package com.periut.retroapi.mixin.movement.client;

import com.periut.retroapi.movement.api.EntitySwimming;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.periut.retroapi.movement.RetroMovement.lastMovementFovMultiplier;
import static com.periut.retroapi.movement.RetroMovement.movementFovMultiplier;

@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererMixin {
    @Shadow
    protected abstract float getFov(float f);

    @Shadow
    private Minecraft client;

    @Shadow
    private float viewDistance;


    /**
     * Beta puts the camera at {@code y - (standingEyeHeight - 1.62)} so that entities with a
     * different eye-height convention (OtherPlayerEntity pins it to 0) still frame at 1.62 above
     * the feet. That normalisation would exactly cancel the swimming pose - eye drops 1.22, camera
     * rises 1.22 - and leave first person looking like nothing happened. Reporting the standing
     * height turns the correction into a no-op, which puts the camera on the swimming eye at 0.4
     * above the feet, where modern Minecraft has it.
     */
    @Redirect(
            method = "applyCameraTransform",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;standingEyeHeight:F", opcode = Opcodes.GETFIELD)
    )
    private float retroapi$swimCameraEyeHeight(LivingEntity camera) {
        return ((EntitySwimming) camera).isVisuallySwimming() ? 1.62F : camera.standingEyeHeight;
    }

    @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;getFov(F)F"), require = 0)
    public float redirectToCustomFov(GameRenderer instance, float value) {
        return getFovMultiplier(value, false);
    }

    @Unique
    public float getFovMultiplier(float f, boolean isHand) {
        LivingEntity entity = this.client.camera;
        float fov = 70F;

        if (isHand) {
            fov = 70F;
        }

        if (entity.isInFluid(Material.WATER)) {
            fov *= 60.0F / 70.0F;
        }

        if (entity.health <= 0) {
            float deathTimeFov = (float) entity.deathTime + f;
            fov /= (1.0F - 500F / (deathTimeFov + 500F)) * 2.0F + 1.0F;
        }

        if (!isHand) {
            fov *= lastMovementFovMultiplier + (movementFovMultiplier - lastMovementFovMultiplier) * f;
        }

        return fov;
    }

    @Inject(method = "renderFirstPersonHand", at = @At(value = "HEAD"))
    public void adjustHandFov(float f, int i, CallbackInfo ci) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GLU.gluPerspective(getFovMultiplier(f, true), (float) client.displayWidth / (float) client.displayHeight, 0.05F, viewDistance * 2.0F);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }
}
