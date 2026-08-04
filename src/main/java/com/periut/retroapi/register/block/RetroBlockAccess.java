package com.periut.retroapi.register.block;

import com.periut.retroapi.register.RetroInjected;
import com.periut.retroapi.register.rendertype.RenderType;
import net.minecraft.sound.BlockSoundGroup;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

/**
 * Duck interface injected onto all Blocks via mixin.
 * Provides RetroAPI functionality without requiring subclassing.
 *
 * <p>Usage:
 * <pre>
 * Block myBlock = RetroBlockAccess.create(Material.STONE)
 *     .sounds(Block.STONE_SOUNDS)
 *     .strength(1.5f, 10.0f)
 *     .texture(id)
 *     .register(id);
 * </pre>
 */
public interface RetroBlockAccess {

	/**
	 * Sentinel id meaning "RetroAPI, allocate a placeholder slot for me." Pass it straight to a
	 * vanilla {@code Block} (or subclass) constructor in place of a real id; RetroAPI fills in a free,
	 * reserved slot atomically from <em>inside</em> the constructor, so there is no
	 * {@link #allocateId()} call to make and no scan-then-construct window to race:
	 * <pre>
	 * MyBlock block = (MyBlock) RetroBlockAccess.of(new MyBlock(RetroBlockAccess.AUTO_ID, Material.STONE))
	 *     .strength(1.5f)
	 *     .register(id("my_block"));
	 * </pre>
	 * and from a subclass that adds its own constructor args, forward the sentinel to {@code super}:
	 * <pre>
	 * public MyBlock(int id) { super(id, Material.STONE); ... }   // new MyBlock(RetroBlockAccess.AUTO_ID)
	 * </pre>
	 *
	 * <p>This is the block twin of {@link com.periut.retroapi.register.item.RetroItemAccess#AUTO_ID} and
	 * has the same value, so the two can never be told apart by accident. It is a deliberately
	 * out-of-range value rather than {@code -1}, which is a legitimate id in the item space.
	 */
	int AUTO_ID = Integer.MIN_VALUE;

	// --- Block property wrappers (delegate to protected Block methods) ---

	default RetroBlockAccess sounds(BlockSoundGroup sounds) { throw RetroInjected.missing(); }

	/** Sets both hardness and blast resistance to the same value. */
	default RetroBlockAccess strength(float strength) { throw RetroInjected.missing(); }

	/** Sets hardness and blast resistance separately. */
	default RetroBlockAccess strength(float strength, float resistance) { throw RetroInjected.missing(); }

	default RetroBlockAccess resistance(float resistance) { throw RetroInjected.missing(); }

	default RetroBlockAccess light(float light) { throw RetroInjected.missing(); }

	default RetroBlockAccess opacity(int opacity) { throw RetroInjected.missing(); }

	/**
	 * Makes this block always drop items when broken, regardless of which tool is used.
	 * By default, blocks with stone/metal material only drop when mined with the correct tool.
	 */
	default RetroBlockAccess alwaysDrops() { throw RetroInjected.missing(); }

	/**
	 * Makes any tool effective at mining this block (not just the material-appropriate tool).
	 */
	default RetroBlockAccess alwaysEffectiveTool() { throw RetroInjected.missing(); }

	/**
	 * Sets a specific tool class as the effective tool for this block.
	 * Use with {@link net.minecraft.item.PickaxeItem}, {@link net.minecraft.item.AxeItem},
	 * {@link net.minecraft.item.ShovelItem}, or {@link net.minecraft.item.SwordItem}.
	 */
	default RetroBlockAccess effectiveTool(Class<? extends Item> toolClass) { throw RetroInjected.missing(); }

	/**
	 * Declares this block's state definition: an ordered property list whose Cartesian
	 * product becomes the block's states. Each state's flattened index is the storage
	 * format (bits 0-3 ride vanilla metadata, bits 4-11 live in the region sidecar), so
	 * definitions are capped at 4096 states and fail fast beyond that. The default state
	 * takes the first value of every property; see {@link #defaultState}.
	 *
	 * <p>Read and write states with {@code RetroStates.get/set}; blocks that skip this call
	 * get an implicit {@code meta} property (0-15). The blockstate JSON can declare the
	 * same definition with {@code "properties": {...}} (code wins on conflict).</p>
	 */
	default RetroBlockAccess states(com.periut.retroapi.state.RetroProperty<?>... properties) { throw RetroInjected.missing(); }

