package com.periut.retroapi.component;

import com.periut.retroapi.register.block.RetroTexture;

/**
 * One layer of a {@link RetroLayeredTexture}: an atlas sprite and an RGB tint applied to it
 * ({@code 0xRRGGBB}; use {@link #NO_TINT} for none). Layers draw back to front, so the first layer in
 * the list is the base and later layers stack on top. There is no separate "base texture", the base is
 * simply layer 0, which keeps the whole look in one list that an item can rebuild from a stack's components.
 *
 * <p>The sprite can be supplied two ways:
 * <ul>
 *   <li>a raw sprite index - {@link #plain(int)} / {@link #tinted(int, int)} - for a sprite whose index is
 *       already correct at the call site (e.g. a vanilla item read at render time, {@code Item.STICK.getTextureId(0)});</li>
 *   <li>a {@link RetroTexture} handle - {@link #plain(RetroTexture)} / {@link #tinted(RetroTexture, int)} -
 *       which resolves to the texture's CURRENT atlas index at draw time ({@link #resolvedSpriteId()}).</li>
 * </ul>
 *
 * <p>Prefer the handle form for a custom texture from {@link com.periut.retroapi.register.block.RetroTextures#addItemTexture}:
 * its index is a placeholder until the atlas resolves (under StationAPI the modded sprite is re-pointed after
 * registration), so capturing {@code addItemTexture(...).id} into a constant at registration freezes the
 * placeholder and renders the missing-texture sprite. Passing the {@code RetroTexture} reads {@code .id} at
 * draw time, the same render-time resolution the vanilla-sprite layers already get.
 */
public record RetroTextureLayer(int spriteId, int tint, RetroTexture texture) {

	/** White: the sprite draws with its own colors, untinted. */
	public static final int NO_TINT = 0xFFFFFF;

	/** Raw-index form (no deferred texture handle). */
	public RetroTextureLayer(int spriteId, int tint) {
		this(spriteId, tint, null);
	}

	/** The sprite index to draw, resolved now: the texture handle's current id if present, else the raw id. */
	public int resolvedSpriteId() {
		return texture != null ? texture.id : spriteId;
	}

	/** A layer drawn with its own colors, from a raw sprite index. */
	public static RetroTextureLayer plain(int spriteId) {
		return new RetroTextureLayer(spriteId, NO_TINT, null);
	}

	/** A layer multiplied by an {@code 0xRRGGBB} tint (like a dye color), from a raw sprite index. */
	public static RetroTextureLayer tinted(int spriteId, int tint) {
		return new RetroTextureLayer(spriteId, tint, null);
	}

	/** A layer whose sprite is a {@link RetroTexture} handle, resolved to its current atlas index at draw time. */
	public static RetroTextureLayer plain(RetroTexture texture) {
		return new RetroTextureLayer(0, NO_TINT, texture);
	}

	/** A {@link RetroTexture}-handle layer multiplied by an {@code 0xRRGGBB} tint. */
	public static RetroTextureLayer tinted(RetroTexture texture, int tint) {
		return new RetroTextureLayer(0, tint, texture);
	}
}
