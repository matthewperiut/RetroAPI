package com.periut.retroapi.register.recipe.event;

import net.ornithemc.osl.core.api.events.Event;

/**
 * Fired during {@code RetroAPI.init()} (inside the non-StationAPI branch) so mods may
 * register crafting/smelting recipes. Mirrors {@code BlockRegistrationCallback} /
 * {@code ItemRegistrationCallback}.
 *
 * <p>Listeners typically call {@link com.periut.retroapi.register.recipe.RetroRecipes}
 * (or the StationAPI-compatible {@code CraftingRegistry} facade). After all listeners
 * run, RetroAPI re-sorts the crafting recipe list so shaped recipes correctly take
 * precedence over shapeless ones (see {@code RetroRecipes.sortCraftingRecipes()}).</p>
 *
 * <p>When StationAPI is present this callback is bridged from StationAPI's own
 * {@code RecipeRegisterEvent} via {@code StationAPIRegistryForwarder}.</p>
 */
public class RecipeRegistrationCallback {
	public static final Event<Runnable> EVENT = Event.runnable();
}