	/**
	 * Overrides the default state, e.g. {@code .defaultState(s -> s.with(LIT, false))}.
	 * Must come after {@link #states}.
	 */
	default RetroBlockAccess defaultState(java.util.function.UnaryOperator<com.periut.retroapi.state.RetroBlockState> transformer) { throw RetroInjected.missing(); }

	/**
	 * Makes this block turn to face the placer, the furnace/chest behavior, with no {@code onPlaced} code
	 * of your own. It declares the built-in {@link com.periut.retroapi.state.RetroFacing#PROPERTY} state
	 * (adding it to any {@link #states} you also set) and, on placement, writes
	 * {@code RetroFacing.fromYaw(placer.yaw)} into state. The default state is aimed south so the inventory
	 * icon shows its front on a visible face (override with {@link #defaultState} if you like).
	 *
	 * <p>Supply the look with a blockstate JSON that y-rotates one {@code orientable}-style model per
	 * {@code facing=} value (the freezer pattern); a {@code BlockWithEntity} still needs its
	 * {@code getRenderType()} override to honor the model, exactly as documented.</p>
	 */
	default RetroBlockAccess facing() { throw RetroInjected.missing(); }

	/**
	 * Six-way facing: the same deal as {@link #facing()}, but the block can also point straight up or down
	 * (the dispenser/piston rule - look steeply up or down as you place it). Declares the built-in
	 * {@link com.periut.retroapi.util.RetroDirection#PROPERTY}, also named {@code "facing"}, whose values
	 * serialize as {@code down/up/north/south/west/east}.
	 *
	 * <p>Pairs with {@link #sided}: when the facing is vertical the front texture goes on the top (or
	 * bottom) face and the ring of four gets the side texture.
	 */
	default RetroBlockAccess facingAll() { throw RetroInjected.missing(); }

	/** True when {@link #facing()} or {@link #facingAll()} was called: the block auto-orients toward the placer. */
	default boolean isAutoFacing() { throw RetroInjected.missing(); }

	/**
	 * Per-face textures in code - the furnace look with no model JSON, no blockstate file and no
	 * {@code getTexture} override:
	 * <pre>
	 * RetroBlockAccess.create(Material.STONE)
	 *     .facing()                                                  // or .facingAll()
	 *     .sided(id("machine_top"), id("machine_side"), id("machine_front"))
	 *     .register(id("machine"));
	 * </pre>
	 * The front follows the block's {@code facing} state, so all four (or six) orientations come from one
	 * declaration. Without a facing property the front sits on north. The bottom reuses {@code top}; use
	 * {@link #sided(NamespacedIdentifier, NamespacedIdentifier, NamespacedIdentifier, NamespacedIdentifier)}
	 * to give it its own.
	 */
	default RetroBlockAccess sided(NamespacedIdentifier top, NamespacedIdentifier side, NamespacedIdentifier front) { throw RetroInjected.missing(); }

	/** {@link #sided(NamespacedIdentifier, NamespacedIdentifier, NamespacedIdentifier)} with an explicit bottom texture. */
	default RetroBlockAccess sided(NamespacedIdentifier top, NamespacedIdentifier side, NamespacedIdentifier front,
		NamespacedIdentifier bottom) {
		throw RetroInjected.missing();
	}

	/**
	 * A log-style column: one texture on both end caps, another around the sides. Sugar for
	 * {@code .sided(top, side, side, top)} on a block that does not turn.
	 */
	default RetroBlockAccess column(NamespacedIdentifier top, NamespacedIdentifier side) { throw RetroInjected.missing(); }

	/**
	 * One texture nailed to each world face, in vanilla face order: bottom, top, north, south, west, east.
	 * Unlike {@link #sided} these do not follow a facing state. Pass {@code null} for a face to leave it
	 * on the block's main {@link #texture} sprite.
	 */
	default RetroBlockAccess textures(NamespacedIdentifier bottom, NamespacedIdentifier top, NamespacedIdentifier north,
		NamespacedIdentifier south, NamespacedIdentifier west, NamespacedIdentifier east) {
		throw RetroInjected.missing();
	}

