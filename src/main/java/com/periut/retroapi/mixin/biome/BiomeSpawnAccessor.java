package com.periut.retroapi.mixin.biome;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.EntitySpawnGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * Reaches the protected spawn lists of ANY biome (vanilla or modded), so
 * {@link com.periut.retroapi.biome.RetroBiomes} can edit them additively without
 * subclassing. RetroBiome reaches them by being a subclass; this reaches a vanilla
 * Biome instance from outside.
 */
@Mixin(Biome.class)
public interface BiomeSpawnAccessor {

	@Accessor("spawnableMonsters")
	List<EntitySpawnGroup> retroapi$getMonsters();

	@Accessor("spawnablePassive")
	List<EntitySpawnGroup> retroapi$getPassive();

	@Accessor("spawnableWaterCreatures")
	List<EntitySpawnGroup> retroapi$getWaterCreatures();
}
