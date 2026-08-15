package com.periut.retrotweaks.feature.inventory;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.mixin.client.MinecraftAccessor;
import com.periut.retrotweaks.mixin.screen.SlotAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from MouseTweaks' (YaLTeR) {@code Main.onMouseScrolled} - the wheel tweak, which moves items
 * between the two inventories on screen instead of between the cursor and the slot under it. Scroll
 * over a chest slot and its items walk into your inventory a notch at a time, and the other way round.
 *
 * <p>Everything here goes through {@code interactionManager.clickSlot}, the same clicks a player could
 * have made by hand, so unlike RetroTweaks' older cursor/slot scroll - which edits stacks directly -
 * this works on a server as well as in singleplayer.
 *
 * <p>Two of MouseTweaks' cases are deliberately not ported. Scrolling while <em>holding</em> a stack
 * leans on several subtle click semantics to swap items around without dropping any, so this bows out
 * and leaves that case to the cursor/slot scroll below it. And the crafting output slot is skipped
 * rather than special-cased; the crafting result shortcuts already cover crafting in bulk.
 */
public final class WheelTweak {

	private WheelTweak() {}

	private static final int LEFT = 0;
	private static final int RIGHT = 1;

	/**
	 * @param wheelDegrees raw {@code Mouse.getDWheel()}, 120 per notch.
	 * @return true when the wheel tweak took the scroll, whether or not anything moved. Hovering a
	 *         slot it could act on but finding nowhere to put the items still counts as handled, so
	 *         the scroll does not silently fall through to a different feature once a chest fills up.
	 */
	public static boolean handleScroll(ScreenHandler handler, Slot hoveredSlot, int wheelDegrees) {
		if (hoveredSlot == null) return false;

		Minecraft mc = MinecraftAccessor.retrotweaks$getInstance();
		if (mc.player.inventory.getCursorStack() != null) return false;

		ItemStack hoveredStack = hoveredSlot.getStack();
		if (hoveredStack == null) return false;

		/** - A slot that will not take the item back - a crafting or furnace output - cannot be worked
		 *    a notch at a time: clicking it with a matching stack held empties it onto the cursor
		 *    instead of topping it up, so it is left to [Shift-Click]. */
		if (!hoveredSlot.canInsert(hoveredStack)) return false;

		int notches = wheelDegrees / 120;
		if (notches == 0) notches = Integer.signum(wheelDegrees);
		if (notches == 0) return false;

		List<Slot> slots = handler.slots;

		boolean pushItems = notches < 0;
		Enums.WheelDirection direction = Config.INVENTORY.scroll.transferScrollDirection;
		if (direction.isPositionAware() && otherInventoryIsAbove(mc, hoveredSlot, slots)) pushItems = !pushItems;
		if (direction.isInverted()) pushItems = !pushItems;

		int itemsToMove = Math.abs(notches);
		if (pushItems) {
			push(mc, handler, slots, hoveredSlot, hoveredStack, itemsToMove);
		} else {
			pull(mc, handler, slots, hoveredSlot, hoveredStack, itemsToMove);
		}
		return true;
	}

	/** Send items out of the hovered slot and into the other inventory. */
	private static void push(Minecraft mc, ScreenHandler handler, List<Slot> slots, Slot hoveredSlot, ItemStack hoveredStack, int itemsToMove) {
		itemsToMove = Math.min(itemsToMove, hoveredStack.count);

		List<Slot> targets = findPushSlots(mc, slots, hoveredSlot, hoveredStack);
		if (targets.isEmpty()) return;

		// Right-click picks up half the stack, which leaves a furnace input mid-smelt alone; left-click
		// takes the lot, and is only reached for when the lot is what is being moved anyway.
		click(mc, handler, hoveredSlot, hoveredStack.count <= itemsToMove ? LEFT : RIGHT);

		ItemStack picked = mc.player.inventory.getCursorStack();
		if (picked == null) return;
		itemsToMove = Math.min(itemsToMove, picked.count);

		for (Slot target : targets) {
			if (itemsToMove <= 0) break;
			int clicks = Math.min(maxCount(target, picked) - count(target), itemsToMove);
			itemsToMove -= clicks;
			while (clicks-- > 0) click(mc, handler, target, RIGHT);
		}

		// Whatever was picked up but not distributed goes back where it came from.
		if (mc.player.inventory.getCursorStack() != null) click(mc, handler, hoveredSlot, LEFT);
	}

