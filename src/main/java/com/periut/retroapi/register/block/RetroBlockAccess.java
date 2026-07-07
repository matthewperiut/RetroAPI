package com.periut.retroapi.register.block;

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

	// --- Block property wrappers (delegate to protected Block methods) ---

//	RetroBlockAccess sounds(BlockSoundGroup sounds);

    RetroBlockAccess sounds(BlockSoundGroup sounds);

    /** Sets both hardness and blast resistance to the same value. */
	RetroBlockAccess strength(float strength);

	/** Sets hardness and blast resistance separately. */
	RetroBlockAccess strength(float strength, float resistance);

	RetroBlockAccess resistance(float resistance);

	RetroBlockAccess light(float light);

	RetroBlockAccess opacity(int opacity);

	/**
	 * Makes this block always drop items when broken, regardless of which tool is used.
	 * By default, blocks with stone/metal material only drop when mined with the correct tool.
	 */
	RetroBlockAccess alwaysDrops();

	/**
	 * Makes any tool effective at mining this block (not just the material-appropriate tool).
	 */
	RetroBlockAccess alwaysEffectiveTool();

	/**
	 * Sets a specific tool class as the effective tool for this block.
	 * Use with {@link net.minecraft.item.PickaxeItem}, {@link net.minecraft.item.AxeItem},
	 * {@link net.minecraft.item.ShovelItem}, or {@link net.minecraft.item.SwordItem}.
	 */
	RetroBlockAccess effectiveTool(Class<? extends Item> toolClass);

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
	RetroBlockAccess states(com.periut.retroapi.state.RetroProperty<?>... properties);

	/**
	 * Overrides the default state, e.g. {@code .defaultState(s -> s.with(LIT, false))}.
	 * Must come after {@link #states}.
	 */
	RetroBlockAccess defaultState(java.util.function.UnaryOperator<com.periut.retroapi.state.RetroBlockState> transformer);

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
	RetroBlockAccess facing();

	/** True when {@link #facing()} was called: the block auto-orients toward the placer. */
	boolean isAutoFacing();

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
	RetroBlockAccess mineable(com.periut.retroapi.tag.RetroTool... tools);

	/**
	 * Check if this block always drops items regardless of tool.
	 */
	boolean isAlwaysDrops();

	/**
	 * Check if any tool is effective at mining this block.
	 */
	boolean isAlwaysEffectiveTool();

	/**
	 * Get the effective tool class for this block, or null if not set.
	 */
	Class<? extends Item> getEffectiveTool();

	// --- RetroAPI extensions ---

	/**
	 * Mark this block as non-opaque (not a full opaque cube).
	 * Allows neighboring blocks to render their adjacent faces
	 * and lets light pass through.
	 */
	RetroBlockAccess nonOpaque();

	/**
	 * Set persistent block bounds for collision, selection, and rendering.
	 * Unlike {@link Block#setBoundingBox}, these bounds survive inventory rendering resets.
	 */
	RetroBlockAccess bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

	/**
	 * Set the render type for this block using a flattened identifier.
	 *
	 * @see RenderType for vanilla and custom render type identifiers
	 */
	RetroBlockAccess renderType(NamespacedIdentifier renderTypeId);

	/**
	 * Set the sprite index for this block (used for particles and fallback rendering).
	 */
	RetroBlockAccess sprite(int spriteId);

	/**
	 * Register a single texture for all faces.
	 * Texture file: assets/{id.namespace()}/textures/block/{id.identifier()}.png
	 */
	RetroBlockAccess texture(NamespacedIdentifier textureId);

	/**
	 * Register this block with RetroAPI.
	 * Handles BlockItem creation, registry, and StationAPI compat.
	 */
	Block register(NamespacedIdentifier id);

	/**
	 * Register this block with a custom BlockItem (e.g. {@link RetroMetaBlockItem} for
	 * meta-named blocks). The factory receives the shifted item id expected by the
	 * {@link net.minecraft.item.BlockItem} constructor (block id - 256).
	 */
	Block register(NamespacedIdentifier id, java.util.function.IntFunction<net.minecraft.item.BlockItem> itemFactory);

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
	 */
	static int allocateId() {
		Block[] byId = Block.BLOCKS;
		Item[] itemById = Item.ITEMS;
		for (int i = 256; i < byId.length; i++) {
			if (byId[i] == null && (i >= itemById.length || itemById[i] == null)) {
				return i;
			}
		}
		throw new RuntimeException("No more placeholder block IDs available (256-" + (byId.length - 1) + " exhausted)");
	}

	/** @deprecated Use {@link #allocateId()} */
	@Deprecated
	static int allocatePlaceholderBlockId() {
		return allocateId();
	}
}
