package com.periut.retroapi.register.block;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import com.periut.retroapi.compat.StationBridges;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Texture registration API for RetroAPI.
 * Register textures by identifier and get {@link RetroTexture} objects whose .id
 * is updated when the atlas resolves.
 * <p>
 * When StationAPI is present, textures are resolved during TextureRegisterEvent.
 * When absent, textures are composited into the vanilla atlas at the allocated slots.
 * <p>
 * Texture files: assets/{namespace}/textures/block/{path}.png
 * and assets/{namespace}/textures/item/{path}.png
 */
public class RetroTextures {
	private static final List<RetroTexture> terrainTextures = new ArrayList<>();
	private static final List<RetroTexture> itemTextures = new ArrayList<>();
	private static int nextTerrainSlot = 256;
	private static int nextItemSlot = 256;

	// Queued block/item updates for StationAPI resolution
	private static final List<BlockEntry> trackedBlocks = new ArrayList<>();
	private static final List<ItemEntry> trackedItems = new ArrayList<>();

	/**
	 * Register a block/terrain texture.
	 * The returned RetroTexture's .id is updated when the atlas resolves.
	 */
	public static RetroTexture addBlockTexture(NamespacedIdentifier id) {
		int slot = nextTerrainSlot++;
		RetroTexture tex = new RetroTexture(id, slot);
		terrainTextures.add(tex);
		return tex;
	}

	/**
	 * Register an item texture.
	 * The returned RetroTexture's .id is updated when the atlas resolves.
	 */
	public static RetroTexture addItemTexture(NamespacedIdentifier id) {
		int slot = nextItemSlot++;
		RetroTexture tex = new RetroTexture(id, slot);
		itemTextures.add(tex);
		return tex;
	}

	/**
	 * The item texture for an id, registering it only if it is not registered yet.
	 *
	 * <p>{@link #addItemTexture} allocates a NEW atlas slot every call, so asking for the same sprite
	 * twice - from two places that both need it, or from code that runs on both sides - quietly burns a
	 * slot and hands back a second handle to the same image. Use this whenever the caller does not
	 * <em>own</em> the texture, e.g. a layer that tints a sprite someone else registered.
	 *
	 * <p>Safe on either side and at any time: registration is common (the atlas is only stitched on the
	 * client), and repeat calls return the identical handle, so its {@code .id} still resolves correctly
	 * once the atlas is built.
	 */
	public static RetroTexture getOrAddItemTexture(NamespacedIdentifier id) {
		RetroTexture existing = findByIdentifier(itemTextures, id);
		return existing != null ? existing : addItemTexture(id);
	}

	/** {@link #getOrAddItemTexture} for a block/terrain sprite. */
	public static RetroTexture getOrAddBlockTexture(NamespacedIdentifier id) {
		RetroTexture existing = findByIdentifier(terrainTextures, id);
		return existing != null ? existing : addBlockTexture(id);
	}

	private static RetroTexture findByIdentifier(List<RetroTexture> pool, NamespacedIdentifier id) {
		if (id == null) {
			return null;
		}
		for (int i = 0; i < pool.size(); i++) {
			RetroTexture tex = pool.get(i);
			if (id.equals(tex.getIdentifier())) {
				return tex;
			}
		}
		return null;
	}

	/**
	 * Registers an animated block texture from code, equivalent to a {@code .png.mcmeta}
	 * with a uniform {@code frametime}. The PNG at
	 * {@code assets/{namespace}/textures/block/{path}.png} holds square frames stacked
	 * vertically (height = frameCount * width), advancing every {@code ticksPerFrame} ticks.
	 *
	 * <p>Data-driven alternative: register with plain {@link #addBlockTexture} (or
	 * {@code .texture(id)}) and put a standard {@code .png.mcmeta} next to the PNG; it is
	 * detected automatically, supports per-frame times and interpolation, and also animates
	 * under StationAPI (whose atlas honors mcmeta natively).</p>
	 *
	 * @param frameCount informational; the actual count comes from the PNG dimensions
	 */
	public static RetroTexture addAnimatedBlockTexture(NamespacedIdentifier id, int frameCount, int ticksPerFrame) {
		RetroTexture tex = addBlockTexture(id);
		codeAnimations.put(tex, com.periut.retroapi.client.texture.AnimationMetadata.uniform(ticksPerFrame));
		return tex;
	}

	/**
	 * Registers an animated item texture from code. Same contract as
	 * {@link #addAnimatedBlockTexture}, against {@code textures/item/{path}.png}.
	 */
	public static RetroTexture addAnimatedItemTexture(NamespacedIdentifier id, int frameCount, int ticksPerFrame) {
		RetroTexture tex = addItemTexture(id);
		codeAnimations.put(tex, com.periut.retroapi.client.texture.AnimationMetadata.uniform(ticksPerFrame));
		return tex;
	}

	/**
	 * A handle for an EXISTING atlas sprite (a vanilla texture): not composited, not
	 * tracked, just an id carrier so model faces can reference vanilla textures by name
	 * ({@code minecraft:block/cobblestone}) without anyone re-shipping the PNG.
	 */
	public static RetroTexture vanillaTexture(NamespacedIdentifier id, int sprite) {
		return new RetroTexture(id, sprite);
	}

