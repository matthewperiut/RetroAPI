package net.modificationstation.stationapi.api.client.color.item;

import net.minecraft.item.ItemStack;

/** Stub - see src/stapiStub/java/README.md. Answers the colour for one tint index of one item. */
public interface ItemColorProvider {
	int getColor(ItemStack stack, int tintIndex);
}
