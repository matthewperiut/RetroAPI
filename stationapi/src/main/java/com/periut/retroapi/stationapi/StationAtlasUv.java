package com.periut.retroapi.stationapi;

import com.periut.retroapi.client.model.TerrainUv;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlas;
import net.modificationstation.stationapi.api.client.texture.atlas.Atlases;

/**
 * StationAPI {@link TerrainUv} strategy. Under StationAPI a RetroAPI block texture's slot IS a StationAPI
 * terrain-atlas sprite index (re-pointed by {@code RetroTextures.resolveStationAPITextures}), but
 * StationAPI's atlas is dynamically sized - NOT the vanilla 16-column / 256px grid {@link RetroAtlas}'s
 * default strategy assumes. So map the slot + pixel coordinate onto the sprite's real UV span.
 *
 * <p>Only ever instantiated by {@link RetroAtlas} when StationAPI is loaded, so this class (and its
 * StationAPI imports) never loads without StationAPI. A one-entry sprite cache keeps the per-vertex calls
 * cheap (a face's four corners share one slot).
 */
public final class StationAtlasUv implements TerrainUv {

	private int lastSlot = -1;
	private Atlas.Sprite lastSprite;

	@Override
	public double u(int slot, double pixelU) {
		Atlas.Sprite s = sprite(slot);
		return s.getStartU() + (pixelU / 16.0) * (s.getEndU() - s.getStartU());
	}

	@Override
	public double v(int slot, double pixelV) {
		Atlas.Sprite s = sprite(slot);
		return s.getStartV() + (pixelV / 16.0) * (s.getEndV() - s.getStartV());
	}

	private Atlas.Sprite sprite(int slot) {
		if (slot != lastSlot || lastSprite == null) {
			lastSlot = slot;
			lastSprite = Atlases.getTerrain().getTexture(slot);
		}
		return lastSprite;
	}
}
