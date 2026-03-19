package com.periut.retroapi.compat;

import com.periut.retroapi.register.block.RetroTextures;
import net.mine_diver.unsafeevents.listener.EventListener;
import net.modificationstation.stationapi.api.client.event.texture.TextureRegisterEvent;
import net.modificationstation.stationapi.api.mod.entrypoint.Entrypoint;
import net.modificationstation.stationapi.api.mod.entrypoint.EventBusPolicy;
import net.modificationstation.stationapi.api.util.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * StationAPI event bus entrypoint - handles texture registration during StationAPI's lifecycle.
 * Only loaded when StationAPI is present.
 */
@Entrypoint(eventBus = @EventBusPolicy(registerInstance = true))
public class StationAPIRegistryForwarder {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/StationAPI");

	@Entrypoint.Namespace
	public Namespace namespace = Namespace.of("retroapi");

	@Entrypoint.Logger
	public Logger logger = LOGGER;

	@EventListener
	public void registerTextures(TextureRegisterEvent event) {
		LOGGER.info("Resolving RetroAPI textures with StationAPI atlas system");
		RetroTextures.resolveStationAPITextures();
	}
}
