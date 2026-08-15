package com.periut.retrotweaks.feature.inventory;

import com.periut.retrotweaks.mixin.client.MinecraftAccessor;
import com.periut.retrotweaks.mixin.screen.SlotAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.List;

/**
 * Ported from Glass Inventory Tweaks' {@code impl.VanillaClickImpl} - the client-only shift-click and
 * scroll-wheel path that needs no server-side companion mod, since it drives ordinary
 * {@code interactionManager.clickSlot} calls instead of Glass Inventory Tweaks' StationAPI packet
 * ({@code impl.ModdedClickImpl}/{@code impl.InventoryTweaksPacket}). That networked path is the
 * excluded public API surface (see {@code PORT_STATUS.md}) and is not ported.
 */
public final class PriorityShiftClick {

	private PriorityShiftClick() {}

	/** Ported from {@code VanillaClickImpl.shiftClick(Slot, ScreenHandler)}. */
	public static boolean shiftClick(Slot shiftClickedSlot, ScreenHandler handler) {
		if (shiftClickedSlot == null || shiftClickedSlot.getStack() == null) return false;

		Minecraft mc = MinecraftAccessor.retrotweaks$getInstance();

		List<SlotPriority.Group> groups = SlotPriority.getGroups(handler);
		SlotPriority.Group originalGroup = SlotPriority.groupOf(groups, shiftClickedSlot);
		if (originalGroup == null) return false;

		boolean stashedCursorStack = mc.player.inventory.getCursorStack() != null;

		mc.interactionManager.clickSlot(handler.syncId, shiftClickedSlot.id, 0, false, mc.player);

		boolean result = shiftClickCursorStack(handler, originalGroup);

		// Put the stack back if nothing accepted it.
		if (mc.player.inventory.getCursorStack() != null || stashedCursorStack) {
			mc.interactionManager.clickSlot(handler.syncId, shiftClickedSlot.id, 0, false, mc.player);
		}
		return result;
	}

	/** Ported from {@code VanillaClickImpl.shiftClickCursorStack(ScreenHandler, SlotData)}. */
	public static boolean shiftClickCursorStack(ScreenHandler handler, SlotPriority.Group originalGroup) {
		Minecraft mc = MinecraftAccessor.retrotweaks$getInstance();
		ItemStack stackToMove = mc.player.inventory.getCursorStack();

		if (handler.slots.isEmpty() || stackToMove == null) return false;

		List<SlotPriority.Group> groups = SlotPriority.getGroups(handler);

		for (SlotPriority.Group group : groups) {
			if (group == originalGroup || !SlotPriority.isShiftClickDestination(group)) continue;

			for (Slot potentialSlot : group.slots) {
				ItemStack mergeStack = potentialSlot.getStack();

				if (potentialSlot.canInsert(stackToMove)
					&& (mergeStack == null || (mergeStack.isItemEqual(stackToMove)
						&& mergeStack.count < potentialSlot.getMaxItemCount()
						&& mergeStack.count < mergeStack.getItem().getMaxCount()))) {

					mc.interactionManager.clickSlot(handler.syncId, potentialSlot.id, 0, false, mc.player);

					if (mc.player.inventory.getCursorStack() == null) return true;
				}
			}
		}
		return true;
	}

	/**
	 * Ported from {@code VanillaClickImpl.handleScroll(HandledScreen, float)}. The caller already
	 * resolved the hovered slot (RetroTweaks tracks it for its own scroll feature), so this skips Glass
	 * Inventory Tweaks' own mouse-position lookup and takes the slot directly.
	 */
	public static boolean handleScroll(ScreenHandler handler, Slot shiftClickedSlot) {
		if (shiftClickedSlot == null) return false;

		Minecraft mc = MinecraftAccessor.retrotweaks$getInstance();
		if (mc.player.inventory.getCursorStack() != null) return false;

		// Sorry pal, minecraft code limitations: only peel from a slot that is part of the player's
		// own inventory, matching Glass Inventory Tweaks' own restriction.
		Inventory shiftClickedInventory = ((SlotAccessor) shiftClickedSlot).retrotweaks$getInventory();
		if (shiftClickedInventory != mc.player.inventory) return false;

		List<SlotPriority.Group> groups = SlotPriority.getGroups(handler);
		SlotPriority.Group originalGroup = SlotPriority.groupOf(groups, shiftClickedSlot);
		if (originalGroup == null) return false;

		Slot holdMyStackASec = null;
		for (Slot potentialEmptySlot : handler.slots) {
			if (SlotPriority.groupOf(groups, potentialEmptySlot) == originalGroup && potentialEmptySlot.getStack() == null) {
				holdMyStackASec = potentialEmptySlot;
				break;
			}
		}

		if (holdMyStackASec == null) return false;

		mc.interactionManager.clickSlot(handler.syncId, shiftClickedSlot.id, 0, false, mc.player);
		mc.interactionManager.clickSlot(handler.syncId, holdMyStackASec.id, 1, false, mc.player);
		mc.interactionManager.clickSlot(handler.syncId, shiftClickedSlot.id, 0, false, mc.player);
		mc.interactionManager.clickSlot(handler.syncId, holdMyStackASec.id, 0, false, mc.player);
		shiftClickCursorStack(handler, originalGroup);
		return true;
	}
}
