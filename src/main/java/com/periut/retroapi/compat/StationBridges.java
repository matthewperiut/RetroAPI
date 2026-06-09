package com.periut.retroapi.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holds the active {@link StationBridge}. When the optional {@code retroapi-stationapi} mod is present, its
 * implementation ({@code com.periut.retroapi.stationapi.StationBridgeImpl}) is loaded reflectively on first
 * use - by class name, so core never references a StationAPI-dependent type. When it is absent (the default),
 * a {@link NoopStationBridge} is used. All core call sites already gate on
 * {@code FabricLoader.isModLoaded("stationapi")}, so the no-op methods are never actually invoked.
 *
 * <p>Reflective lazy init (rather than an entrypoint that pushes the impl in) keeps this free of any mod
 * load-order assumption: the impl resolves the first time core asks for it, which is always after Fabric has
 * finished discovering mods.
 */
public final class StationBridges {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/StationAPI");
	private static final String IMPL_CLASS = "com.periut.retroapi.stationapi.StationBridgeImpl";

	private static volatile StationBridge instance;

	private StationBridges() {}

	public static StationBridge get() {
		StationBridge local = instance;
		if (local == null) {
			synchronized (StationBridges.class) {
				local = instance;
				if (local == null) {
					local = load();
					instance = local;
				}
			}
		}
		return local;
	}

	private static StationBridge load() {
		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			try {
				return (StationBridge) Class.forName(IMPL_CLASS)
					.getDeclaredConstructor()
					.newInstance();
			} catch (ReflectiveOperationException | LinkageError e) {
				LOGGER.error("StationAPI is present but the RetroAPI StationAPI bridge ({}) failed to load; "
					+ "StationAPI integration is disabled.", IMPL_CLASS, e);
			}
		}
		return new NoopStationBridge();
	}
}
