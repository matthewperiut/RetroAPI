package com.periut.retroapi.register.item;

import net.minecraft.item.ArmorItem;

/**
 * A wearable armor piece with its own worn texture, the clean way to add an armor set in beta.
 * It is a vanilla {@link ArmorItem} (so it equips, protects, and takes durability exactly like
 * iron or diamond) that also implements {@link RetroArmorTexture}, so the player renders YOUR
 * art instead of a vanilla material's. The icon in the inventory is the vanilla armor sprite
 * for the slot, tinted by {@code tint} (the leather-dye trick), so a set needs only the two
 * worn-texture sheets, no per-piece item PNGs.
 *
 * <p>{@code material} is the protection/durability tier: 0 leather, 1 chain, 2 iron, 3 diamond,
 * 4 gold. {@code slot} is 0 helmet, 1 chestplate, 2 leggings, 3 boots. {@code wornTextureBase}
 * is the resource path without the {@code _1/_2.png} suffix, e.g.
 * {@code "/assets/mymod/textures/armor/zanite"}.</p>
 *
 * <pre>{@code
 * ZANITE_HELMET = (Item) RetroItemAccess.of(
 *     new RetroArmor(2, 0, "/assets/mymod/textures/armor/zanite", 0x71B0A8))
 *     .register(id("zanite_helmet"));
 * }</pre>
 */
public class RetroArmor extends ArmorItem implements RetroArmorTexture {

	private final String wornTextureBase;
	private final int tint;

	public RetroArmor(int material, int slot, String wornTextureBase, int tint) {
		// Beta's ArmorItem(id, material, textureIndex, slot); textureIndex is the vanilla worn
		// sheet, which our mixin overrides, so 0 is fine here.
		super(RetroItemAccess.allocateId(), material, 0, slot);
		this.wornTextureBase = wornTextureBase;
		this.tint = tint;
		this.setMaxCount(1);
		// Inventory icon: the vanilla IRON armor sprite for this slot (helmet 2, chestplate
		// 18, leggings 34, boots 50, a column-per-slot in items.png), tinted by getColorMultiplier
		// to the set's color. So a set needs no per-piece item PNGs, just the two worn sheets.
		this.setTextureId(2 + slot * 16);
	}

	public RetroArmor(int material, int slot, String wornTextureBase) {
		this(material, slot, wornTextureBase, 0xFFFFFF);
	}

	@Override
	public int getColorMultiplier(int meta) {
		return this.tint; // tints the vanilla armor icon to the set's color
	}

	@Override
	public String getArmorTexture(int layer) {
		return this.wornTextureBase + "_" + (layer == 2 ? 2 : 1) + ".png";
	}
}
