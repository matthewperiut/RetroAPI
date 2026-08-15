package com.periut.retroapi.mixin.movement.client;

import com.periut.retroapi.movement.api.EntitySwimming;
import com.periut.retroapi.movement.client.SwimRenderState;
import com.periut.retroapi.movement.util.MathUtil;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AvatarRenderer.setupRotations' swimming branch. b1.7.3's applyHandSwingRotation sits at exactly
 * the same point in the matrix stack - after the translate to the entity, before the -1/-1/1
 * flip - so the rotation and offset carry over unchanged.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At("HEAD"))
    private void retroapi$beginRender(LivingEntity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        SwimRenderState.begin(entity, tickDelta);
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;DDDFF)V", at = @At("TAIL"))
    private void retroapi$endRender(LivingEntity entity, double x, double y, double z, float yaw, float tickDelta, CallbackInfo ci) {
        SwimRenderState.end();
    }

    @Inject(method = "applyHandSwingRotation(Lnet/minecraft/entity/LivingEntity;FFF)V", at = @At("TAIL"))
    private void retroapi$applySwimRotation(LivingEntity entity, float ageInTicks, float bodyYaw, float tickDelta, CallbackInfo ci) {
        float swimAmount = ((EntitySwimming) entity).getSwimAmount(tickDelta);
        if (swimAmount <= 0.0F) {
            return;
        }

        float pitch = entity.prevPitch + (entity.pitch - entity.prevPitch) * tickDelta;
        // Out of water the body only ever reaches flat - that is the crawl, where the head is
        // not aimed along the body the way it is when swimming.
        float target = entity.isSubmergedInWater() ? -90.0F - pitch : -90.0F;
        GL11.glRotatef(MathUtil.lerp(swimAmount, 0.0F, target), 1.0F, 0.0F, 0.0F);
        if (((EntitySwimming) entity).isVisuallySwimming()) {
            GL11.glTranslatef(0.0F, -1.0F, 0.3F);
        }
    }
}
