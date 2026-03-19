package com.periut.retroapi.testmod;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class CrateBlockEntity extends BlockEntity implements Inventory {
	private final ItemStack[] items = new ItemStack[27];
	public int openCount;

	@Override
	public int getSize() {
		return 27;
	}

	@Override
	public ItemStack getItem(int slot) {
		return items[slot];
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		if (items[slot] != null) {
			if (items[slot].size <= amount) {
				ItemStack stack = items[slot];
				items[slot] = null;
				markDirty();
				return stack;
			}
			ItemStack split = items[slot].split(amount);
			if (items[slot].size == 0) {
				items[slot] = null;
			}
			markDirty();
			return split;
		}
		return null;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		items[slot] = stack;
		if (stack != null && stack.size > getMaxStackSize()) {
			stack.size = getMaxStackSize();
		}
		markDirty();
	}

	@Override
	public String getInventoryName() {
		return "Crate";
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public boolean isValid(PlayerEntity player) {
		if (world.getBlockEntity(x, y, z) != this) {
			return false;
		}
		return player.distanceTo(x + 0.5, y + 0.5, z + 0.5) <= 64.0;
	}

	@Override
	public void markDirty() {
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		NbtList list = nbt.getList("Items");
		for (int i = 0; i < items.length; i++) {
			items[i] = null;
		}
		for (int i = 0; i < list.size(); i++) {
			NbtCompound itemNbt = (NbtCompound) list.get(i);
			int slot = itemNbt.getByte("Slot") & 0xFF;
			if (slot < items.length) {
				items[slot] = new ItemStack(itemNbt);
			}
		}
	}

	@Override
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		NbtList list = new NbtList();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				NbtCompound itemNbt = new NbtCompound();
				itemNbt.putByte("Slot", (byte) i);
				items[i].writeNbt(itemNbt);
				list.addElement(itemNbt);
			}
		}
		nbt.put("Items", list);
	}
}
