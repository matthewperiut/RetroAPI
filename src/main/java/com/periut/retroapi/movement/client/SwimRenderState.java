package com.periut.retroapi.movement.client;

import com.periut.retroapi.movement.api.EntitySwimming;
import net.minecraft.entity.LivingEntity;

/**
 * Modern models read their swim lean off a per-entity render state. b1.7.3's EntityModel is handed
 * nothing but floats, so the entity currently being rendered is parked here for the duration of
 * one LivingEntityRenderer.render call - rendering in beta is single threaded, and the value being
 * cleared afterwards is what keeps the first-person hand (PlayerEntityRenderer.renderHand, which
 * calls setAngles on its own) out of the swim animation.
 */
public final class SwimRenderState {
    private static float swimAmount;

    public static void begin(LivingEntity entity, float tickDelta) {
        swimAmount = ((EntitySwimming) entity).getSwimAmount(tickDelta);
    }

    public static void end() {
        swimAmount = 0.0F;
    }

    /** The swim lean of the entity being rendered, or 0 when nothing is. */
    public static float swimAmount() {
        return swimAmount;
    }

    private SwimRenderState() {
    }
}
