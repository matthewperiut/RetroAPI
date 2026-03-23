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
