package com.periut.retroapi.component;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The tooltip registry. Two ways to add tooltip lines to an item, mirroring how you can
 * either override a method on a custom item or register a callback for one you do not own:
 *
 * <ul>
 *   <li>have the Item class implement {@link RetroTooltipProvider}, or</li>
 *   <li>register a provider for any item (even vanilla) with {@link #register}.</li>
 * </ul>
 *
 * <p>The client-side tooltip mixin calls {@link #linesFor} for the hovered stack and draws
 * the lines under the name. Lines are plain strings; prefix with section signs for colour.</p>
 */
public final class RetroTooltips {

	private static final Map<Item, RetroTooltipProvider> PROVIDERS = new HashMap<>();

	private RetroTooltips() {
	}

	/** Adds a tooltip provider for an item (works for vanilla items too). */
	public static void register(Item item, RetroTooltipProvider provider) {
		PROVIDERS.put(item, provider);
	}

	/** All extra tooltip lines for a stack: the item's own provider plus any registered one. */
	public static List<String> linesFor(ItemStack stack) {
		List<String> lines = new ArrayList<>();
		if (stack == null) {
			return lines;
		}
		Item item = stack.getItem();
		if (item instanceof RetroTooltipProvider) {
			((RetroTooltipProvider) item).appendTooltip(stack, lines);
		}
		RetroTooltipProvider registered = PROVIDERS.get(item);
		if (registered != null) {
			registered.appendTooltip(stack, lines);
		}
		return lines;
	}
}
