package com.periut.retroapi.entity.client;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

/**
 * Client-side factory for a living modded entity (mob). Mirrors StationAPI's
 * {@code Function<World, LivingEntity>} mob handler so an Aether port is a near-mechanical swap.
 */
@FunctionalInterface
public interface MobFactory {
	LivingEntity create(World world);
}
