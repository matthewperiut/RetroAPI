package com.periut.retroapi.achievement.event;

import net.ornithemc.osl.core.api.events.Event;

/**
 * OSL-style registration hook fired from {@code RetroAPI.init()} immediately after
 * blocks and items have been registered (inside the {@code if (!hasStationAPI)} branch),
 * so achievement icons can reference already-registered modded blocks/items.
 *
 * <p>Mirrors {@link com.periut.retroapi.register.block.event.BlockRegistrationCallback}.
 */
public class AchievementRegistrationCallback {
	public static final Event<Runnable> EVENT = Event.runnable();
}
