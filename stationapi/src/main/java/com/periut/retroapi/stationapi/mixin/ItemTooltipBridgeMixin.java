package com.periut.retroapi.stationapi.mixin;

import com.periut.retroapi.component.RetroTooltips;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.modificationstation.stationapi.api.client.item.CustomTooltipProvider;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

/**
 * StationAPI compat bridge for RetroAPI's multi-line tooltips. Active ONLY when StationAPI is present
 * (gated by {@code RetroAPIMixinPlugin}'s {@code STATIONAPI_ONLY_MIXINS} - references StationAPI's
 * {@code CustomTooltipProvider}, so it must stay off the load path otherwise).
 *
 * <p>Under StationAPI the native {@code client.HandledScreenTooltipMixin} is disabled (StationAPI's
 * {@code station-items-v0 ContainerScreenMixin} cancels the vanilla tooltip draw and replaces it via a
 * {@code TooltipRenderEvent} that {@code CustomTooltipRendererImpl} handles). So instead of drawing over
 * vanilla, this makes every {@link Item} an {@code CustomTooltipProvider} and feeds RetroAPI's extra lines
 * ({@link RetroTooltips#linesFor(ItemStack)}) into StationAPI's own tooltip pipeline.</p>
 *
 * <p>Contract (read off StationAPI's {@code TooltipHelper.getTooltipForItemStack}): when a provider exists,
 * the returned array becomes the ENTIRE tooltip - StationAPI does NOT prepend the item name. So element 0
 * must be {@code originalTooltip} (the name), followed by the extra lines. When RetroAPI has no extra lines
 * we return {@code {originalTooltip}} unchanged, so plain items look exactly as they do in vanilla
 * StationAPI. The interface is {@code @NotNull}, so this never returns null.</p>
 */
@Mixin(Item.class)
public abstract class ItemTooltipBridgeMixin implements CustomTooltipProvider {

	@Override
	public @NotNull String[] getTooltip(ItemStack stack, String originalTooltip) {
		List<String> extra = RetroTooltips.linesFor(stack);
		if (extra.isEmpty()) {
			return new String[] { originalTooltip };
		}
		String[] out = new String[extra.size() + 1];
		out[0] = originalTooltip;
		for (int i = 0; i < extra.size(); i++) {
			out[i + 1] = extra.get(i);
		}
		return out;
	}
}
