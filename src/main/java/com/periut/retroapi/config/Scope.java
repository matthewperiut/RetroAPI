package com.periut.retroapi.config;

/**
 * Whether an option changes the world or only what one player sees.
 *
 * <p>This is what makes joining a server safe. A tweak that changes block placement, drops or
 * recipes only works if the server agrees; running it against a server that has never heard of
 * RetroTweaks means the client predicts one thing and the server does another, and the player sees
 * blocks snap back and items vanish. A tweak that only moves the hotbar or changes how a click is
 * routed has no such problem and should keep working everywhere.
 *
 * <p>{@link #WORLD} is the default precisely because it is the cautious answer: an option nobody
 * has classified stands down on an unknown server rather than desyncing it.
 */
public enum Scope {

	/** Client-side only. Never touched by what server the player is on. */
	CLIENT,

	/**
	 * Changes world or gameplay behaviour, so it must match the server. Forced off on a server that
	 * is not running RetroTweaks, and replaced by the server's own value on one that is.
	 */
	WORLD,

	/** Inherit from the enclosing section - the default for a {@link Cat}, and what an {@link Opt} says
	 *  when it wants its section's answer. The root of the tree counts as {@link #WORLD}. */
	INHERIT
}
