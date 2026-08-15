package com.periut.retrotweaks.feature.inventory;

import com.periut.retrotweaks.mixin.screen.SlotAccessor;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Ported from Glass Inventory Tweaks' {@code impl.SlotHelper}/{@code impl.SlotData}. Groups a screen
 * handler's slots by their backing {@link Inventory}, in the order Minecraft added them, then sorts
 * the groups by priority so a shift-clicked stack can be routed to the best one first.
 *
 * <p>Glass Inventory Tweaks let any mod annotate its own {@code ScreenHandler} with
 * {@code @ShiftClickTreatSeparately}/{@code @ShouldNotShiftClick} to customise this grouping - that
 * public API surface is a deliberate exclusion (see {@code PORT_STATUS.md}). The two places Glass
 * Inventory Tweaks itself used those annotations - splitting {@link PlayerScreenHandler}'s
 * armor/main/hotbar sections and excluding the 2x2 crafting grid from being a shift-click destination
 * - are kept below as the fixed cases they always resolved to, instead of a reusable annotation.
 */
public final class SlotPriority {

	private SlotPriority() {}

	private static final WeakHashMap<ScreenHandler, List<Group>> CACHE = new WeakHashMap<>();

	public static final class Group {
		public final Inventory inventory;
		public final int priority;
		public final List<Slot> slots = new ArrayList<>();

		private Group(Inventory inventory, int priority) {
			this.inventory = inventory;
			this.priority = priority;
		}
	}

	public static List<Group> getGroups(ScreenHandler handler) {
		return CACHE.computeIfAbsent(handler, SlotPriority::buildGroups);
	}

	public static Group groupOf(List<Group> groups, Slot slot) {
		for (Group group : groups) {
			if (group.slots.contains(slot)) return group;
		}
		return null;
	}

	/** Glass Inventory Tweaks' {@code CraftingInventoryMixin} marked the 2x2 grid @ShouldNotShiftClick. */
	public static boolean isShiftClickDestination(Group group) {
		return !(group.inventory instanceof CraftingInventory);
	}

	// PlayerScreenHandlerMixin's @ShiftClickTreatSeparately(value = {8, 35, 46},
	// sectionPreference = {0, 0, 4, 2, 1, 3}) for craftoutput, craftinput, armor, main, hotbar.
	private static final int[] PLAYER_SPLIT_POINTS = {8, 35, 46};
	private static final int[] PLAYER_SECTION_PREFERENCE = {0, 0, 4, 2, 1, 3};

	private static List<Group> buildGroups(ScreenHandler handler) {
		List<Group> groups = new ArrayList<>();
		int groupIndex = 0;
		Inventory lastInv = null;

		boolean isPlayerHandler = handler instanceof PlayerScreenHandler;
		int[] splitPoints = isPlayerHandler ? PLAYER_SPLIT_POINTS : null;
		int nextSplitPointIndex = 0;

		List<Slot> allSlots = handler.slots;
		for (Slot slot : allSlots) {
			Inventory currentInv = ((SlotAccessor) slot).retrotweaks$getInventory();

			if (currentInv != lastInv) {
				if (!groups.isEmpty()) {
					if (groups.get(groupIndex).slots.isEmpty()) {
						groups.remove(groupIndex);
					} else {
						groupIndex++;
					}
				}

				while (groups.size() <= groupIndex) {
					groups.add(newGroup(currentInv, groups.size(), isPlayerHandler));
				}
			}

			if (splitPoints != null && nextSplitPointIndex < splitPoints.length && slot.id == splitPoints[nextSplitPointIndex]) {
				groups.get(groupIndex).slots.add(slot);

				nextSplitPointIndex++;
				groupIndex++;
				while (groups.size() <= groupIndex) {
					groups.add(newGroup(currentInv, groups.size(), isPlayerHandler));
				}

				lastInv = currentInv;
				continue;
			}

			groups.get(groupIndex).slots.add(slot);
			lastInv = currentInv;
		}

		groups.sort(Comparator.comparingInt(g -> -g.priority));
		return groups;
	}

	private static Group newGroup(Inventory inventory, int index, boolean isPlayerHandler) {
		int priority = isPlayerHandler && index < PLAYER_SECTION_PREFERENCE.length ? PLAYER_SECTION_PREFERENCE[index] : index;
		return new Group(inventory, priority);
	}
}
