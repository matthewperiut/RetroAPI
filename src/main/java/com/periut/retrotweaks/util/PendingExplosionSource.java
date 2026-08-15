package com.periut.retrotweaks.util;

import net.minecraft.entity.Entity;

/**
 * Hands the about-to-explode entity to the {@code Explosion} it is about to create, for the two
 * vanilla call sites that build one with {@code source = null}: {@code TntEntity.explode()} and
 * {@code FireballEntity.tick()}. Ghast fireballs and TNT never pass themselves as
 * {@code Explosion.source} the way {@code CreeperEntity.attack()} does, so
 * {@code Explosion.source instanceof TntEntity} / {@code instanceof FireballEntity} can never be
 * true - see {@code ExplosionMixin}.
 *
 * <p>This is a single slot, not a counter: the mixin that owns the about-to-explode entity sets it
 * immediately before the single, synchronous call to {@code World.createExplosion(...)}, and
 * {@code Explosion}'s own constructor - which runs before that call returns, and before any block or
 * entity is touched - reads and clears it straight back to null. Nothing in b1.7.3's explosion code
 * calls {@code World.createExplosion} again before that constructor returns (a block an explosion
 * destroys can queue a new primed TNT entity, but never detonates one on the spot), so the slot is
 * never live across two different explosions the way MiscTweaks' static counters were.
 *
 * <p>Per-thread, and that part is not belt-and-braces. In singleplayer the integrated server runs on
 * its own thread while the client keeps ticking, and the client builds explosions of its own -
 * {@code ClientNetworkHandler} constructs {@code new Explosion(world, null, ...)} straight from the
 * server's packet. A plain static field would let the client's constructor consume the slot the
 * server thread had just filled for its own TNT, silently swapping which explosion each toggle
 * applied to. A {@link ThreadLocal} closes that window without a lock, since set and take always
 * happen on the same thread within one call.
 */
public final class PendingExplosionSource {

	private PendingExplosionSource() {}

	private static final ThreadLocal<Entity> PENDING = new ThreadLocal<>();

	public static void set(Entity source) {
		PENDING.set(source);
	}

	public static Entity takeAndClear() {
		Entity source = PENDING.get();
		// remove(), not set(null): the client and server threads live for the whole process, so a
		// lingering entry would pin a dead entity for as long as the game runs.
		PENDING.remove();
		return source;
	}
}
