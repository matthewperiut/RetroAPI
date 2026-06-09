package com.periut.retroapi.biome;

import com.periut.retroapi.mixin.biome.BiomeSpawnAccessor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.EntitySpawnGroup;

import java.util.List;

/**
 * Mod-friendly edits to EXISTING biomes, vanilla or another mod's. Where
 * {@link BiomeBuilder} authors a brand-new biome, this reaches into one that already
 * exists and tweaks it.
 *
 * <p>The guiding rule is <b>additive, never destructive</b>. Beta has a single global
 * set of biome objects ({@code Biome.PLAINS}, {@code Biome.FOREST}, ...) shared by every
 * dimension and every mod, so clobbering a spawn list or a colour is how mods break each
 * other. Every method here ADDS to a list (and {@link #addSpawn} de-dupes by class first,
 * so registering the same mob twice does not stack), leaving every other mod's entries in
 * place. Reach for {@link #setSpawn}/{@link #clearSpawns} only when you genuinely mean to
 * replace, and know you are taking that biome over.</p>
 *
 * <pre>{@code
 * // Make zombies a touch rarer is NOT what this does; it ADDS your mob alongside theirs:
 * RetroBiomes.addPassiveSpawn(Biome.PLAINS, AerbunnyEntity.class, 8);
 * RetroBiomes.addPassiveSpawn(Biome.FOREST, AerbunnyEntity.class, 6);
 * }</pre>
 */
public final class RetroBiomes {

	/** Which spawn list a mob belongs to. */
	public enum SpawnGroup {
		PASSIVE,
		MONSTER,
		WATER_CREATURE
	}

	private RetroBiomes() {
	}

	private static List<EntitySpawnGroup> list(Biome biome, SpawnGroup group) {
		BiomeSpawnAccessor accessor = (BiomeSpawnAccessor) biome;
		switch (group) {
			case MONSTER: return accessor.retroapi$getMonsters();
			case WATER_CREATURE: return accessor.retroapi$getWaterCreatures();
			default: return accessor.retroapi$getPassive();
		}
	}

	/**
	 * Adds a spawn entry to a biome, leaving every existing entry (vanilla or other mods')
	 * untouched. If the same class is already present in that group its weight is updated
	 * rather than duplicated, so calling this twice is safe.
	 */
	public static void addSpawn(Biome biome, SpawnGroup group, Class<? extends LivingEntity> entityClass, int weight) {
		com.periut.retroapi.entity.RetroSpawnGroups.assign(entityClass, vanillaGroup(group));
		List<EntitySpawnGroup> entries = list(biome, group);
		for (EntitySpawnGroup entry : entries) {
			if (entry.clazz == entityClass) {
				entry.amount = weight;
				return;
			}
		}
		entries.add(new EntitySpawnGroup(entityClass, weight));
	}

	/** Maps the RetroAPI spawn group to beta's SpawnGroup, for the spawn-cap counting. */
	private static net.minecraft.entity.SpawnGroup vanillaGroup(SpawnGroup group) {
		switch (group) {
			case MONSTER: return net.minecraft.entity.SpawnGroup.MONSTER;
			case WATER_CREATURE: return net.minecraft.entity.SpawnGroup.WATER_CREATURE;
			default: return net.minecraft.entity.SpawnGroup.CREATURE;
		}
	}

	/** Convenience: {@link #addSpawn} into the passive (animal) list. */
	public static void addPassiveSpawn(Biome biome, Class<? extends LivingEntity> entityClass, int weight) {
		addSpawn(biome, SpawnGroup.PASSIVE, entityClass, weight);
	}

	/** Convenience: {@link #addSpawn} into the monster list. */
	public static void addMonsterSpawn(Biome biome, Class<? extends LivingEntity> entityClass, int weight) {
		addSpawn(biome, SpawnGroup.MONSTER, entityClass, weight);
	}

	/** Convenience: {@link #addSpawn} into the water-creature list. */
	public static void addWaterSpawn(Biome biome, Class<? extends LivingEntity> entityClass, int weight) {
		addSpawn(biome, SpawnGroup.WATER_CREATURE, entityClass, weight);
	}

	/** Adds a spawn entry to EVERY listed biome (handy for "spawn in all overworld biomes"). */
	public static void addSpawnToAll(Biome[] biomes, SpawnGroup group, Class<? extends LivingEntity> entityClass, int weight) {
		for (Biome biome : biomes) {
			if (biome != null) {
				addSpawn(biome, group, entityClass, weight);
			}
		}
	}

	/**
	 * Replaces a biome's entire spawn list for one group with a single entry. Destructive:
	 * use only when this biome is yours to define. Prefer {@link #addSpawn}.
	 */
	public static void setSpawn(Biome biome, SpawnGroup group, Class<? extends LivingEntity> entityClass, int weight) {
		List<EntitySpawnGroup> entries = list(biome, group);
		entries.clear();
		entries.add(new EntitySpawnGroup(entityClass, weight));
	}

	/** Empties a biome's spawn list for one group. Destructive; see {@link #setSpawn}. */
	public static void clearSpawns(Biome biome, SpawnGroup group) {
		list(biome, group).clear();
	}

	/** Removes one class from a biome's group, leaving the rest. Safe and additive in spirit. */
	public static void removeSpawn(Biome biome, SpawnGroup group, Class<? extends LivingEntity> entityClass) {
		list(biome, group).removeIf(entry -> entry.clazz == entityClass);
	}

	/** Recolours a biome's grass tint (a shared global object, so this affects every dimension using it). */
	public static void setGrassColor(Biome biome, int color) {
		biome.grassColor = color;
	}

	/** Recolours a biome's foliage (leaves) tint. */
	public static void setFoliageColor(Biome biome, int color) {
		biome.foliageColor = color;
	}
}
