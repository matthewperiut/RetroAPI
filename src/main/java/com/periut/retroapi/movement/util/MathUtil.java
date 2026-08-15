package com.periut.retroapi.movement.util;

/**
 * The handful of net.minecraft.util.Mth helpers the swim animation needs. b1.7.3's MathHelper
 * predates all of them, and the animation is a direct port, so they are copied verbatim.
 */
public final class MathUtil {
    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    /** Mth.rotLerpRad - lerps along the shortest way round the circle. */
    public static float rotLerpRad(float delta, float from, float to) {
        float diff = to - from;

        while (diff < -(float) Math.PI) {
            diff += (float) (Math.PI * 2);
        }

        while (diff >= (float) Math.PI) {
            diff -= (float) (Math.PI * 2);
        }

        return from + delta * diff;
    }

    private MathUtil() {
    }
}
