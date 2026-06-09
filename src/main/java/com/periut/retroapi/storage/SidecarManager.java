package com.periut.retroapi.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Owns the per-world RetroAPI sidecar files and their lifecycle.
 *
 * <p>Sidecars are keyed by <em>dimension</em> as well as region coordinates: two dimensions share the
 * same chunk coordinate space, so without the dimension in the key/path a modded block placed at
 * (0,0) in a modded dimension would clobber the overworld's sidecar. The overworld (dimension 0) keeps
 * the legacy {@code retroapi/chunks} / {@code retroapi/inventories} layout for back-compat; every other
 * dimension lives under {@code retroapi/DIM<id>/...} (mirroring vanilla's {@code DIM-1}/{@code DIM<n>}
 * region folders).
 */
public class SidecarManager {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/SidecarManager");

	private static File worldDir;
	private static final Map<String, RegionSidecar> cache = new HashMap<>();
	private static final Map<String, InventorySidecar> inventoryCache = new HashMap<>();

	public static void setWorldDir(File dir) {
		flush();
		worldDir = dir;
		LOGGER.info("SidecarManager initialized for world: {}", dir);
	}

	public static File getWorldDir() {
		return worldDir;
	}

	/** {@code retroapi/} for the overworld, {@code retroapi/DIM<id>/} otherwise. */
	private static String dimensionDir(int dimensionId) {
		return dimensionId == 0 ? "retroapi/" : "retroapi/DIM" + dimensionId + "/";
	}

	private static String regionKey(int dimensionId, int regionX, int regionZ) {
		return dimensionId + ":" + regionX + ":" + regionZ;
	}

	public static RegionSidecar getRegion(int dimensionId, int chunkX, int chunkZ) {
		if (worldDir == null) return null;
		int regionX = chunkX >> 5;
		int regionZ = chunkZ >> 5;
		String key = "c|" + regionKey(dimensionId, regionX, regionZ);

		return cache.computeIfAbsent(key, k -> {
			File regionFile = new File(worldDir, dimensionDir(dimensionId) + "chunks/r." + regionX + "." + regionZ + ".dat");
			return new RegionSidecar(regionFile);
		});
	}

	public static InventorySidecar getInventoryRegion(int dimensionId, int chunkX, int chunkZ) {
		if (worldDir == null) return null;
		int regionX = chunkX >> 5;
		int regionZ = chunkZ >> 5;
		String key = "i|" + regionKey(dimensionId, regionX, regionZ);

		return inventoryCache.computeIfAbsent(key, k -> {
			File regionFile = new File(worldDir, dimensionDir(dimensionId) + "inventories/r." + regionX + "." + regionZ + ".dat");
			return new InventorySidecar(regionFile);
		});
	}

	public static void saveAll() {
		for (RegionSidecar region : cache.values()) {
			region.save();
		}
		for (InventorySidecar inv : inventoryCache.values()) {
			inv.save();
		}
	}

	public static void flush() {
		saveAll();
		// World switch: block until every queued async write has reached disk before the caches
		// (and worldDir) are torn down - the next world must never observe half-written sidecars.
		SidecarIo.drain();
		cache.clear();
		inventoryCache.clear();
		InventorySidecar.clearPending();
		PlayerDimensionSidecar.reset();
		PlayerItemSidecar.reset();
		worldDir = null;
	}
}
