package com.periut.retrotweaks.mixin.screen;

import com.periut.retrotweaks.config.Config;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.DispenserScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Shift-clicking moves items in and out of a dispenser. From InventoryTweaks.
 *
 * <p>Vanilla's {@code quickMove} does nothing for dispensers at all. In singleplayer this is purely
 * a client convenience; in multiplayer the server has to run it too, which is why that half is a
 * separate option the server owner opts into.
 */
@Mixin(DispenserScreenHandler.class)
public abstract class DispenserScreenHandlerMixin extends ScreenHandler {

	@Shadow private DispenserBlockEntity dispenserBlockEntity;

	@Override
	public ItemStack quickMove(int slotIndex) {
		boolean singleplayerClient = dispenserBlockEntity.world != null
			&& FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
		if (!singleplayerClient && !Config.INVENTORY.modern.multiplayerShiftClickDispensers) {
			return super.quickMove(slotIndex);
		}

		Slot clickedSlot = this.slots.get(slotIndex);
		if (clickedSlot == null || !clickedSlot.hasStack()) return null;

		ItemStack slotItem = clickedSlot.getStack();
		ItemStack copy = slotItem.copy();
		int dispenserSize = dispenserBlockEntity.size();
		if (slotIndex < dispenserSize) {
			// Out of the dispenser and into the inventory, filling the hotbar last as vanilla does.
			this.insertItem(slotItem, dispenserSize, this.slots.size(), true);
		} else {
			this.insertItem(slotItem, 0, dispenserSize, false);
		}

		if (slotItem.count == 0) {
			clickedSlot.setStack(null);
		} else {
			clickedSlot.markDirty();
		}
		return copy;
	}
}
