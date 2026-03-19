package com.periut.retroapi.testmod;

import com.periut.retroapi.register.blockentity.RetroMenu;
import com.periut.retroapi.register.blockentity.SyncField;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.inventory.slot.InventorySlot;
import net.minecraft.item.ItemStack;

public class FreezerMenu extends RetroMenu {
	// Synced in this order to match FurnaceMenu's data IDs (0, 1, 2)
	@SyncField("cookTime")
	public int cookTime;

	@SyncField("fuelTime")
	public int fuelTime;

	@SyncField("totalFuelTime")
	public int totalFuelTime;

	public FreezerMenu(PlayerInventory playerInv, FreezerBlockEntity freezer) {
		super(freezer);

		// Slot 0: input (water bucket)
		addSlot(new InventorySlot(freezer, 0, 56, 17));
		// Slot 1: fuel
		addSlot(new InventorySlot(freezer, 1, 56, 53));
		// Slot 2: result (ice)
		addSlot(new InventorySlot(freezer, 2, 116, 35));

		// Player inventory (3 rows)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new InventorySlot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
			}
		}

		// Player hotbar
		for (int col = 0; col < 9; col++) {
			addSlot(new InventorySlot(playerInv, col, 8 + col * 18, 142));
		}
	}

	@Override
	public boolean isValid(PlayerEntity player) {
		return true;
	}

	public ItemStack quickMoveItem(int slotIndex) {
		return null;
	}
}
