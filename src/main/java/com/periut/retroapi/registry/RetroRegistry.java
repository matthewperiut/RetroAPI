package com.periut.retroapi.registry;

import com.periut.retroapi.entity.EntityRegistration;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RetroRegistry {
	private static final List<BlockRegistration> BLOCKS = new ArrayList<>();
	private static final List<ItemRegistration> ITEMS = new ArrayList<>();
	private static final List<EntityRegistration> ENTITIES = new ArrayList<>();

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

	public static void registerEntity(EntityRegistration registration) {
		ENTITIES.add(registration);
	}

	public static List<EntityRegistration> getEntities() {
		return Collections.unmodifiableList(ENTITIES);
	}

	public static EntityRegistration getEntityRegistration(Class<?> entityClass) {
		for (EntityRegistration reg : ENTITIES) {
			if (reg.getEntityClass() == entityClass) {
				return reg;
			}
		}
		return null;
	}

	public static EntityRegistration getEntityById(NamespacedIdentifier id) {
		for (EntityRegistration reg : ENTITIES) {
			if (reg.getId().equals(id)) {
				return reg;
			}
		}
		return null;
	}

	/** Look up an entity registration by its wire string id ({@code id.toString()}, e.g. "aether:moa"). */
	public static EntityRegistration getEntityByStringId(String id) {
		for (EntityRegistration reg : ENTITIES) {
			if (reg.getId().toString().equals(id)) {
				return reg;
			}
		}
		return null;
	}

	/**
	 * Resolve any item identifier string - modded ("aether:skyroot_pickaxe") or vanilla
	 * ("minecraft:iron_ingot", names from {@link VanillaIds}) - to the live Item, or null.
	 * Modded items, then modded block-items, then vanilla.
	 */
	public static Item getItemByStringId(String id) {
		for (ItemRegistration reg : ITEMS) {
			if (reg.getId().toString().equals(id)) {
				return reg.getItem();
			}
		}
		for (BlockRegistration reg : BLOCKS) {
			if (reg.getBlockItem() != null && reg.getId().toString().equals(id)) {
				return reg.getBlockItem();
			}
		}
		int vanillaId = VanillaIds.itemId(id);
		if (vanillaId >= 0 && vanillaId < Item.ITEMS.length) {
			return Item.ITEMS[vanillaId];
		}
		return null;
	}

	/**
	 * Resolve any block identifier string - modded or vanilla ({@link VanillaIds} names) -
	 * to the live Block, or null.
	 */
	public static Block getBlockByStringId(String id) {
		for (BlockRegistration reg : BLOCKS) {
			if (reg.getId().toString().equals(id)) {
				return reg.getBlock();
			}
		}
		int vanillaId = VanillaIds.blockId(id);
		if (vanillaId >= 0 && vanillaId < Block.BLOCKS.length) {
			return Block.BLOCKS[vanillaId];
		}
		return null;
	}

	/** All item identifier strings resolvable by {@link #getItemByStringId}: modded items, modded block-items, vanilla. */
	public static List<String> getItemIdentifierStrings() {
		List<String> ids = new ArrayList<>();
		for (ItemRegistration reg : ITEMS) {
			ids.add(reg.getId().toString());
		}
		for (BlockRegistration reg : BLOCKS) {
			if (reg.getBlockItem() != null) {
				ids.add(reg.getId().toString());
			}
		}
		ids.addAll(VanillaIds.itemIdentifiers());
		return ids;
	}

	/** All block identifier strings resolvable by {@link #getBlockByStringId}: modded, then vanilla. */
	public static List<String> getBlockIdentifierStrings() {
		List<String> ids = new ArrayList<>();
		for (BlockRegistration reg : BLOCKS) {
			ids.add(reg.getId().toString());
		}
		ids.addAll(VanillaIds.blockIdentifiers());
		return ids;
	}
}
