package com.periut.retroapi.register.item;

import com.periut.retroapi.register.RetroInjected;
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

	default RetroItemAccess maxStackSize(int size) { throw RetroInjected.missing(); }

	default RetroItemAccess texture(NamespacedIdentifier textureId) { throw RetroInjected.missing(); }

	/**
	 * Layered sprite with NO model JSON: {@code base} is the bottom layer, each of {@code overlays} stacks
	 * on top, all flattened onto one atlas slot. Base and overlays may be modded ({@code mymod:sparkle} →
	 * {@code textures/item/sparkle.png}) or vanilla ({@code minecraft:apple}), so e.g. a "candied apple" is
	 * {@code .layers(id("minecraft","apple"), id("mymod","sugar_sparkle"))} with no {@code items/} or
	 * {@code models/item/} JSON at all. A model JSON, if present, still overrides this at registration.
	 */
	default RetroItemAccess layers(NamespacedIdentifier base, NamespacedIdentifier... overlays) { throw RetroInjected.missing(); }

	/**
	 * Stacks one more sprite on top of this item's current texture (set by {@link #texture} or
	 * {@link #layers}). Chain several for multiple layers: {@code .texture(base).overlay(a).overlay(b)}.
	 * Must follow a {@code .texture(...)}/{@code .layers(...)} call that established the base.
	 */
	default RetroItemAccess overlay(NamespacedIdentifier overlayTextureId) { throw RetroInjected.missing(); }

	/**
	 * Stacks a TINTED sprite on top of this item, the item twin of
	 * {@link com.periut.retroapi.register.block.RetroBlockAccess#overlay(NamespacedIdentifier, int)}.
	 *
	 * <p>The untinted {@link #overlay(NamespacedIdentifier)} flattens its sprite into the atlas at stitch
	 * time, which is why it cannot tint: by the time anything could apply a color the layers are one
	 * image. This one is drawn as a separate pass at render time, so the {@code 0xRRGGBB} multiply
	 * survives - and because it is declared here rather than implemented, it works on <em>any</em> item,
	 * including a subclass you did not write:
	 * <pre>
	 * RetroItemAccess.of(id -&gt; new MyOreItem(id, material))
	 *     .texture(id("ore_reg/raw"))
	 *     .overlay(id("ore_reg/raw_overlay"), material.color)
	 *     .register(id("raw_" + material.id));
	 * </pre>
	 *
	 * <p>The base is layer 0, taken from the {@link #texture}/{@link #layers} already declared, so call
	 * this after one of those. The overlay sprite is registered with
	 * {@link com.periut.retroapi.register.block.RetroTextures#getOrAddItemTexture} rather than a fresh
	 * slot, so declaring the same sprite on twenty items costs one slot, and its index is read at draw
	 * time - a handle captured before the atlas resolves still points at the right sprite.
	 *
	 * <p>An item that implements {@link com.periut.retroapi.component.RetroLayeredTexture} overrides this:
	 * per-stack layers win over declared ones, so a component-driven look can still vary per stack.
	 */
	default RetroItemAccess overlay(NamespacedIdentifier overlayTextureId, int tint) { throw RetroInjected.missing(); }

	/**
	 * Appends one fully-specified {@link com.periut.retroapi.component.RetroTextureLayer}, for a look that
	 * {@link #overlay(NamespacedIdentifier, int)} cannot express - a tinted base, or a layer built from a
	 * sprite index you already hold. Layers draw back to front; the first is the base.
	 */
	default RetroItemAccess layer(com.periut.retroapi.component.RetroTextureLayer layer) { throw RetroInjected.missing(); }

	/**
	 * The static layers declared on this item, in draw order, or an empty list if none. This is the
	 * read-back for {@link #overlay(NamespacedIdentifier, int)}/{@link #layer} - so code that builds items
	 * generically can inspect (or re-derive) the look it gave them.
	 */
	default java.util.List<com.periut.retroapi.component.RetroTextureLayer> getDeclaredLayers() { throw RetroInjected.missing(); }

	/**
	 * Declares this item's tool kind(s) for the {@code mineable/<tool>} tag system, for custom tool
	 * items that don't subclass the vanilla tool classes (those are inferred by instanceof and need no
	 * call). Pass more than one to make a multi-tool, e.g. {@code .tool(RetroTool.PICKAXE, RetroTool.AXE)}
	 * for a paxel that mines everything in both tags. Each call REPLACES the previously declared kinds.
	 * See {@code RetroBlockAccess.mineable(...)}.
	 */
	default RetroItemAccess tool(com.periut.retroapi.tag.RetroTool... tools) { throw RetroInjected.missing(); }

	/** The declared tool kinds (empty if none). Prefer {@code RetroTool.kindsOf(item)}, which also infers vanilla classes. */
	default java.util.Set<com.periut.retroapi.tag.RetroTool> getToolKinds() { throw RetroInjected.missing(); }

	/**
	 * Declares this item's tool TIER for the {@code needs_<tier>_tool} tag system, for custom
	 * tools that don't subclass {@code ToolItem} (those infer their tier from their material's
	 * mining level automatically). Chainable like every builder method, and equally usable from
	 * an item constructor via {@code RetroItemAccess.of(this).tier(...)}.
	 */
	default RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier tier) { throw RetroInjected.missing(); }

	/**
	 * Declares a DYNAMIC tool tier, computed from the actual {@link net.minecraft.item.ItemStack} at
	 * harvest time, so the tier can change at runtime (a tool that levels up, or whose tier rides its
	 * damage/NBT). Wins over a static {@link #tier(com.periut.retroapi.tag.RetroToolTier)}. Example:
	 * {@code .tier(stack -> stack.getDamage() < 100 ? RetroToolTier.DIAMOND : RetroToolTier.IRON)}.
	 */
	default RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier.Dynamic tier) { throw RetroInjected.missing(); }

	/**
	 * Declares a tier that also sees WHAT is being mined and WHO is mining it, so one tool can be worth
	 * different levels on different blocks - "diamond-tier on stone, stone-tier on wood", a drill whose
	 * bit only bites certain ores - or in different places, since the player carries the world with it.
	 * Wins over both the dynamic and the static tier.
	 * <pre>
	 * .tier((stack, block, player) -&gt; block.material == Material.STONE ? RetroToolTier.DIAMOND : RetroToolTier.WOOD)
	 *
	 * // the same tool, blunted in the nether:
	 * .tier((stack, block, player) -&gt; player != null &amp;&amp; player.world.dimension.isNether
	 *     ? RetroToolTier.WOOD : RetroToolTier.DIAMOND)
	 * </pre>
	 * The player is null only when a tier is asked for outside a harvest; the block never carries
	 * metadata or a position, because beta's harvest hooks do not pass one.
	 */
	default RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier.Contextual tier) { throw RetroInjected.missing(); }

	/**
	 * A tool tier that also sees WHERE the block is, so it can read that block's state.
	 * {@link com.periut.retroapi.tag.RetroToolTier.Contextual} gets the {@code Block}, which is the block
	 * TYPE, and every state of a block is the same {@code Block} object - so "diamond-tier on lit ore,
	 * wood-tier on unlit" was not expressible. RetroAPI records the block a player is breaking and hands
	 * the position over here.
	 * <pre>
	 * .tier((stack, block, player, world, x, y, z) -&gt; {
	 *     RetroBlockState state = RetroStates.get(world, x, y, z);
	 *     return state != null &amp;&amp; state.get(MyOre.RICH) ? RetroToolTier.DIAMOND : RetroToolTier.IRON;
	 * })
	 * </pre>
	 * The world is null (and the coordinates zero) when a tier is asked for outside an actual break -
	 * a tooltip, another mod's query - the same "no context" rule as the player. Consulted ahead of every
	 * other tier form.
	 */
	default RetroItemAccess tier(com.periut.retroapi.tag.RetroToolTier.Positional tier) { throw RetroInjected.missing(); }

	/** The declared static tool tier, or null. Prefer {@code RetroToolTier.of(stack)} which also infers ToolItems and honors the dynamic tier. */
	default com.periut.retroapi.tag.RetroToolTier getToolTier() { throw RetroInjected.missing(); }

	/** The declared dynamic tool-tier supplier, or null. */
	default com.periut.retroapi.tag.RetroToolTier.Dynamic getToolTierDynamic() { throw RetroInjected.missing(); }

	/** The declared block-aware tool-tier supplier, or null. */
	default com.periut.retroapi.tag.RetroToolTier.Contextual getToolTierContextual() { throw RetroInjected.missing(); }

	/** The declared position-aware tool-tier supplier, or null. */
	default com.periut.retroapi.tag.RetroToolTier.Positional getToolTierPositional() { throw RetroInjected.missing(); }

	/**
	 * How fast this item mines the blocks its {@link #tool} kinds cover - the {@code miningSpeed} of a
	 * {@code ToolMaterial}, without needing one. (Vanilla's {@code ToolMaterial} is an enum, so a mod
	 * cannot add a material at all; these per-parameter setters are the way around that.)
	 *
	 * <p>Reference points: wood 2, stone 4, iron 6, diamond 8, gold 12. Unset, a declared tool mines at
	 * its {@link #tier} speed.
	 */
	default RetroItemAccess miningSpeed(float speed) { throw RetroInjected.missing(); }

	/** The declared mining speed, or {@code 0} when none was set. */
	default float getMiningSpeed() { throw RetroInjected.missing(); }

	/**
	 * How much damage this item deals in hand - the {@code attackDamage} of a {@code ToolMaterial},
	 * without needing one. Vanilla references: sword 4-7, pickaxe 2-5, bare hand 1.
	 */
	default RetroItemAccess attackDamage(int damage) { throw RetroInjected.missing(); }

	/** The declared attack damage, or {@code -1} when none was set. */
	default int getAttackDamage() { throw RetroInjected.missing(); }

	/**
	 * How many uses this item survives, i.e. its durability. Sugar for vanilla's {@code setMaxDamage},
	 * completing the "a tool is a set of numbers, not a material" set:
	 * <pre>
	 * RetroItemAccess.create()
	 *     .tool(RetroTool.PICKAXE).tier(RetroToolTier.IRON)
	 *     .miningSpeed(8.0F).attackDamage(4).durability(750)
	 *     .handheld().texture(id("drill")).register(id("drill"));
	 * </pre>
	 */
	default RetroItemAccess durability(int uses) { throw RetroInjected.missing(); }

	/**
	 * Makes this item lose a point of durability each time it breaks a block hard enough to matter,
	 * exactly like a vanilla tool. On by default for items that declare both a {@link #tool} kind and a
	 * {@link #durability}; call with {@code false} to opt out (a creative-style tool that never wears).
	 */
	default RetroItemAccess damageOnMine(boolean enabled) { throw RetroInjected.missing(); }

	/**
	 * Marks this item as held like a tool: the in-hand render angles it through the fist
	 * (the diagonal pose vanilla tools and sticks get) instead of the flat held-sprite pose.
	 * Vanilla {@code ToolItem}s already are; plain Items opt in here, or automatically by
	 * giving the item a model JSON with {@code "parent": "minecraft:item/handheld"}.
	 */
	default RetroItemAccess handheld() { throw RetroInjected.missing(); }

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
	default RetroItemAccess food(int health, boolean meat, RetroFood.OnEaten onEaten) { throw RetroInjected.missing(); }

	/**
	 * Register this item with RetroAPI.
	 */
	default Item register(NamespacedIdentifier id) { throw RetroInjected.missing(); }

	/**
	 * This same item as a plain {@link Item}, for reaching a vanilla method mid-chain.
	 *
	 * <p>Every builder method here returns {@code RetroItemAccess} so the chain keeps working inside
	 * RetroAPI itself, which cannot compile against its own interface injection - so from a consuming mod,
	 * where {@code Item} does carry all of these methods, a chain that starts with a RetroAPI call is
	 * stuck in RetroAPI's half of the API until {@link #register} hands back an {@code Item}. This is the
	 * way out and back:
	 * <pre>
	 * RetroItemAccess.of(RetroItemAccess.create().maxStackSize(16).item().setTranslationKey("wand"))
	 *     .texture(id("wand"))
	 *     .register(id("wand"));
	 * </pre>
	 * A free cast, not a conversion: it is the same object either way.
	 */
	default Item item() {
		return (Item) this;
	}

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