	/**
	 * Declares which tool kinds mine this block effectively, exactly like modern
	 * Minecraft's {@code mineable/<tool>} tags (this call is sugar for
	 * {@code RetroTags.addToTag(RetroTool.X.mineableTag(), block)}). Unions with any
	 * {@code data/{ns}/tags/block/mineable/<tool>.json} files; replaces nothing.
	 *
	 * <p>Modern, DECOUPLED semantics: a mineable tag grants <b>speed</b> for matching tools. Whether the
	 * block needs a tool to <b>drop</b> is separate, coming from its material (beta's stone/metal rule,
	 * {@code Material.isHandHarvestable()}) or a {@code needs_<tier>_tool} tag. So a stone/metal-material
	 * block in {@code mineable/pickaxe} drops only with a pickaxe (of sufficient tier), while a
	 * hand-breakable block in {@code mineable/axe} still drops by hand and just mines faster with an axe.
	 * {@link #alwaysDrops()} forces it to always drop.</p>
	 */
	default RetroBlockAccess mineable(com.periut.retroapi.tag.RetroTool... tools) { throw RetroInjected.missing(); }

	/**
	 * Declares the tool TIER needed to harvest this block, the code form of a
	 * {@code data/{ns}/tags/block/needs_<tier>_tool.json} file: {@code .needsTool(RetroToolTier.IRON)} means
	 * "drops only for an iron-or-better tool of a matching {@link #mineable} kind". Sugar for
	 * {@code RetroTags.addToTag(tier.needsTag(), block)}, so it composes with data files rather than
	 * replacing them. {@code WOOD} is a no-op (everything mines wood tier).
	 */
	default RetroBlockAccess needsTool(com.periut.retroapi.tag.RetroToolTier tier) { throw RetroInjected.missing(); }

	/**
	 * Adds this block to arbitrary tags at registration, so a tag no longer needs a separate
	 * {@code RetroTags.addToTag(...)} line after the builder chain:
	 * <pre>
	 * .tag(RetroTagKey.block("ore_dictionary/stone"), MyTags.MACHINE_BASE)
	 * </pre>
	 * Unions with any data-file membership.
	 */
	default RetroBlockAccess tag(com.periut.retroapi.tag.RetroTagKey... tags) { throw RetroInjected.missing(); }

	/**
	 * Makes this block indestructible in survival, the bedrock rule: infinite hardness and blast
	 * resistance. Vanilla's {@code setUnbreakable()} is protected, so this used to force a Block subclass
	 * for something with no behavior in it at all.
	 */
	default RetroBlockAccess unbreakable() { throw RetroInjected.missing(); }

	/**
	 * Tints this block's rendering by a color computed per position, with NO model JSON and no custom
	 * renderer - grass/leaves biome coloring for your own block, or a state-driven color (a "wordle"
	 * letter that turns green or yellow):
	 * <pre>
	 * .tint((state, world, x, y, z, tintIndex) -&gt; state.get(MARK) == Mark.HIT ? 0x6AAA64 : 0xC9B458)
	 * .tint(RetroBlockColors.GRASS)     // the vanilla biome grass colormap
	 * </pre>
	 * The provider is consulted at render time for the world form and with a null world for the inventory
	 * form, so one lambda covers both. For a JSON model, the same provider drives faces with
	 * {@code tintindex} - this call just makes it work on plain code-textured blocks too.
	 */
	default RetroBlockAccess tint(com.periut.retroapi.client.render.RetroBlockColors.Provider provider) { throw RetroInjected.missing(); }

	/**
	 * Draws another sprite on top of this block's faces, with no model JSON and no custom renderer - the
	 * grass-side-overlay trick, available to any block. Chain it for several layers:
	 * <pre>
	 * .texture(id("letter_backdrop")).overlay(id("letter_q"))
	 * </pre>
	 * Each overlay is drawn as an extra render pass over the same geometry, so it can be tinted
	 * independently of the base ({@link #overlay(NamespacedIdentifier, int)}) and can change per state
	 * without needing one flattened sprite per combination.
	 */
	default RetroBlockAccess overlay(NamespacedIdentifier textureId) { throw RetroInjected.missing(); }

	/** {@link #overlay(NamespacedIdentifier)} with a fixed {@code 0xRRGGBB} tint for that layer only. */
	default RetroBlockAccess overlay(NamespacedIdentifier textureId, int tint) { throw RetroInjected.missing(); }

