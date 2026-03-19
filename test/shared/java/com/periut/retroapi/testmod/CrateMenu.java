package com.periut.retroapi.testmod;

import com.periut.retroapi.register.blockentity.RetroMenu;
import com.periut.retroapi.register.blockentity.SyncField;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.inventory.slot.InventorySlot;

public class CrateMenu extends RetroMenu {
	@SyncField("openCount")
	public int openCount;

	public CrateMenu(PlayerInventory playerInv, CrateBlockEntity crate) {
		super(crate);

		// Crate inventory slots (3 rows of 9)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new InventorySlot(crate, col + row * 9, 8 + col * 18, 18 + row * 18));
			}
		}

		// Player inventory (3 rows of 9)
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
}
