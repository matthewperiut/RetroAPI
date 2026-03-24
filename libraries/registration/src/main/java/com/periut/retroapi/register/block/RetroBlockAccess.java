package com.periut.retroapi.register.block;

import com.periut.retroapi.register.rendertype.RenderType;
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

	RetroBlockAccess sounds(Block.Sounds sounds);

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
	 * Unlike {@link Block#setShape}, these bounds survive inventory rendering resets.
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
	 * Allocate a placeholder block ID.
	 * Use when subclassing Block: {@code super(RetroBlockAccess.allocateId(), material)}
	 */
	static int allocateId() {
		Block[] byId = Block.BY_ID;
		Item[] itemById = Item.BY_ID;
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
