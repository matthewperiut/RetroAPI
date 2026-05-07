package com.periut.retroapi.register.item;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.minecraft.item.Item;

/**
 * Duck interface injected onto all Items via mixin.
 * Provides RetroAPI functionality without requiring subclassing.
 *
 * <p>Usage:
 * <pre>
 * Item myItem = RetroItemAccess.create()
 *     .maxStackSize(64)
 *     .texture(id)
 *     .register(id);
 * </pre>
 */
public interface RetroItemAccess {

	RetroItemAccess maxStackSize(int size);

	RetroItemAccess texture(NamespacedIdentifier textureId);

	/**
	 * Register this item with RetroAPI.
	 */
	Item register(NamespacedIdentifier id);

	/**
	 * Create a new Item with an automatically allocated placeholder ID.
	 */
	static RetroItemAccess create() {
		return (RetroItemAccess) new Item(allocateId());
	}

	/**
	 * Wrap an existing Item for fluent configuration.
	 */
	static RetroItemAccess of(Item item) {
		return (RetroItemAccess) item;
	}

	/**
	 * Allocate a placeholder item ID.
	 * Use when subclassing Item: {@code super(RetroItemAccess.allocateId())}
	 */
	static int allocateId() {
		Item[] byId = Item.BY_ID;
		for (int i = 2000 + 256; i < byId.length; i++) {
			if (byId[i] == null) {
				return i - 256;
			}
		}
		throw new RuntimeException("No more placeholder item IDs available");
	}

	/** @deprecated Use {@link #allocateId()} */
	@Deprecated
	static int allocatePlaceholderItemId() {
		return allocateId();
	}
}
