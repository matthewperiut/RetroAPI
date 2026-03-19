package com.periut.retroapi.client;

import com.periut.retroapi.register.blockentity.RetroMenu;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/**
 * A FurnaceBlockEntity subclass that proxies inventory access to a real block entity
 * and syncs progress fields from the RetroMenu's @SyncField values.
 * Used in singleplayer to display furnace-style GUIs for custom block entities.
 */
class FurnaceBlockEntityProxy extends FurnaceBlockEntity {
	private final Inventory realInventory;
	private final RetroMenu menu;

	FurnaceBlockEntityProxy(Inventory realInventory, RetroMenu menu) {
		this.realInventory = realInventory;
		this.menu = menu;
	}

	private void syncFromBlockEntity() {
		int[] values = menu.readSyncValues();
		if (values.length > 0) this.cookTime = values[0];
		if (values.length > 1) this.fuelTime = values[1];
		if (values.length > 2) this.totalFuelTime = values[2];
	}

	@Override
	public boolean hasFuel() {
		syncFromBlockEntity();
		return super.hasFuel();
	}

	@Override
	public int getLitProgress(int scale) {
		syncFromBlockEntity();
		return super.getLitProgress(scale);
	}

	@Override
	public int getCookProgress(int scale) {
		syncFromBlockEntity();
		return super.getCookProgress(scale);
	}

	@Override
	public void tick() {
		// Don't run furnace logic — the real block entity handles ticking
	}

	// Delegate all Inventory methods to the real block entity

	@Override
	public int getSize() {
		return realInventory.getSize();
	}

	@Override
	public ItemStack getItem(int slot) {
		return realInventory.getItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return realInventory.removeItem(slot, amount);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		realInventory.setItem(slot, stack);
	}

	@Override
	public String getInventoryName() {
		return realInventory.getInventoryName();
	}

	@Override
	public int getMaxStackSize() {
		return realInventory.getMaxStackSize();
	}

	@Override
	public void markDirty() {
		realInventory.markDirty();
	}

	@Override
	public boolean isValid(PlayerEntity player) {
		return realInventory.isValid(player);
	}
}
