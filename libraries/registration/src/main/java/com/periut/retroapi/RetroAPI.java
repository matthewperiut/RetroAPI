package com.periut.retroapi;

import com.periut.retroapi.register.block.event.BlockRegistrationCallback;
import com.periut.retroapi.register.item.event.ItemRegistrationCallback;
import com.periut.retroapi.compat.StationAPICompat;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RetroAPI implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger("RetroAPI");

	public static boolean isBlock(int id) {
		return id >= 0 && id < Block.BY_ID.length && Block.BY_ID[id] != null;
	}

	@Override
	public void init() {
		LOGGER.info("RetroAPI initializing");
		boolean hasStationAPI = FabricLoader.getInstance().isModLoaded("stationapi");

		if (!hasStationAPI) {
			// Fire registration events so mods can register their blocks/items
			BlockRegistrationCallback.EVENT.invoker().run();
			ItemRegistrationCallback.EVENT.invoker().run();

			LOGGER.info("Registered {} blocks and {} items",
				RetroRegistry.getBlocks().size(), RetroRegistry.getItems().size());
		} else {
			// When StationAPI is present, it handles ID management and textures.
			// Register our lang path so StationAPI scans lang/ directories.
			StationAPICompat.registerLangPath();
			LOGGER.info("StationAPI detected - delegating registration, ID management, and textures to StationAPI");
		}
	}
}
