package com.periut.retroapi.register.block;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

/**
 * A BlockItem whose damage value carries the block metadata, with per-meta translation keys.
 * Mirrors vanilla {@code WoolBlockItem} (and StationAPI's {@code MetaNamedBlockItemProvider} item):
 * placing keeps the stack's damage as block meta, and the display name resolves
 * {@code <blockTranslationKey><meta>.name} (e.g. {@code tile.aether.holystone0.name}).
 *
 * <p>Register via {@link RetroBlockAccess#register(net.ornithemc.osl.core.api.util.NamespacedIdentifier, java.util.function.IntFunction)}:
 * <pre>
 * RetroBlockAccess.of(new Holystone()).register(id("holystone"), RetroMetaBlockItem::new);
 * </pre>
 */
public class RetroMetaBlockItem extends BlockItem {

	public RetroMetaBlockItem(int id) {
		super(id);
		this.setHasSubtypes(true);
		this.setMaxDamage(0);
	}

	@Override
	public int getPlacementMetadata(int damage) {
		return damage;
	}

	@Override
	public String getTranslationKey(ItemStack stack) {
		return super.getTranslationKey() + stack.getDamage();
	}
}
