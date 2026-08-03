package com.periut.retroapi.world.carver;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.world.height.RetroWorldHeight;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Terrain carving you can register, and vanilla terrain carving you can turn off.
 *
 * <p>In beta, caves are welded into the chunk generators: each of the three constructs its own
 * {@code CaveCarver} in its constructor and calls it from {@code getChunk}, with no registry, no event
 * and no switch. A mod that wanted different caves had two options, both bad - mixin every generator, or
 * carve afterwards through {@code world.setBlock} and pay lighting and re-render costs for every block
 * of every cave in the world.
 *
 * <p>This hooks {@code Carver.carve}, the one method every generator funnels through, and gives both
 * halves of what a cave mod actually needs:
 *
 * <pre>{@code
 * // in your `retroapi` entrypoint
 * RetroCarvers.setVanillaCarving(0, false);           // no vanilla caves in the overworld
 * RetroCarvers.register(new MyNoiseCarver()).dimension(0).register();
 * }</pre>
 *
 * <p>Registered carvers run in registration order, after vanilla's own carving if that is still enabled.
 *
 * <h2>Custom generators</h2>
 * A {@code ChunkSource} that never touches vanilla's {@code Carver} is never hooked - there is nothing to
 * hook. Call {@link #carve(World, int, int, byte[])} from the generator directly, right after building
 * the block array and before the surface pass.
 *
 * <h2>StationAPI</h2>
 * The veto is a cancellable inject at the head of {@code Carver.carve}. StationAPI's own mixin on that
 * method only binds a world reference for its flattening; the two compose, so nothing here needs to be
 * disabled when StationAPI is present.
 */
public final class RetroCarvers {

	private static final List<Registered> CARVERS = new ArrayList<>();
	/** Dimension id -> whether vanilla carving still runs. Absent means yes. */
	private static final Map<Integer, Boolean> VANILLA_CARVING = new HashMap<>();

	private RetroCarvers() {}

	private static final class Registered {
		final RetroCarver carver;
		final Integer dimensionId;

		Registered(RetroCarver carver, Integer dimensionId) {
			this.carver = carver;
			this.dimensionId = dimensionId;
		}

		boolean appliesTo(World world) {
			return dimensionId == null || world.dimension.id == dimensionId;
		}
	}

	// --- registration -------------------------------------------------------------------------

	/** Starts registering a carver. Call {@link Builder#register()} to finish. */
	public static Builder register(RetroCarver carver) {
		return new Builder(carver);
	}

	/** Fluent tail of {@link RetroCarvers#register(RetroCarver)}. */
	public static final class Builder {
		private final RetroCarver carver;
		private Integer dimensionId;

		Builder(RetroCarver carver) {
			this.carver = carver;
		}

		/** Restricts this carver to one dimension. Without it, the carver runs in every dimension. */
		public Builder dimension(int dimensionId) {
			this.dimensionId = dimensionId;
			return this;
		}

		public void register() {
			CARVERS.add(new Registered(carver, dimensionId));
			RetroAPI.LOGGER.debug("Registered world carver{}",
				dimensionId == null ? " (all dimensions)" : " for dimension " + dimensionId);
		}
	}

	/**
	 * Turns vanilla cave/ravine carving on or off for one dimension. Off means beta's own
	 * {@code CaveCarver} never touches a chunk there, so a mod's carver owns the underground outright
	 * instead of layering its caves on top of vanilla's.
	 */
	public static void setVanillaCarving(int dimensionId, boolean enabled) {
		VANILLA_CARVING.put(dimensionId, enabled);
	}

	/** Whether vanilla carving still runs in a dimension. True unless a mod said otherwise. */
	public static boolean isVanillaCarvingEnabled(int dimensionId) {
		return VANILLA_CARVING.getOrDefault(dimensionId, Boolean.TRUE);
	}

	/** True when any carver is registered - a cheap pre-check for the hook. */
	public static boolean hasCarvers() {
		return !CARVERS.isEmpty();
	}

	// --- generation ---------------------------------------------------------------------------

	/**
	 * Runs every registered carver for one chunk. Called by RetroAPI's hook on {@code Carver.carve}; a
	 * custom generator that bypasses vanilla carving entirely should call this itself.
	 */
	public static void carve(World world, int chunkX, int chunkZ, byte[] blocks) {
		if (CARVERS.isEmpty() || world == null || blocks == null) {
			return;
		}
		int dimensionId = world.dimension.id;
		RetroCarverContext context = new RetroCarverContext(world, blocks, chunkX, chunkZ, world.getSeed(),
			RetroWorldHeight.bottomY(dimensionId), RetroWorldHeight.topY(dimensionId));

		for (int i = 0; i < CARVERS.size(); i++) {
			Registered registered = CARVERS.get(i);
			if (!registered.appliesTo(world)) {
				continue;
			}
			try {
				registered.carver.carve(context, blocks);
			} catch (RuntimeException e) {
				// One broken carver must not take the chunk - and with it the world - down.
				RetroAPI.LOGGER.error("Carver failed on chunk {},{}", chunkX, chunkZ, e);
			}
		}
	}
}
