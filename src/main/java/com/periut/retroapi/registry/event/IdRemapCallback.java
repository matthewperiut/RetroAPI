package com.periut.retroapi.registry.event;

import com.periut.retroapi.registry.IdRemap;
import net.ornithemc.osl.core.api.events.Event;

/**
 * Fired after RetroAPI has moved blocks/items to the ids a world (or a server) says they must have, and
 * after it has repaired its own recipe/smelting/fuel/achievement tables.
 *
 * <p>Register a listener if your mod caches an {@link net.minecraft.item.ItemStack} - or a raw numeric id -
 * anywhere that outlives registration:
 *
 * <pre>
 * IdRemapCallback.EVENT.register(remap -&gt; {
 *     remap.fix(MyMod.STARTER_KIT);        // an ItemStack field
 *     MyMod.magicItemId = remap.map(MyMod.magicItemId);   // a raw id
 * });
 * </pre>
 *
 * <p>Fires at most once per world open / server join, and only when something actually moved.
 */
public final class IdRemapCallback {

	@FunctionalInterface
	public interface Listener {
		void onIdsRemapped(IdRemap remap);
	}

	public static final Event<Listener> EVENT = Event.of(listeners -> remap -> {
		for (Listener listener : listeners) {
			listener.onIdsRemapped(remap);
		}
	});

	private IdRemapCallback() {}
}
