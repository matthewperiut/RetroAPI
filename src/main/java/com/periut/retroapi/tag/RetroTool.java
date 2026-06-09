package com.periut.retroapi.tag;

import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;

/**
 * The five tool kinds the tag platform knows about, mirroring modern Minecraft's
 * {@code mineable/<tool>} tag family. Used by {@code RetroBlockAccess.mineable(...)}
 * (block side) and {@code RetroItemAccess.tool(...)} (item side, for custom tools
 * that don't subclass the vanilla tool items).
 */
public enum RetroTool {
	PICKAXE("pickaxe"),
	AXE("axe"),
	SHOVEL("shovel"),
	HOE("hoe"),
	SWORD("sword");

	private final String tagName;

	RetroTool(String tagName) {
		this.tagName = tagName;
	}

	/** The tag path segment, e.g. {@code "pickaxe"} for {@code mineable/pickaxe}. */
	public String getTagName() {
		return tagName;
	}

	/** The {@code mineable/<tool>} tag key for this tool. */
	public RetroTagKey mineableTag() {
		return RetroTagKey.block("mineable/" + tagName);
	}

	/**
	 * Infers the tool kind of an item from the vanilla tool classes, or from a
	 * {@code RetroItemAccess.tool(...)} declaration (checked first, so custom tool
	 * items that subclass plain Item still participate). Returns null for non-tools.
	 */
	public static RetroTool of(Item item) {
		if (item == null) {
			return null;
		}
		RetroTool declared = ((com.periut.retroapi.register.item.RetroItemAccess) item).getToolKind();
		if (declared != null) {
			return declared;
		}
		if (item instanceof PickaxeItem) return PICKAXE;
		if (item instanceof AxeItem) return AXE;
		if (item instanceof ShovelItem) return SHOVEL;
		if (item instanceof HoeItem) return HOE;
		if (item instanceof SwordItem) return SWORD;
		return null;
	}
}
