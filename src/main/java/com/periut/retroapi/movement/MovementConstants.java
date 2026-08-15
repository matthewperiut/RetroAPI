package com.periut.retroapi.movement;

/**
 * Values shared between the sprinting and swimming code. Everything under "swimming" is lifted
 * straight from 26.2 so the backport lands on the same numbers modern Minecraft uses.
 */
public final class MovementConstants {
    /** Shared entity flag bit. Vanilla b1.7.3 uses 0 (on fire), 1 (sneaking) and 2 (riding). */
    public static final int SPRINT_FLAG = 3;
    /** The same bit modern Minecraft uses for Entity.FLAG_SWIMMING. */
    public static final int SWIM_FLAG = 4;

    public static final int START_RUNNING_COMMAND = 4;
    public static final int STOP_RUNNING_COMMAND = 5;

    /** Ticks the "tap forward again to sprint" window stays open - Options.sprintWindow defaults to 7. */
    public static final int SPRINT_TRIGGER_TIME = 7;
    /** Input.hasForwardImpulse() threshold. */
    public static final float FORWARD_IMPULSE = 1.0E-5F;

    /** Avatar.SWIMMING_BB_WIDTH / SWIMMING_BB_HEIGHT and the Pose.SWIMMING eye height. */
    public static final float SWIMMING_WIDTH = 0.6F;
    public static final float SWIMMING_HEIGHT = 0.6F;
    public static final float SWIMMING_EYE_HEIGHT = 0.4F;

    public static final float STANDING_WIDTH = 0.6F;
    public static final float STANDING_HEIGHT = 1.8F;

    /** LivingEntity.SWIM_AMOUNT_PER_TICK - how fast the visual swim lean blends in and out. */
    public static final float SWIM_AMOUNT_PER_TICK = 0.09F;

    /** LivingEntity.BASE_SWIM_SPEED. */
    public static final float BASE_SWIM_SPEED = 0.02F;
    /** Horizontal water drag while sprint-swimming, versus getWaterSlowDown() the rest of the time. */
    public static final float SPRINT_WATER_SLOW_DOWN = 0.9F;
    /** Vertical water drag, applied whether or not the entity is sprinting. */
    public static final float WATER_VERTICAL_SLOW_DOWN = 0.8F;

    /** Player.travel: how hard the look direction pulls a swimmer up or down. */
    public static final double SWIM_PITCH_PULL = 0.06;
    public static final double SWIM_PITCH_PULL_STEEP = 0.085;

    /** LivingEntity.goDownInWater. */
    public static final double WATER_SINK_NUDGE = 0.04;

    private MovementConstants() {
    }
}
