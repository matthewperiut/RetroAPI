package com.periut.retroapi.testmod;

import net.minecraft.world.dimension.OverworldDimension;

/**
 * Minimal test dimension for smoke-testing RetroAPI's dimension subsystem. Extends
 * {@link OverworldDimension} so it reuses the overworld chunk generator + biome source (terrain
 * actually generates), but is registered under a modded serial id so it lives in its own
 * {@code DIM<serialId>/} save folder. Built via {@code RetroDimensions.register(id, TestDimension::new)}
 * - the {@code IntFunction} factory receives the assigned serial id.
 */
public class TestDimension extends OverworldDimension {
	public TestDimension(int serialId) {
		this.id = serialId;
	}
}