	/** Draw items into the hovered slot from the other inventory. */
	private static void pull(Minecraft mc, ScreenHandler handler, List<Slot> slots, Slot hoveredSlot, ItemStack hoveredStack, int itemsToMove) {
		itemsToMove = Math.min(itemsToMove, maxCount(hoveredSlot, hoveredStack) - hoveredStack.count);

		while (itemsToMove > 0) {
			Slot source = findPullSlot(mc, slots, hoveredSlot, hoveredStack);
			if (source == null) return;

			ItemStack sourceStack = source.getStack();
			click(mc, handler, source, sourceStack.count == 1 ? LEFT : RIGHT);

			ItemStack picked = mc.player.inventory.getCursorStack();
			if (picked == null) return;

			// Always at least one, so the loop cannot spin on a source it fails to drain.
			int moving = Math.min(picked.count, itemsToMove);
			if (moving == picked.count) {
				click(mc, handler, hoveredSlot, LEFT);
			} else {
				for (int i = 0; i < moving; i++) click(mc, handler, hoveredSlot, RIGHT);
			}
			itemsToMove -= moving;

			// Half a stack was picked up to move one item; the rest goes back.
			if (mc.player.inventory.getCursorStack() != null) click(mc, handler, source, LEFT);
		}
	}

	/**
	 * Slots in the other inventory that can take this stack: partly filled ones first, in screen
	 * order, then empty ones, matching MouseTweaks' preference for topping stacks up over spreading.
	 */
	private static List<Slot> findPushSlots(Minecraft mc, List<Slot> slots, Slot hoveredSlot, ItemStack hoveredStack) {
		boolean wantPlayerInventory = isOtherInventoryThePlayers(mc, hoveredSlot);

		List<Slot> partial = new ArrayList<>();
		List<Slot> empty = new ArrayList<>();

		for (Slot slot : slots) {
			if (slot == hoveredSlot || slot instanceof CraftingResultSlot) continue;
			if (isPlayerInventory(mc, slot) != wantPlayerInventory) continue;

			if (!slot.canInsert(hoveredStack)) continue;

			ItemStack stack = slot.getStack();
			if (stack == null) {
				empty.add(slot);
			} else if (stack.isItemEqual(hoveredStack) && stack.count < maxCount(slot, stack)) {
				partial.add(slot);
			}
		}

		partial.addAll(empty);
		return partial;
	}

	/** The slot in the other inventory to take items from, in the configured search order. */
	private static Slot findPullSlot(Minecraft mc, List<Slot> slots, Slot hoveredSlot, ItemStack hoveredStack) {
		boolean wantPlayerInventory = isOtherInventoryThePlayers(mc, hoveredSlot);
		boolean firstToLast = Config.INVENTORY.scroll.transferScrollSearchOrder == Enums.WheelSearchOrder.FIRST_TO_LAST;

		for (int i = 0; i < slots.size(); i++) {
			Slot slot = slots.get(firstToLast ? i : slots.size() - 1 - i);
			if (slot == hoveredSlot || slot instanceof CraftingResultSlot) continue;
			if (isPlayerInventory(mc, slot) != wantPlayerInventory) continue;

			ItemStack stack = slot.getStack();
			if (stack != null && stack.isItemEqual(hoveredStack)) return slot;
		}

		return null;
	}

	/**
	 * Whether the other inventory is drawn above the hovered slot, decided by which side holds more of
	 * its slots. Only the position aware scroll directions ask.
	 */
	private static boolean otherInventoryIsAbove(Minecraft mc, Slot hoveredSlot, List<Slot> slots) {
		boolean wantPlayerInventory = isOtherInventoryThePlayers(mc, hoveredSlot);
		int above = 0;
		int below = 0;

		for (Slot slot : slots) {
			if (isPlayerInventory(mc, slot) != wantPlayerInventory) continue;
			if (slot.y < hoveredSlot.y) above++;
			else below++;
		}

		return above > below;
	}

	/**
	 * The screen's other inventory is the player's whenever the hovered slot is not, which is what makes
	 * a scroll over a chest slot walk items down into the player's inventory. In the player's own screen
	 * both sides are the player, so "the other inventory" resolves to the 2x2 crafting grid.
	 */
	private static boolean isOtherInventoryThePlayers(Minecraft mc, Slot hoveredSlot) {
		return !isPlayerInventory(mc, hoveredSlot);
	}

	private static boolean isPlayerInventory(Minecraft mc, Slot slot) {
		Inventory inventory = ((SlotAccessor) slot).retrotweaks$getInventory();
		return inventory == mc.player.inventory;
	}

	/** How many of this stack the slot will hold, whichever of the two caps is lower. */
	private static int maxCount(Slot slot, ItemStack stack) {
		return Math.min(slot.getMaxItemCount(), stack.getMaxCount());
	}

	private static int count(Slot slot) {
		ItemStack stack = slot.getStack();
		return stack == null ? 0 : stack.count;
	}

	private static void click(Minecraft mc, ScreenHandler handler, Slot slot, int button) {
		mc.interactionManager.clickSlot(handler.syncId, slot.id, button, false, mc.player);
	}
}
