package com.periut.retroapi.register.block.event;

import net.ornithemc.osl.core.api.events.Event;

import java.util.function.Consumer;

public class BlockRegistrationCallback {
	public static final Event<Runnable> EVENT = Event.runnable();
}
