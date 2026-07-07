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

	/**
	 * Sentinel id meaning "RetroAPI, allocate a placeholder slot for me." Pass it straight to a
	 * vanilla {@code Item} (or subclass) constructor in place of a real id; RetroAPI fills in a
	 * free, reserved slot atomically from <em>inside</em> the constructor, so there is no
	 * {@link #allocateId()} call to make and no scan-then-construct window to race:
	 * <pre>
	 * MyItem item = (MyItem) RetroItemAccess.of(new MyItem(RetroItemAccess.AUTO_ID))
	 *     .texture(id("my_item"))
	 *     .register(id("my_item"));
	 * </pre>
	 * and from a subclass that adds its own constructor args, forward the sentinel to {@code super}:
	 * <pre>
	 * public MyItem(int id) { super(id); ... }   // called as new MyItem(RetroItemAccess.AUTO_ID)
	 * </pre>
	 *
	 * <p>It is a deliberately out-of-range value, <em>not</em> {@code -1}: beta's own {@code BlockItem}
	 * for block id 255 legitimately constructs {@code Item(-1)}, and every id from {@code -256} to
	 * {@code -1} is a real block-item slot, so {@code -1} could never be told apart from a genuine id.
	 * {@link Integer#MIN_VALUE} can't collide with anything real.
	 */
	int AUTO_ID = Integer.MIN_VALUE;

	RetroItemAccess maxStackSize(int size);

	RetroItemAccess texture(NamespacedIdentifier textureId);

	/**
	 * Layered sprite with NO model JSON: {@code base} is the bottom layer, each of {@code overlays} stacks
	 * on top, all flattened onto one atlas slot. Base and overlays may be modded ({@code mymod:sparkle} →
	 * {@code textures/item/sparkle.png}) or vanilla ({@code minecraft:apple}), so e.g. a "candied apple" is
	 * {@code .layers(id("minecraft","apple"), id("mymod","sugar_sparkle"))} with no {@code items/} or
	 * {@code models/item/} JSON at all. A model JSON, if present, still overrides this at registration.
	 */
	RetroItemAccess layers(NamespacedIdentifier base, NamespacedIdentifier... overlays);

	/**
	 * Stacks one more sprite on top of this item's current texture (set by {@link #texture} or
	 * {@link #layers}). Chain several for multiple layers: {@code .texture(base).overlay(a).overlay(b)}.
	 * Must follow a {@code .texture(...)}/{@code .layers(...)} call that established the base.
	 */
	RetroItemAccess overlay(NamespacedIdentifier overlayTextureId);

	/**
	 * Declares this item's tool kind(s) for the {@code mineable/<tool>} tag system, for custom tool
	 * items that don't subclass the vanilla tool classes (those are inferred by instanceof and need no
	 * call). Pass more than one to make a multi-tool, e.g. {@code .tool(RetroTool.PICKAXE, RetroTool.AXE)}
	 * for a paxel that mines everything in both tags. Each call REPLACES the previously declared kinds.
	 * See {@code RetroBlockAccess.mineable(...)}.
	 */
	RetroItemAccess tool(com.periut.retroapi.tag.RetroTool... tools);

	/** The declared tool kinds (empty if none). Prefer {@code RetroTool.kindsOf(item)}, which also infers vanilla classes. */
	java.util.Set<com.periut.retroapi.tag.RetroTool> getToolKinds();

	/**
	 * Declares this item's tool TIER for the {@code needs_<tier>_tool} tag system, for custom
	 * tools that don't subclass {@code ToolItem} (those infer their tier from their material's
	 * mining level automatically). Chainable like every builder method, and equally usable from
	 * an item constructor via {@code RetroItemAccess.of(this).tier(...)}.
	 */
	RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier tier);

	/**
	 * Declares a DYNAMIC tool tier, computed from the actual {@link net.minecraft.item.ItemStack} at
	 * harvest time, so the tier can change at runtime (a tool that levels up, or whose tier rides its
	 * damage/NBT). Wins over a static {@link #tier(com.periut.retroapi.tag.RetroToolTier)}. Example:
	 * {@code .tier(stack -> stack.getDamage() < 100 ? RetroToolTier.DIAMOND : RetroToolTier.IRON)}.
	 */
	RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier.Dynamic tier);

	/** The declared static tool tier, or null. Prefer {@code RetroToolTier.of(stack)} which also infers ToolItems and honors the dynamic tier. */
	com.periut.retroapi.tag.RetroToolTier getToolTier();

	/** The declared dynamic tool-tier supplier, or null. */
	com.periut.retroapi.tag.RetroToolTier.Dynamic getToolTierDynamic();

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
		return (RetroItemAccess) new Item(AUTO_ID);
	}

	/**
	 * Wrap an existing Item for fluent configuration.
	 */
	static RetroItemAccess of(Item item) {
		return (RetroItemAccess) item;
	}

	/**
	 * Wrap an Item subclass by its <em>constructor</em>, so you never hand-write the id boilerplate:
	 * <pre>
	 * RetroItemAccess.of(WandItem::new)   // WandItem(int id) { super(id); ... }
	 *     .texture(id("wand"))
	 *     .register(id("wand"));
	 * </pre>
	 * RetroAPI passes {@link #AUTO_ID}, which the Item constructor resolves to a free, reserved slot
	 * atomically (see {@link #AUTO_ID}). Your constructor just forwards its id to {@code super(id)}.
	 */
	static RetroItemAccess of(java.util.function.IntFunction<? extends Item> factory) {
		return (RetroItemAccess) factory.apply(AUTO_ID);
	}

	/**
	 * Allocate a placeholder item ID.
	 * Use when subclassing Item: {@code super(RetroItemAccess.allocateId())}
	 *
	 * <p>Prefer {@link #AUTO_ID}, which allocates atomically from inside the constructor. This method
	 * remains for cases where you need the numeric id in hand; it now reserves the slot it returns
	 * (see {@link RetroItemIds}) so it can never hand the same slot to two items.
	 */
	static int allocateId() {
		return RetroItemIds.allocate();
	}

	/** @deprecated Use {@link #allocateId()} */
	@Deprecated
	static int allocatePlaceholderItemId() {
		return allocateId();
	}
}