	private static final java.util.Map<RetroTexture, com.periut.retroapi.client.texture.AnimationMetadata>
		codeAnimations = new java.util.HashMap<>();

	/** Internal: the code-driven animation for a texture, or null. Read by AtlasExpander. */
	public static com.periut.retroapi.client.texture.AnimationMetadata getCodeAnimation(RetroTexture tex) {
		return codeAnimations.get(tex);
	}

	/**
	 * Track a block for sprite update during StationAPI resolution.
	 * After resolution, block.sprite is set to the RetroTexture's resolved id.
	 */
	public static void trackBlock(Block block, RetroTexture texture) {
		trackedBlocks.add(new BlockEntry(block, texture));
	}

	/**
	 * Track an item for texture update during StationAPI resolution.
	 * After resolution, the item's texture is set via StationAPI's setTexture.
	 */
	/** The texture currently tracked for an item, or null. */
	public static RetroTexture getTrackedTexture(Item item) {
		for (ItemEntry entry : trackedItems) {
			if (entry.item() == item) {
				return entry.texture();
			}
		}
		return null;
	}

	public static void trackItem(Item item, RetroTexture texture) {
		trackedItems.add(new ItemEntry(item, texture));
	}

	// --- which atlas a block's flat icon comes from ---------------------------------------------

	private static final java.util.Set<Integer> itemAtlasSprites = new java.util.HashSet<>();

	/**
	 * Gives a BLOCK's item a flat icon drawn from the item atlas, and registers it there.
	 *
	 * <p>Only blocks need this. Beta decides which atlas an icon comes from by asking whether the id is
	 * below 256, which sorts vanilla blocks from vanilla items and says nothing useful about a modded
	 * one, since every modded id is above 256. RetroAPI answers that question with "is it a block"
	 * instead, so a modded block behaves like a vanilla one and reads from terrain, which is what a
	 * modded torch or ladder wants.
	 *
	 * <p>Some do not. A door, a sign or anything whose icon is a drawing of the thing rather than a
	 * sprite off its side wants an ordinary item texture, in {@code textures/item/}, editable on its own
	 * and stitched with the other item sprites. Registering one on the item atlas without saying so left
	 * the sprite index pointing into one atlas while the other was bound, and drew whatever happened to
	 * sit at that index. This is how a block says so.
	 *
	 * @param item    the block's item, from {@code Item.ITEMS[block.id]}
	 * @param texture an item texture, from {@link #getOrAddItemTexture}
	 */
	public static void setItemSprite(Item item, RetroTexture texture) {
		if (item == null || texture == null) {
			return;
		}
		item.setTextureId(texture.id);
		itemAtlasSprites.add(item.id);
		trackItem(item, texture);
	}

	/**
	 * Whether an item's flat icon is drawn from the terrain atlas rather than the item atlas. True for a
	 * modded block by default, and false once it has declared an icon with {@link #setItemSprite}.
	 *
	 * <p>Read by the renderers on every icon drawn, so it is a set lookup and not a scan.
	 */
	public static boolean drawsFromTerrainAtlas(int itemId) {
		return com.periut.retroapi.RetroAPI.isBlock(itemId) && !itemAtlasSprites.contains(itemId);
	}

	/**
	 * Whether an item has been given an icon of its own through {@link #setItemSprite}, and so should be
	 * left to report it rather than being pointed back at its block's sprite.
	 */
	public static boolean hasOwnItemSprite(int itemId) {
		return itemAtlasSprites.contains(itemId);
	}

	/**
	 * Called during StationAPI's TextureRegisterEvent.
	 * Registers all textures with StationAPI's atlas and updates RetroTexture.id values.
	 */
	public static void resolveStationAPITextures() {
		// Register terrain textures and update ids
		for (RetroTexture tex : terrainTextures) {
			tex.id = StationBridges.get().addTerrainTexture(tex.getIdentifier().namespace(), tex.getIdentifier().identifier());
		}
		// Update tracked block sprites
		for (BlockEntry entry : trackedBlocks) {
			entry.block.textureId = entry.texture.id;
		}

		// Register item textures and update ids
		for (RetroTexture tex : itemTextures) {
			tex.id = StationBridges.get().addItemTexture(tex.getIdentifier().namespace(), tex.getIdentifier().identifier());
		}
		// Use setTexture for tracked items (handles StationAPI renderer internals)
		for (ItemEntry entry : trackedItems) {
			StationBridges.get().setItemTexture(entry.item, entry.texture.getIdentifier().namespace(), entry.texture.getIdentifier().identifier());
		}
	}

	/** Terrain textures for AtlasExpander (without StationAPI). */
	public static List<RetroTexture> getTerrainTextures() {
		return Collections.unmodifiableList(terrainTextures);
	}

	/** Item textures for AtlasExpander (without StationAPI). */
	public static List<RetroTexture> getItemTextures() {
		return Collections.unmodifiableList(itemTextures);
	}

	private record BlockEntry(Block block, RetroTexture texture) {}
	private record ItemEntry(Item item, RetroTexture texture) {}
}
