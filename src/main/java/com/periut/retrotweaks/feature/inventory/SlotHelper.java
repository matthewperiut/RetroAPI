package com.periut.retrotweaks.feature.inventory;

import com.periut.retrotweaks.compat.ApiBridge;
import com.periut.retrotweaks.compat.Mods;
import com.periut.retrotweaks.mixin.recipe.FurnaceFuelAccessor;

import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.SmeltingRecipeManager;
import net.minecraft.screen.slot.Slot;

/**
 * Slot questions the inventory tweaks need to answer, plus the two furnace lookups.
 *
 * <p>InventoryTweaks asked StationAPI whether a stack could be smelted or burned, and skipped the
 * feature entirely when StationAPI was absent. Vanilla can answer both questions itself, so the
 * feature now works on a bare install - and it picks up RetroTweaks' own extra fuels for free,
 * because it asks the furnace rather than a separate table.
 */
public final class SlotHelper {

	private SlotHelper() {}

	/** Crates adds container slots that behave differently; the tweaks check for it. */
	public static final boolean HAS_CRATES = Mods.isLoaded("crate");

	/** True when the slot holds a stack of the same item and damage. */
	public static boolean isItemInSlot(ItemStack stack, Slot slot) {
		ItemStack inSlot = slot.getStack();
		return inSlot != null && stack.isItemEqual(inSlot);
	}

	/**
	 * How the stack would fit: {@code 1} empty slot, {@code 0} same item with room,
	 * {@code -1} no room.
	 */
	public static int canItemFitInSlot(ItemStack stack, Slot slot) {
		ItemStack inSlot = slot.getStack();
		if (inSlot == null) return 1;
		if (!stack.isItemEqual(inSlot)) return -1;
		return inSlot.count == inSlot.getMaxCount() ? -1 : 0;
	}

	/**
	 * What this stack smelts into, or null if it is not a smelting input. StationAPI is asked first
	 * when present, because recipes registered through it never reach vanilla's table.
	 */
	public static ItemStack getSmeltingResult(ItemStack stack) {
		if (stack == null) return null;
		ItemStack fromApi = ApiBridge.smeltingResult(stack);
		if (fromApi != null) return fromApi;
		return SmeltingRecipeManager.getInstance().craft(stack.itemId);
	}

	/**
	 * How long this stack burns for, or 0 if it is not fuel. An installed API is asked first (it
	 * knows about fuels vanilla has never heard of); otherwise the question goes to the furnace
	 * itself, so anything mixed into its table - RetroTweaks' own extra wooden items included - counts.
	 */
	public static int getFuelTime(ItemStack stack) {
		if (stack == null) return 0;
		int fromApi = ApiBridge.fuelTime(stack);
		if (fromApi >= 0) return fromApi;
		return ((FurnaceFuelAccessor) FUEL_PROBE).retrotweaks$getFuelTime(stack);
	}

	/**
	 * A furnace that is never placed in a world, used only to reach the fuel table. Vanilla's
	 * {@code getFuelTime} is an instance method but reads no instance state.
	 */
	private static final FurnaceBlockEntity FUEL_PROBE = new FurnaceBlockEntity();
}
