package com.periut.retroapi.entity.client;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Client-side factory for a non-living modded entity. Mirrors StationAPI's
 * {@code EntityWorldAndPosFactory} so an Aether port is a near-mechanical swap.
 */
@FunctionalInterface
public interface EntityFactory {
	Entity create(World world, double x, double y, double z);
}
