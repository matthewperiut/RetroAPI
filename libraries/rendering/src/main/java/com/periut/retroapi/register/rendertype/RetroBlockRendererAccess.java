package com.periut.retroapi.register.rendertype;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Duck interface injected into BlockRenderer for per-vertex color support.
 */
@Environment(EnvType.CLIENT)
public interface RetroBlockRendererAccess {
	void retroapi$setupSmoothFace(float v1, float v2, float v3, float v4, float shade);
	void retroapi$cleanupSmoothFace();
}
