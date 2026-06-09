package com.periut.retroapi.biome;

import net.minecraft.entity.LivingEntity;
import net.minecraft.world.biome.Biome;

/**
 * Fluent builder for a modded {@link Biome} (tint colours, weather flags, per-biome spawn weights),
 * used inside a {@link com.periut.retroapi.biome.event.BiomeRegistrationCallback} listener:
 *
 * <pre>{@code
 * Biome aether = BiomeBuilder.start("aether")
 *     .grassAndLeavesColor(353825)
 *     .precipitation(false)
 *     .snow(false)
 *     .passiveEntity(EntityMoa.class, 10)
 *     .hostileEntity(EntityCockatrice.class, 3)
 *     .build();
 * }</pre>
 *
 * <p>API-compatible with StationAPI's {@code BiomeBuilder} so a consuming mod barely changes. The
 * built biome is a plain vanilla {@link Biome} - a custom dimension hands it back from its
 * {@code BiomeSource}, so no global biome registration is required.
 */
public final class BiomeBuilder {
	private final RetroBiome biome = new RetroBiome();

	private BiomeBuilder(String name) {
		this.biome.name = name;
	}

	public static BiomeBuilder start(String name) {
		return new BiomeBuilder(name);
	}

	/** Sets both the grass tint and the foliage (leaves) tint to the same colour. */
	public BiomeBuilder grassAndLeavesColor(int color) {
		this.biome.grassColor = color;
		this.biome.foliageColor = color;
		return this;
	}

	public BiomeBuilder grassColor(int color) {
		this.biome.grassColor = color;
		return this;
	}

	public BiomeBuilder leavesColor(int color) {
		this.biome.foliageColor = color;
		return this;
	}

	/** {@code false} disables rain in this biome (access-widened {@code hasRain}). */
	public BiomeBuilder precipitation(boolean hasRain) {
		this.biome.hasRain = hasRain;
		return this;
	}

	/** {@code true} enables snowfall in this biome (access-widened {@code hasSnow}). */
	public BiomeBuilder snow(boolean hasSnow) {
		this.biome.hasSnow = hasSnow;
		return this;
	}

	public BiomeBuilder passiveEntity(Class<? extends LivingEntity> entityClass, int rarity) {
		this.biome.addPassive(entityClass, rarity);
		return this;
	}

	public BiomeBuilder hostileEntity(Class<? extends LivingEntity> entityClass, int rarity) {
		this.biome.addMonster(entityClass, rarity);
		return this;
	}

	public BiomeBuilder waterCreature(Class<? extends LivingEntity> entityClass, int rarity) {
		this.biome.addWaterCreature(entityClass, rarity);
		return this;
	}

	public Biome build() {
		return this.biome;
	}
}
