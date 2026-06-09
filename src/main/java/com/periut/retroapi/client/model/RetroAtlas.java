package com.periut.retroapi.client.model;

import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.loader.api.FabricLoader;

/**
 * UV helper for the model renderer: maps (slot, pixel coordinate 0-16) to atlas UV. Read at RENDER time,
 * so atlas reloads and slot reassignment (mutable RetroTexture.id) are always reflected.
 *
 * <p>The atlas-coordinate strategy is chosen ONCE at class load and held in a {@code static final}
 * {@link TerrainUv} field: <b>without StationAPI</b>, RetroAPI's own expanded atlas ({@link Grid} - the
 * sprite-size-aware 16-column grid over {@link AtlasExpander#terrainAtlasSize}); <b>with StationAPI</b>,
 * {@link StationAtlasUv} (the slot is a StationAPI sprite index on its dynamically-sized atlas). Because
 * the strategy is a constant field the JIT devirtualizes + inlines it, so the hot per-vertex path has no
 * StationAPI branch - and the {@code retroapi:model} blocks that rendered with wrong textures under
 * StationAPI (the default grid assumes a 256px atlas that does not exist there) now sample correctly.
 */
public final class RetroAtlas {

	private static final TerrainUv IMPL = chooseStrategy();

	private RetroAtlas() {}

	public static double terrainU(int slot, double uPx) {
		return IMPL.u(slot, uPx);
	}

	public static double terrainV(int slot, double vPx) {
		return IMPL.v(slot, vPx);
	}

	/**
	 * The StationAPI strategy lives in the optional {@code retroapi-stationapi} mod and is loaded reflectively
	 * by class name, so core carries no StationAPI reference. This runs once at class load; when StationAPI is
	 * absent (or the bridge mod is missing), RetroAPI's own expanded-atlas grid is used.
	 */
	private static TerrainUv chooseStrategy() {
		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			try {
				return (TerrainUv) Class.forName("com.periut.retroapi.stationapi.StationAtlasUv")
					.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException | LinkageError e) {
				throw new IllegalStateException("StationAPI present but RetroAPI StationAPI atlas strategy failed to load", e);
			}
		}
		return new Grid();
	}

	/** RetroAPI's own expanded-atlas grid math (sprite-size aware, 16-column over the expanded atlas size). */
	private static final class Grid implements TerrainUv {
		@Override
		public double u(int slot, double uPx) {
			int spriteSize = AtlasExpander.terrainSpriteSize;
			return ((slot % 16) * spriteSize + uPx * spriteSize / 16.0) / AtlasExpander.terrainAtlasSize;
		}

		@Override
		public double v(int slot, double vPx) {
			int spriteSize = AtlasExpander.terrainSpriteSize;
			return ((slot / 16) * spriteSize + vPx * spriteSize / 16.0) / AtlasExpander.terrainAtlasSize;
		}
	}
}
