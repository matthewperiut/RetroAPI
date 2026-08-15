package com.periut.retroapi.achievement.event;

import net.ornithemc.osl.core.api.events.Event;

/**
 * Registration hook for modded achievements, fired once blocks and items exist so an achievement's
 * icon can reference already-registered content.
 *
 * <p>Mirrors {@link com.periut.retroapi.register.block.event.BlockRegistrationCallback}, with one
 * difference that matters: <b>a listener added after the event has already fired runs immediately.</b>
 *
 * <p>That is not a nicety. Without StationAPI, {@code RetroAPI.init} fires this itself, after the
 * {@code retroapi} entrypoint has given every mod its chance to listen - so the order is fixed and
 * obvious. With StationAPI, registration belongs to StationAPI and this is fired from ITS achievement
 * event instead ({@code StationAPIRegistryForwarder}), whose position relative to RetroAPI's own init
 * is decided by two mod loaders' entrypoint ordering and is not ours to assume. Lose that race and the
 * old behaviour was silent: every mod's achievements simply never registered, on exactly the installs
 * that had StationAPI. Replaying for late listeners makes the outcome the same whichever order the two
 * happen to run in.
 *
 * <p>Registration therefore happens exactly once per listener, either way.
 */
public class AchievementRegistrationCallback {
	public static final Event<Runnable> EVENT = Event.runnable();

	/** Set by {@link #fire()}; from then on {@link #register} runs its listener on the spot. */
	private static boolean fired;

	/**
	 * Adds a listener, or runs it immediately when the event has already been fired.
	 *
	 * <p>Prefer this over {@code EVENT.register}, which cannot know it is too late.
	 */
	public static void register(final Runnable listener) {
		if (listener == null) {
			return;
		}
		if (fired) {
			listener.run();
			return;
		}
		EVENT.register(listener);
	}

	/** Runs every listener, and marks the event as past for anything registering later. */
	public static void fire() {
		fired = true;
		EVENT.invoker().run();
	}

	/** True once achievements have been registered this launch. */
	public static boolean hasFired() {
		return fired;
	}
}
