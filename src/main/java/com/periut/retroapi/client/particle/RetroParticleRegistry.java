package com.periut.retroapi.client.particle;

import com.periut.retroapi.RetroAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The client-side particle registry: identifier -&gt; factory.
 *
 * <p>Beta decides what a particle name means inside a hardcoded chain in the world renderer, with no way
 * in. RetroAPI intercepts that lookup: a name containing a namespace ({@code mymod:spark}) is resolved
 * here first, and only unknown names fall through to vanilla - so registering a particle is now one line,
 * and vanilla's own names keep working untouched.
 *
 * <p>Register from your {@code retroapi-client} entrypoint (particles are a client-only concept; the
 * spawn call in {@link com.periut.retroapi.particle.RetroParticles} is what common code uses):
 * <pre>
 * RetroParticleRegistry.register(MyMod.SPARK, (world, x, y, z, vx, vy, vz) -&gt;
 *     new RetroSpriteParticle(world, x, y, z, vx, vy, vz, MyMod.SPARK_TEXTURE)
 *         .lifetime(20).scale(0.8F).gravity(0.02F).tint(0xFFD070));
 *
 * // or your own Particle subclass, for full control:
 * RetroParticleRegistry.register(MyMod.RUNE, MyRuneParticle::new);
 * </pre>
 */
@Environment(EnvType.CLIENT)
public final class RetroParticleRegistry {

	/**
	 * Builds one particle instance. Coordinates are world space; the velocity is in blocks per tick and
	 * is whatever the spawn call passed. Return null to spawn nothing.
	 */
	@FunctionalInterface
	public interface Factory {
		Particle create(World world, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ);
	}

	private static final Map<String, Factory> FACTORIES = new HashMap<>();

	private RetroParticleRegistry() {}

	/** Registers a particle factory under a namespaced id. Re-registering the same id replaces it. */
	public static void register(NamespacedIdentifier id, Factory factory) {
		if (id == null || factory == null) {
			throw new IllegalArgumentException("particle id and factory must not be null");
		}
		FACTORIES.put(id.toString(), factory);
		RetroAPI.LOGGER.debug("Registered particle {}", id);
	}

	/** The factory for a particle name, or null when it is not a RetroAPI particle. */
	public static Factory get(String name) {
		// Fast path: vanilla names have no namespace separator, so they never touch the map.
		if (name == null || name.indexOf(':') < 0) {
			return null;
		}
		return FACTORIES.get(name);
	}

	/** True when this name resolves to a registered RetroAPI particle. */
	public static boolean isRegistered(String name) {
		return get(name) != null;
	}
}
