package com.periut.retroapi.particle;

import net.minecraft.world.World;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.Random;

/**
 * Spawning custom particles from common code.
 *
 * <p>Beta's particle system is a hardcoded {@code if/else} over names inside the client renderer, so mods
 * could not add one at all. RetroAPI turns it into a registry: the client side registers a factory per
 * identifier ({@code RetroParticleRegistry.register} from your {@code retroapi-client} entrypoint), and
 * <em>this</em> class spawns them from wherever your gameplay code lives - block ticks, item use,
 * block-entity logic - with no client types in sight.
 *
 * <pre>
 * // common code (a block's onUse, a block entity's tick, …)
 * RetroParticles.spawn(world, MyMod.SPARK, x + 0.5, y + 1.0, z + 0.5);
 * RetroParticles.spawnCloud(world, MyMod.SPARK, x + 0.5, y + 1.0, z + 0.5, 12, 0.25);
 * </pre>
 *
 * <h2>Multiplayer</h2>
 * b1.7.3's protocol has no particle packet, and vanilla's server-side world event listener drops
 * {@code addParticle} on the floor - so a particle spawned by server logic reached nobody. RetroAPI
 * bridges it (the same way it bridges sounds): spawn from server code and the players in range see it.
 * Spawning still works unchanged in singleplayer, where the client IS the authority.
 */
public final class RetroParticles {

	/** How far away (in blocks) a bridged particle is still sent to a player. */
	public static final double BRIDGE_RANGE = 32.0;

	private static final Random RANDOM = new Random();

	private RetroParticles() {}

	/** Spawns one particle at rest. */
	public static void spawn(World world, NamespacedIdentifier id, double x, double y, double z) {
		spawn(world, id, x, y, z, 0.0, 0.0, 0.0);
	}

	/** Spawns one particle with a velocity (blocks per tick). */
	public static void spawn(World world, NamespacedIdentifier id, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ) {
		if (world == null || id == null) {
			return;
		}
		world.addParticle(id.toString(), x, y, z, velocityX, velocityY, velocityZ);
	}

	/** Spawns a vanilla particle by its beta name ({@code "smoke"}, {@code "flame"}, {@code "reddust"}, …). */
	public static void spawnVanilla(World world, String name, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ) {
		if (world != null) {
			world.addParticle(name, x, y, z, velocityX, velocityY, velocityZ);
		}
	}

	/**
	 * Spawns {@code count} particles scattered in a cube of {@code spread} blocks around the point, each
	 * with a small random drift - the "puff of something happened here" effect, without the loop.
	 */
	public static void spawnCloud(World world, NamespacedIdentifier id, double x, double y, double z,
			int count, double spread) {
		for (int i = 0; i < count; i++) {
			spawn(world, id,
				x + (RANDOM.nextDouble() - 0.5) * 2.0 * spread,
				y + (RANDOM.nextDouble() - 0.5) * 2.0 * spread,
				z + (RANDOM.nextDouble() - 0.5) * 2.0 * spread,
				(RANDOM.nextDouble() - 0.5) * 0.1,
				RANDOM.nextDouble() * 0.1,
				(RANDOM.nextDouble() - 0.5) * 0.1);
		}
	}

	/**
	 * Spawns particles evenly over the faces of a block, the way vanilla dresses a block that is doing
	 * something (a furnace burning, a portal humming).
	 */
	public static void spawnOnBlock(World world, NamespacedIdentifier id, int x, int y, int z, int count) {
		for (int i = 0; i < count; i++) {
			spawn(world, id,
				x + RANDOM.nextDouble(),
				y + RANDOM.nextDouble(),
				z + RANDOM.nextDouble(),
				0.0, 0.0, 0.0);
		}
	}
}
