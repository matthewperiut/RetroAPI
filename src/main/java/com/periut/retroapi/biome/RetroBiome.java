package com.periut.retroapi.biome;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.EntitySpawnGroup;

/**
 * A modded biome. Extending {@link Biome} is the cleanest way to populate it: the base constructor
 * seeds the vanilla default spawns (spider/zombie/sheep/...) into the protected spawn lists, which a
 * modded biome almost never wants - so this clears them and lets {@link BiomeBuilder} add exactly the
 * modded spawns. Subclassing also grants access to the protected spawn lists.
 */
public class RetroBiome extends Biome {
	public RetroBiome() {
		super();
		this.spawnableMonsters.clear();
		this.spawnablePassive.clear();
		this.spawnableWaterCreatures.clear();
	}

	public void addPassive(Class<? extends LivingEntity> entityClass, int weight) {
		this.spawnablePassive.add(new EntitySpawnGroup(entityClass, weight));
		com.periut.retroapi.entity.RetroSpawnGroups.assign(entityClass, net.minecraft.entity.SpawnGroup.CREATURE);
	}

	public void addMonster(Class<? extends LivingEntity> entityClass, int weight) {
		this.spawnableMonsters.add(new EntitySpawnGroup(entityClass, weight));
		com.periut.retroapi.entity.RetroSpawnGroups.assign(entityClass, net.minecraft.entity.SpawnGroup.MONSTER);
	}

	public void addWaterCreature(Class<? extends LivingEntity> entityClass, int weight) {
		this.spawnableWaterCreatures.add(new EntitySpawnGroup(entityClass, weight));
		com.periut.retroapi.entity.RetroSpawnGroups.assign(entityClass, net.minecraft.entity.SpawnGroup.WATER_CREATURE);
	}
}