	/**
	 * {@link #overlay(NamespacedIdentifier)} whose sprite and tint are chosen per position, for overlays
	 * that depend on state or neighbors. Return {@code null} from the provider to skip the layer at that
	 * position.
	 */
	default RetroBlockAccess overlay(com.periut.retroapi.register.block.RetroBlockLayer.Provider provider) { throw RetroInjected.missing(); }

	/** The overlay layers as drawn with no world context (the inventory/hand form). Never null. */
	default java.util.List<com.periut.retroapi.register.block.RetroBlockLayer> getOverlayLayers() { throw RetroInjected.missing(); }

	/** The overlay layers for one position, with each provider consulted. Never null. */
	default java.util.List<com.periut.retroapi.register.block.RetroBlockLayer> getOverlayLayers(
			com.periut.retroapi.state.RetroBlockState state, net.minecraft.world.BlockView world,
			int x, int y, int z) {
		throw RetroInjected.missing();
	}

	/** True when {@link #overlay} was called at least once (a cheap pre-check for the renderer). */
	default boolean hasOverlayLayers() { throw RetroInjected.missing(); }

	/**
	 * Check if this block always drops items regardless of tool.
	 */
	default boolean isAlwaysDrops() { throw RetroInjected.missing(); }

	/**
	 * Check if any tool is effective at mining this block.
	 */
	default boolean isAlwaysEffectiveTool() { throw RetroInjected.missing(); }

	/**
	 * Get the effective tool class for this block, or null if not set.
	 */
	default Class<? extends Item> getEffectiveTool() { throw RetroInjected.missing(); }

	// --- RetroAPI extensions ---

	/**
	 * Mark this block as non-opaque (not a full opaque cube).
	 * Allows neighboring blocks to render their adjacent faces
	 * and lets light pass through.
	 */
	default RetroBlockAccess nonOpaque() { throw RetroInjected.missing(); }

