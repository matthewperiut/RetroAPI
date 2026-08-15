package com.periut.retroapi.movement.api;

/**
 * Implemented on every Entity by the mod. Mirrors the swimming half of modern Entity/LivingEntity:
 * a synced state flag plus the interpolated lean used by the renderer.
 */
public interface EntitySwimming {
    void setSwimming(boolean swimming);

    boolean isSwimming();

    /**
     * True while the entity is drawn in the swimming pose. Modern splits this from
     * {@link #isSwimming()} so the horizontal pose can outlive the state (crawling); here they
     * only differ for the tick the pose cannot be left because there is no headroom.
     */
    boolean isVisuallySwimming();

    /** LivingEntity.getSwimAmount - 0 standing, 1 fully leaned into the swim pose. */
    float getSwimAmount(float tickDelta);

    /**
     * Entity.isUnderWater - the head is submerged, as opposed to vanilla's isSubmergedInWater()
     * which despite the name only means "touching water".
     */
    boolean isUnderWater();
}
