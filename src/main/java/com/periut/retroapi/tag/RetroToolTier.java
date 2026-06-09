package com.periut.retroapi.tag;

import net.minecraft.item.Item;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterial;

/**
 * Tool material tiers, mirroring modern Minecraft's {@code needs_<tier>_tool} tag family.
 * A block in {@code needs_iron_tool} only drops when the breaking tool's tier is at least
 * IRON (and the tool KIND still has to match a {@code mineable/<tool>} tag, exactly like
 * modern). Vanilla gold tools mine at wood tier, also like modern.
 *
 * <p>Item side: vanilla {@link ToolItem} subclasses infer their tier from their material's
 * mining level automatically; custom items declare one with
 * {@code RetroItemAccess.tier(RetroToolTier.IRON)} (chainable at registration or from a
 * constructor via {@code RetroItemAccess.of(this).tier(...)}).</p>
 *
 * <p>Block side: data files at {@code data/{ns}/tags/block/needs_stone_tool.json} (and
 * {@code needs_iron_tool}, {@code needs_diamond_tool}) in the modern format, or from code:
 * {@code RetroTags.addToTag(RetroToolTier.IRON.needsTag(), block)}.</p>
 */
public enum RetroToolTier {
	WOOD(0, null),
	STONE(1, "needs_stone_tool"),
	IRON(2, "needs_iron_tool"),
	DIAMOND(3, "needs_diamond_tool");

	private final int level;
	private final String tagName;

	RetroToolTier(int level, String tagName) {
		this.level = level;
		this.tagName = tagName;
	}

	/** Numeric mining level, comparable to {@link ToolMaterial#getMiningLevel()}. */
	public int getLevel() {
		return level;
	}

	/** The {@code needs_<tier>_tool} tag key, or null for WOOD (everything mines wood tier). */
	public RetroTagKey needsTag() {
		return tagName == null ? null : RetroTagKey.block(tagName);
	}

	/** True when a tool of this tier may harvest a block requiring the given tier. */
	public boolean isAtLeast(RetroToolTier required) {
		return level >= required.level;
	}

	/**
	 * The tier of an item: an explicit {@code RetroItemAccess.tier(...)} declaration wins,
	 * vanilla tool items infer from their material's mining level, everything else is WOOD
	 * (matching modern, where a bare hand or a stick is a wood-tier "tool").
	 */
	public static RetroToolTier of(Item item) {
		if (item == null) {
			return WOOD;
		}
		RetroToolTier declared = ((com.periut.retroapi.register.item.RetroItemAccess) item).getToolTier();
		if (declared != null) {
			return declared;
		}
		if (item instanceof ToolItem) {
			return fromLevel(((com.periut.retroapi.mixin.register.ToolItemAccessor) item)
				.retroapi$getToolMaterial().getMiningLevel());
		}
		return WOOD;
	}

	/** The tier for a numeric mining level, clamped into the known range. */
	public static RetroToolTier fromLevel(int level) {
		if (level >= 3) return DIAMOND;
		if (level == 2) return IRON;
		if (level == 1) return STONE;
		return WOOD;
	}
}
