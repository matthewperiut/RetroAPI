package com.periut.retroapi.register.item.event;

import net.ornithemc.osl.core.api.events.Event;

import java.util.function.Consumer;

public class ItemRegistrationCallback {
	public static final Event<Runnable> EVENT = Event.runnable();
}
