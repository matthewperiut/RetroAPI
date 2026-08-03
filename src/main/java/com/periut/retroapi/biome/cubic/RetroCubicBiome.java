package com.periut.retroapi.biome.cubic;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * A registered cubic biome: an identity a volume of the world can be tagged with, independent of the
 * surface {@link net.minecraft.world.biome.Biome} above it.
 *
 * <p>Deliberately almost empty. A surface biome in beta is a bundle of concrete decisions - grass
 * colour, top block, spawn lists, rainfall - because beta's generator consults it while building
 * terrain. A cubic biome is consulted by <em>mods</em>, and every mod wants different things from it: a
 * cave mod wants a block palette and a feature list, an ambience mod wants a fog colour and a sound
 * loop, a mob mod wants spawn entries. Baking any one of those in would make the other two wrong. So
 * this carries an identity and nothing else, and mods key their own tables off it.
 *
 * @see RetroCubicBiomes
 */
public final class RetroCubicBiome {

	private final NamespacedIdentifier id;
	private final int runtimeId;

	RetroCubicBiome(NamespacedIdentifier id, int runtimeId) {
		this.id = id;
		this.runtimeId = runtimeId;
	}

	/** The registered identifier. This, not {@link #getRuntimeId()}, is what the world stores. */
	public NamespacedIdentifier getId() {
		return id;
	}

	/**
	 * The per-session integer this biome is held as in chunk storage. Never persist it: like a modded
	 * block id it is an allocation of the installed mod set, not a property of the world, so it moves
	 * when mods are added or removed. Always &gt; 0 - zero means "unassigned".
	 */
	public int getRuntimeId() {
		return runtimeId;
	}

	@Override
	public String toString() {
		return id.toString();
	}
}
