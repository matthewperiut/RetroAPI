package com.periut.retroapi.testmod;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

/**
 * A minimal modded block entity with a 9-slot inventory, serialized in the vanilla {@code Items}
 * format (a list of {@code {Slot, ...item...}} compounds). Registered via
 * {@code RetroBlockEntities.register("retroapi_test:crate", ...)} so it round-trips through chunk NBT,
 * the sidecar (whole-BE strip/restore), and the StationAPI conversion (modded BE injection/extraction).
 */
public class TestCrateBlockEntity extends BlockEntity implements Inventory {
	private final ItemStack[] stacks = new ItemStack[9];

	public TestCrateBlockEntity() {}

	@Override
	public int size() { return stacks.length; }

	@Override
	public ItemStack getStack(int slot) { return slot >= 0 && slot < stacks.length ? stacks[slot] : null; }

	@Override
	public void setStack(int slot, ItemStack stack) { if (slot >= 0 && slot < stacks.length) stacks[slot] = stack; }

	@Override
	public ItemStack removeStack(int slot, int amount) {
		ItemStack s = getStack(slot);
		if (s == null) return null;
		ItemStack split = s.split(amount);
		if (s.count == 0) stacks[slot] = null;
		return split;
	}

	@Override
	public String getName() { return "Crate"; }

	@Override
	public int getMaxCountPerStack() { return 64; }

	@Override
	public boolean canPlayerUse(PlayerEntity player) { return true; }

	@Override
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		NbtList items = new NbtList();
		for (int i = 0; i < stacks.length; i++) {
			if (stacks[i] != null) {
				NbtCompound item = new NbtCompound();
				item.putByte("Slot", (byte) i);
				stacks[i].writeNbt(item);
				items.add(item);
			}
		}
		nbt.put("Items", items);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		NbtList items = nbt.getList("Items");
		if (items == null) return;
		for (int i = 0; i < items.size(); i++) {
			NbtCompound item = (NbtCompound) items.get(i);
			int slot = item.getByte("Slot") & 0xFF;
			if (slot >= 0 && slot < stacks.length) {
				ItemStack s = new ItemStack(0, 0, 0);
				s.readNbt(item);
				stacks[slot] = s;
			}
		}
	}
}
