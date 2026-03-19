package com.periut.retroapi.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SidecarManager {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/SidecarManager");

	private static File worldDir;
	private static final Map<Long, RegionSidecar> cache = new HashMap<>();
	private static final Map<Long, InventorySidecar> inventoryCache = new HashMap<>();

	public static void setWorldDir(File dir) {
		flush();
		worldDir = dir;
		LOGGER.info("SidecarManager initialized for world: {}", dir);
	}

	public static File getWorldDir() {
		return worldDir;
	}

	public static RegionSidecar getRegion(int chunkX, int chunkZ) {
		if (worldDir == null) return null;
		int regionX = chunkX >> 5;
		int regionZ = chunkZ >> 5;
		long key = ((long) regionX << 32) | (regionZ & 0xFFFFFFFFL);

		return cache.computeIfAbsent(key, k -> {
			File regionFile = new File(worldDir, "retroapi/chunks/r." + regionX + "." + regionZ + ".dat");
			return new RegionSidecar(regionFile);
		});
	}

	public static InventorySidecar getInventoryRegion(int chunkX, int chunkZ) {
		if (worldDir == null) return null;
		int regionX = chunkX >> 5;
		int regionZ = chunkZ >> 5;
		long key = ((long) regionX << 32) | (regionZ & 0xFFFFFFFFL);

		return inventoryCache.computeIfAbsent(key, k -> {
			File regionFile = new File(worldDir, "retroapi/inventories/r." + regionX + "." + regionZ + ".dat");
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
		cache.clear();
		inventoryCache.clear();
		worldDir = null;
	}
}
