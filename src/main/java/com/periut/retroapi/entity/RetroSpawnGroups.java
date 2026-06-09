package com.periut.retroapi.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Makes modded mobs count toward beta's built-in spawn caps.
 *
 * <p>Beta limits natural spawns per category: {@code NaturalSpawner} asks
 * {@code World.countEntities(group.getCreatureClass())} and stops spawning once that
 * count passes the category capacity. The catch is that the count is an
 * {@code instanceof} test against a vanilla base class ({@code AnimalEntity} for
 * CREATURE, {@code Monster} for MONSTER, {@code WaterCreatureEntity} for WATER_CREATURE).
 * A mob that extends {@code LivingEntity} directly, the common case for a custom mob, is
 * not an instance of any of those, so vanilla never counts it and it spawns without
 * limit.</p>
 *
 * <p>This registry records which {@link SpawnGroup} each modded mob class belongs to,
 * populated automatically when the mob is added to a biome's spawn list (via
 * {@code BiomeBuilder} or {@code RetroBiomes}). A mixin on {@code World.countEntities}
 * then adds the modded count, so the cap applies to every mob, vanilla or modded, with
 * no per-mob code.</p>
 */
public final class RetroSpawnGroups {

	private static final Map<Class<?>, SpawnGroup> GROUPS = new HashMap<>();

	private RetroSpawnGroups() {
	}

	/** Records that {@code entityClass} spawns under {@code group} (so the cap counts it). */
	public static void assign(Class<? extends LivingEntity> entityClass, SpawnGroup group) {
		GROUPS.put(entityClass, group);
	}

	/** The group a modded class was assigned to, or null. */
	public static SpawnGroup groupOf(Class<?> entityClass) {
		return GROUPS.get(entityClass);
	}

	/**
	 * The number of loaded modded entities that belong to the category whose vanilla
	 * creature class is {@code creatureClass}, but which are NOT already instances of it
	 * (so they were missed by vanilla's count). Added to the vanilla count by the mixin.
	 */
	public static int extraCount(World world, Class<?> creatureClass) {
		SpawnGroup group = null;
		for (SpawnGroup g : SpawnGroup.values()) {
			if (g.getCreatureClass() == creatureClass) {
				group = g;
				break;
			}
		}
		if (group == null || world.entities == null) {
			return 0;
		}
		int extra = 0;
		for (Object o : world.entities) {
			Entity entity = (Entity) o;
			if (GROUPS.get(entity.getClass()) == group && !creatureClass.isInstance(entity)) {
				extra++;
			}
		}
		return extra;
	}
}
