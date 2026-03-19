package com.periut.retroapi.testmod;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class FreezerBlockEntity extends BlockEntity implements Inventory {
	private final ItemStack[] inventory = new ItemStack[3]; // 0=input, 1=fuel, 2=output
	public int cookTime;
	public int fuelTime;
	public int totalFuelTime;

	@Override
	public int getSize() {
		return 3;
	}

	@Override
	public ItemStack getItem(int slot) {
		return inventory[slot];
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		if (inventory[slot] != null) {
			if (inventory[slot].size <= amount) {
				ItemStack stack = inventory[slot];
				inventory[slot] = null;
				return stack;
			}
			ItemStack split = inventory[slot].split(amount);
			if (inventory[slot].size == 0) {
				inventory[slot] = null;
			}
			return split;
		}
		return null;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		inventory[slot] = stack;
		if (stack != null && stack.size > getMaxStackSize()) {
			stack.size = getMaxStackSize();
		}
	}

	@Override
	public String getInventoryName() {
		return "Freezer";
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
	public void tick() {
		boolean wasLit = fuelTime > 0;
		boolean changed = false;

		if (fuelTime > 0) {
			fuelTime--;
		}

		if (!world.isMultiplayer) {
			// Try to consume fuel if needed and we can cook
			if (fuelTime == 0 && canFreeze()) {
				totalFuelTime = fuelTime = getFuelTime(inventory[1]);
				if (fuelTime > 0) {
					changed = true;
					if (inventory[1] != null) {
						inventory[1].size--;
						if (inventory[1].size == 0) {
							inventory[1] = null;
						}
					}
				}
			}

			// Cook progress
			if (fuelTime > 0 && canFreeze()) {
				cookTime++;
				if (cookTime >= 200) {
					cookTime = 0;
					finishFreezing();
					changed = true;
				}
			} else {
				cookTime = 0;
			}
		}

		if (changed) {
			markDirty();
		}
	}

	private boolean canFreeze() {
		if (inventory[0] == null) return false;
		// Only freeze water buckets
		if (inventory[0].getItem().id != Item.WATER_BUCKET.id) return false;
		// Check output slot
		if (inventory[2] == null) return true;
		if (inventory[2].getItem().id != Block.ICE.id) return false;
		return inventory[2].size < getMaxStackSize() && inventory[2].size < inventory[2].getMaxSize();
	}

	private void finishFreezing() {
		if (!canFreeze()) return;

		// Place ice in output
		if (inventory[2] == null) {
			inventory[2] = new ItemStack(Block.ICE, 1);
		} else {
			inventory[2].size++;
		}

		// Consume water bucket, leave empty bucket
		inventory[0].size--;
		if (inventory[0].size <= 0) {
			inventory[0] = new ItemStack(Item.BUCKET, 1);
		}
	}

	private int getFuelTime(ItemStack stack) {
		if (stack == null) return 0;
		int id = stack.getItem().id;
		if (id < 256 && Block.BY_ID[id] != null && Block.BY_ID[id].material == Material.WOOD) return 300;
		if (id == Item.STICK.id) return 100;
		if (id == Item.COAL.id) return 1600;
		if (id == Item.LAVA_BUCKET.id) return 20000;
		if (id == Block.SAPLING.id) return 100;
		return 0;
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		NbtList list = nbt.getList("Items");
		for (int i = 0; i < inventory.length; i++) {
			inventory[i] = null;
		}
		for (int i = 0; i < list.size(); i++) {
			NbtCompound itemNbt = (NbtCompound) list.get(i);
			int slot = itemNbt.getByte("Slot") & 0xFF;
			if (slot < inventory.length) {
				inventory[slot] = new ItemStack(itemNbt);
			}
		}
		cookTime = nbt.getShort("CookTime");
		fuelTime = nbt.getShort("FuelTime");
		totalFuelTime = nbt.getShort("TotalFuelTime");
	}

	@Override
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		NbtList list = new NbtList();
		for (int i = 0; i < inventory.length; i++) {
			if (inventory[i] != null) {
				NbtCompound itemNbt = new NbtCompound();
				itemNbt.putByte("Slot", (byte) i);
				inventory[i].writeNbt(itemNbt);
				list.addElement(itemNbt);
			}
		}
		nbt.put("Items", list);
		nbt.putShort("CookTime", (short) cookTime);
		nbt.putShort("FuelTime", (short) fuelTime);
		nbt.putShort("TotalFuelTime", (short) totalFuelTime);
	}
}