	/**
	 * Set persistent block bounds for collision, selection, and rendering.
	 * Unlike {@link Block#setBoundingBox}, these bounds survive inventory rendering resets.
	 */
	default RetroBlockAccess bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) { throw RetroInjected.missing(); }

	/**
	 * The size this block is drawn at as a dropped item on the ground, overriding beta's own rule.
	 *
	 * <p>b1.7.3 draws a dropped block at quarter scale <em>unless</em> it is not a full cube, in which
	 * case it doubles it:
	 * <pre>
	 * float scale = 0.25F;
	 * if (!block.isFullCube() &amp;&amp; id != Block.SLAB.id &amp;&amp; block.getRenderType() != 16) scale = 0.5F;
	 * </pre>
	 * That reads as a rule about small blocks - a torch or a flower is mostly empty space, so drawing it
	 * bigger makes it visible on the floor. Applied to a block that is <em>nearly</em> a cube it is simply
	 * wrong: a stair, a cactus or any custom-rendered shape lands twice the size of every other item in
	 * the pile, and being twice the size, it also clips through the ground. There is no way to opt out,
	 * because the rule reads only {@code isFullCube()}, which such a block cannot answer true to without
	 * lying to collision and lighting as well.
	 *
	 * <p>{@link #compactDroppedItem()} is the value later Minecraft versions use for everything.
	 *
	 * <p><b>Not available under StationAPI.</b> Its arsenic renderer merges the method this reads, and an
	 * injector cannot target a merged method, so RetroAPI disables the hook there rather than crash on
	 * load. A block that declares a scale keeps vanilla's sizing in that configuration.
	 *
	 * @param scale the GL scale, or a negative value to go back to beta's rule
	 */
	default RetroBlockAccess droppedItemScale(float scale) { throw RetroInjected.missing(); }

	/**
	 * Draws this block's dropped form at the same size as a full cube, which is what every Minecraft
	 * version after beta does for every block. Sugar for {@code droppedItemScale(0.25F)}.
	 */
	default RetroBlockAccess compactDroppedItem() { return droppedItemScale(0.25F); }

	/** The declared dropped-item scale, or a negative value when the block leaves beta's rule alone. */
	default float getDroppedItemScale() { throw RetroInjected.missing(); }

	/**
	 * Set the render type for this block using a flattened identifier.
	 *
	 * @see RenderType for vanilla and custom render type identifiers
	 */
	default RetroBlockAccess renderType(NamespacedIdentifier renderTypeId) { throw RetroInjected.missing(); }

	/**
	 * Set the sprite index for this block (used for particles and fallback rendering).
	 */
	default RetroBlockAccess sprite(int spriteId) { throw RetroInjected.missing(); }

	/**
	 * Register a single texture for all faces.
	 * Texture file: assets/{id.namespace()}/textures/block/{id.identifier()}.png
	 */
	default RetroBlockAccess texture(NamespacedIdentifier textureId) { throw RetroInjected.missing(); }

	/**
	 * Register this block with RetroAPI.
	 * Handles BlockItem creation, registry, and StationAPI compat.
	 */
	default Block register(NamespacedIdentifier id) { throw RetroInjected.missing(); }

	/**
	 * Register this block with a custom BlockItem (e.g. {@link RetroMetaBlockItem} for
	 * meta-named blocks). The factory receives the shifted item id expected by the
	 * {@link net.minecraft.item.BlockItem} constructor (block id - 256).
	 */
	default Block register(NamespacedIdentifier id, java.util.function.IntFunction<net.minecraft.item.BlockItem> itemFactory) { throw RetroInjected.missing(); }

	/**
	 * This same block as a plain {@link Block}, for reaching a vanilla method mid-chain.
	 *
	 * <p>Every builder method here returns {@code RetroBlockAccess} so the chain keeps working inside
	 * RetroAPI itself, which cannot compile against its own interface injection - so from a consuming mod,
	 * where {@code Block} does carry all of these methods, a chain that starts with a RetroAPI call is
	 * stuck in RetroAPI's half of the API until {@link #register} hands back a {@code Block}. This is the
	 * way out and back:
	 * <pre>
	 * Block slab = RetroBlockAccess.create(Material.STONE).strength(2f).block();
	 * slab.setBoundingBox(0f, 0f, 0f, 1f, 0.5f, 1f);           // vanilla, returns void
	 * RetroBlockAccess.of(slab).texture(id("slab")).register(id("slab"));
	 * </pre>
	 * A free cast, not a conversion: it is the same object either way.
	 */
	default Block block() {
		return (Block) this;
	}

	/**
	 * Create a new Block with an automatically allocated placeholder ID.
	 */
	static RetroBlockAccess create(Material material) {
		return (RetroBlockAccess) new Block(allocateId(), material);
	}

	/**
	 * Wrap an existing Block for fluent configuration.
	 * Use this for Block subclasses:
	 * <pre>
	 * RetroBlockAccess.of(new MyBlock(RetroBlockAccess.allocateId(), material))
	 *     .sounds(Block.STONE_SOUNDS)
	 *     .register(id);
	 * </pre>
	 */
	static RetroBlockAccess of(Block block) {
		return (RetroBlockAccess) block;
	}

	/**
	 * Wrap a Block subclass by its <em>constructor</em>, so you never hand-write the id boilerplate:
	 * <pre>
	 * RetroBlockAccess.of(CounterBlock::new)   // CounterBlock(int id)
	 *     .sounds(Block.STONE_SOUND_GROUP)
	 *     .register(id("counter"));
	 * </pre>
	 * RetroAPI allocates a free placeholder id and passes it to the constructor. Use this when your block
	 * class takes only the id; if it also needs a {@link Material}, use {@link #of(java.util.function.BiFunction)}.
	 */
	static RetroBlockAccess of(java.util.function.IntFunction<? extends Block> factory) {
		return (RetroBlockAccess) factory.apply(allocateId());
	}

	/**
	 * Wrap a Block subclass whose constructor takes {@code (int id, Material material)}:
	 * <pre>
	 * RetroBlockAccess.of(MyBlock::new, Material.STONE).register(id("my_block"));
	 * </pre>
	 */
	static RetroBlockAccess of(java.util.function.BiFunction<Integer, Material, ? extends Block> factory, Material material) {
		return (RetroBlockAccess) factory.apply(allocateId(), material);
	}

	/**
	 * Allocate a placeholder block ID.
	 * Use when subclassing Block: {@code super(RetroBlockAccess.allocateId(), material)}
	 *
	 * <p>Every slot handed out is reserved and never handed out again, so holding one across other
	 * registration is safe. {@link #AUTO_ID} is still preferable where you can use it: allocating from
	 * inside the constructor makes the scan and the store one atomic step.
	 */
	static int allocateId() {
		return RetroBlockIds.allocate();
	}

	/** @deprecated Use {@link #allocateId()} */
	@Deprecated
	static int allocatePlaceholderBlockId() {
		return allocateId();
	}
}
