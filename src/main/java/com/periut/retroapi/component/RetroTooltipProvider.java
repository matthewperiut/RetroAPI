package com.periut.retroapi.component;

import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Implemented by an Item that wants extra tooltip lines, the beta port of modern's
 * {@code Item.appendHoverText}. Beta has NO tooltip system at all (the inventory shows
 * only the item's name on hover), so RetroAPI both adds the multi-line tooltip and reads
 * the lines from here. Add as many strings as you like; they render under the name.
 *
 * <pre>{@code
 * public class CounterItem extends Item implements RetroTooltipProvider {
 *     public void appendTooltip(ItemStack stack, List<String> lines) {
 *         lines.add("Clicks: " + RetroComponents.get(stack, CLICK_COUNT));
 *     }
 * }
 * }</pre>
 */
public interface RetroTooltipProvider {

	void appendTooltip(ItemStack stack, List<String> lines);
}
