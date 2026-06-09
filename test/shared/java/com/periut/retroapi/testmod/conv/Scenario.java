package com.periut.retroapi.testmod.conv;

import java.io.File;

/**
 * Drives the headless conversion round-trip test. A scenario is selected with
 * {@code -Dretroapi.test.scenario=<name>} and operates on the world directory
 * {@code -Dretroapi.test.world=<dir>} (default: the dedicated-server world {@code world}).
 *
 * <ul>
 *   <li>{@code populate} (non-StationAPI server) - place a deterministic set of modded content
 *       (extended blocks with metadata, a vanilla chest of modded + component-bearing items,
 *       dropped modded item entities, a persistent modded mob, test-dimension content), verify it
 *       in-engine, write a {@link ConvManifest}, save and stop. Produces a McRegion + RetroAPI
 *       sidecar world.</li>
 *   <li>{@code roundtrip} (StationAPI client) - at the title screen, forward-convert the world to
 *       StationAPI's flattened format, verify the modded content survived on disk, reverse-convert
 *       it back to McRegion, verify again, write a result file and exit (non-zero on any data loss).</li>
 *   <li>{@code verify} (non-StationAPI server) - load the (reverse-converted) world and confirm the
 *       modded content is runtime-valid in plain RetroAPI.</li>
 * </ul>
 *
 * The whole flow is wired together by the {@code conversionRoundTripTest} Gradle task; each step is a
 * separate JVM that runs to completion and exits, so a failure in any step fails the build.
 */
public final class Scenario {
	private Scenario() {}

	public static final String POPULATE = "populate";
	public static final String ROUNDTRIP = "roundtrip";
	public static final String VERIFY = "verify";

	/** Manifest + result file names, kept inside the world directory so they travel with it. */
	public static final String MANIFEST_FILE = "retroapi_test_manifest.dat";
	public static final String RESULT_FILE = "retroapi_test_result.txt";

	public static String name() {
		return System.getProperty("retroapi.test.scenario", "");
	}

	public static boolean is(String scenario) {
		return scenario.equalsIgnoreCase(name());
	}

	public static boolean isActive() {
		return !name().isEmpty();
	}

	/** The world directory the scenario operates on (dedicated-server default is {@code world}). */
	public static File worldDir() {
		return new File(System.getProperty("retroapi.test.world", "world"));
	}
}
