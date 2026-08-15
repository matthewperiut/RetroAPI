package com.periut.retrotweaks.mixin.screen;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the backing {@link Inventory} each {@link Slot} wraps, which vanilla keeps private with no
 * getter. Glass Inventory Tweaks widened this field with an access widener; RetroTweaks keeps the
 * widener minimal and reaches it through a mixin accessor instead (see {@code retrotweaks.accesswidener}).
 */
@Mixin(Slot.class)
public interface SlotAccessor {

	@Accessor("inventory")
	Inventory retrotweaks$getInventory();
}
