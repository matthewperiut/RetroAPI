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
	 * Declares this item's tool kind for the {@code mineable/<tool>} tag system, for custom
	 * tool items that don't subclass the vanilla tool classes (those are inferred by
	 * instanceof and need no call). See {@code RetroBlockAccess.mineable(...)}.
	 */
	RetroItemAccess tool(com.periut.retroapi.tag.RetroTool tool);

	/** The declared tool kind, or null. Prefer {@code RetroTool.of(item)} which also infers vanilla classes. */
	com.periut.retroapi.tag.RetroTool getToolKind();

	/**
	 * Declares this item's tool TIER for the {@code needs_<tier>_tool} tag system, for custom
	 * tools that don't subclass {@code ToolItem} (those infer their tier from their material's
	 * mining level automatically). Chainable like every builder method, and equally usable from
	 * an item constructor via {@code RetroItemAccess.of(this).tier(...)}.
	 */
	RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier tier);

	/** The declared tool tier, or null. Prefer {@code RetroToolTier.of(item)} which also infers ToolItems. */
	com.periut.retroapi.tag.RetroToolTier getToolTier();

	/**
	 * Marks this item as held like a tool: the in-hand render angles it through the fist
	 * (the diagonal pose vanilla tools and sticks get) instead of the flat held-sprite pose.
	 * Vanilla {@code ToolItem}s already are; plain Items opt in here, or automatically by
	 * giving the item a model JSON with {@code "parent": "minecraft:item/handheld"}.
	 */
	RetroItemAccess handheld();

	/**
	 * Makes this item EDIBLE: right-clicking eats one and restores {@code health} points
	 * (beta has no hunger bar; food heals hearts directly). Like modern's {@code .food(...)},
	 * food is a property here, not a subclass, so it stacks onto any item builder.
	 */
	default RetroItemAccess food(int health) {
		return food(health, false, null);
	}

	/** Edible, with an {@link RetroFood.OnEaten} effect that runs after the heal (a buff, a hit, ...). */
	default RetroItemAccess food(int health, RetroFood.OnEaten onEaten) {
		return food(health, false, onEaten);
	}

	/** Edible, full control: heal amount, whether it counts as meat, and the on-eat effect. */
	RetroItemAccess food(int health, boolean meat, RetroFood.OnEaten onEaten);

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
		Item[] byId = Item.ITEMS;
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
