package com.periut.retroapi.compat;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.registry.IdMap;
import net.modificationstation.stationapi.api.vanillafix.datafixer.schema.StationFlatteningItemStackSchema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Map;

/**
 * Injects RetroAPI's numerical-to-flattened ID mappings into StationAPI's
 * conversion lookup tables before a world is converted from McRegion to
 * the flattened format.
 *
 * This enables seamless conversion of:
 * - Placed blocks (chunk data)
 * - Items in inventories, chests, and dropped entities
 *
 * The reverse (unconversion) is also handled automatically because
 * putState/putItem populate bidirectional lookup maps.
 */
public class WorldConversionHelper {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/WorldConversion");

	public static void injectRetroApiMappings(File worldDir) {
		LOGGER.info("RetroAPI world conversion hook fired for: {}", worldDir);
		File idMapFile = new File(worldDir, "retroapi/id_map.dat");
		if (!idMapFile.exists()) {
			LOGGER.info("No retroapi/id_map.dat found in {}, skipping conversion injection", worldDir);
			return;
		}

		IdMap idMap = new IdMap();
		idMap.load(idMapFile);

		int blockCount = 0;
		int itemCount = 0;

		for (Map.Entry<NamespacedIdentifier, Integer> entry : idMap.getBlockIds().entrySet()) {
			int numericId = entry.getValue();
			String flattenedId = entry.getKey().toString();

			// OLD_ID_TO_BLOCKSTATE is only 256 entries; extended blocks (>= 256) are stored
			// as air in vanilla chunk data, so they never appear in block conversion.
			if (numericId < 256) {
				StationFlatteningItemStackSchema.putState(numericId, flattenedId);
			}
			// Always register as item (OLD_ID_TO_ITEM has 32000 entries) for block items in inventories
			StationFlatteningItemStackSchema.putItem(numericId, flattenedId);
			blockCount++;
			LOGGER.debug("Injected block mapping: {} -> {}", numericId, flattenedId);
		}

		for (Map.Entry<NamespacedIdentifier, Integer> entry : idMap.getItemIds().entrySet()) {
			int numericId = entry.getValue();
			String flattenedId = entry.getKey().toString();

			// putItem registers the item for item stack conversion
			StationFlatteningItemStackSchema.putItem(numericId, flattenedId);
			itemCount++;
			LOGGER.debug("Injected item mapping: {} -> {}", numericId, flattenedId);
		}

		LOGGER.info("Injected {} block and {} item RetroAPI mappings for world conversion", blockCount, itemCount);
	}
}
