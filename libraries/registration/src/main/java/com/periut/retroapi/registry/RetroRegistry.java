package com.periut.retroapi.registry;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RetroRegistry {
	private static final List<BlockRegistration> BLOCKS = new ArrayList<>();
	private static final List<ItemRegistration> ITEMS = new ArrayList<>();

	public static void registerBlock(BlockRegistration registration) {
		BLOCKS.add(registration);
	}

	public static void registerItem(ItemRegistration registration) {
		ITEMS.add(registration);
	}

	public static List<BlockRegistration> getBlocks() {
		return Collections.unmodifiableList(BLOCKS);
	}

	public static List<ItemRegistration> getItems() {
		return Collections.unmodifiableList(ITEMS);
	}

	public static BlockRegistration getBlockRegistration(Block block) {
		for (BlockRegistration reg : BLOCKS) {
			if (reg.getBlock() == block) {
				return reg;
			}
		}
		return null;
	}

	public static ItemRegistration getItemRegistration(Item item) {
		for (ItemRegistration reg : ITEMS) {
			if (reg.getItem() == item) {
				return reg;
			}
		}
		return null;
	}

	public static BlockRegistration getBlockById(NamespacedIdentifier id) {
		for (BlockRegistration reg : BLOCKS) {
			if (reg.getId().equals(id)) {
				return reg;
			}
		}
		return null;
	}

	public static ItemRegistration getItemById(NamespacedIdentifier id) {
		for (ItemRegistration reg : ITEMS) {
			if (reg.getId().equals(id)) {
				return reg;
			}
		}
		return null;
	}
}
