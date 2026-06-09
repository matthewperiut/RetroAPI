package com.periut.retroapi.client.model;

/**
 * The atlas-coordinate strategy behind {@link RetroAtlas}: maps a terrain sprite slot + a 0-16 pixel
 * coordinate to an atlas UV. Chosen ONCE at class load (RetroAPI's expanded-atlas grid, or StationAPI's
 * sprite atlas) and stored in a {@code static final} field, so the per-vertex render path is a single
 * devirtualized + inlined call with no StationAPI branch. Being an interface also keeps the
 * StationAPI-referencing implementation off the verifier's radar until it is actually instantiated.
 */
public interface TerrainUv {
	double u(int slot, double pixelU);

	double v(int slot, double pixelV);
}
