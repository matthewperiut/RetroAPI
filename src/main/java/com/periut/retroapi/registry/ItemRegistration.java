package com.periut.retroapi.registry;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.minecraft.item.Item;

public class ItemRegistration {
	private final NamespacedIdentifier id;
	private final Item item;

	public ItemRegistration(NamespacedIdentifier id, Item item) {
		this.id = id;
		this.item = item;
	}

	public NamespacedIdentifier getId() {
		return id;
	}

	public Item getItem() {
		return item;
	}
}
