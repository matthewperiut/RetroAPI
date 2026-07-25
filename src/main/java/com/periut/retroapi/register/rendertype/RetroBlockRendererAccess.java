package com.periut.retroapi.register.rendertype;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Duck interface injected into BlockRenderer for per-vertex colour support and the texture controls
 * {@link BlockRenderContext} exposes (sprite override, mirroring, per-face rotation, face culling).
 */
@Environment(EnvType.CLIENT)
public interface RetroBlockRendererAccess {
	void retroapi$setupSmoothFace(float v1, float v2, float v3, float v4, float shade);
	void retroapi$cleanupSmoothFace();

	/** Forces one sprite on every face (-1 to clear). */
	void retroapi$setTextureOverride(int sprite);

	/** Mirrors face textures horizontally. */
	void retroapi$setFlipTexture(boolean flipped);

	/** Rotates one face's texture in 90° steps; face indices are 0 bottom … 5 east. */
	void retroapi$setFaceRotation(int face, int quarterTurns);

	/** Draws faces that a neighbour would normally hide. */
	void retroapi$setSkipFaceCulling(boolean skip);
}
