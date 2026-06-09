package com.periut.retroapi.component;

import net.minecraft.nbt.NbtCompound;

/**
 * Duck interface on the dropped-item spawn packet: lets the client network handler read the
 * component blob the packet carried, after it has rebuilt the {@link net.minecraft.item.ItemStack}.
 */
public interface SpawnComponentCarrier {

	NbtCompound retroapi$spawnComponents();
}
