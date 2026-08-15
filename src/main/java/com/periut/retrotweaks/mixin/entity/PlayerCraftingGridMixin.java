package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Persists the player's 2x2 crafting grid contents across logout/login instead of them just
 * vanishing when the inventory screen closes. From BetaTweaks ({@code PlayerEntityMixin}) and
 * UniTweaksTelsAddons ({@code keepplayercraftinggrid.PlayerBaseMixin}), which ship the identical
 * fix - one config option covers both. The half that stops vanilla from dropping the grid's
 * contents on close lives in {@code PlayerScreenHandlerMixin}.
 *
 * <p>This is a second, independent {@code @Mixin(PlayerEntity.class)} writeNbt/readNbt pair
 * alongside {@code PlayerVehicleMixin} - deliberately not merged into it, since the two features
 * are unrelated and this keeps each mixin's target self-contained.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerCraftingGridMixin extends LivingEntity {

	@Shadow public ScreenHandler playerScreenHandler;

	public PlayerCraftingGridMixin(World world) {
		super(world);
	}

	@Unique
	private void retrotweaks$clearSlotData(NbtCompound tag, int slotIndex) {
		tag.putInt("ItemID_Slot" + slotIndex, 0);
		tag.putInt("ItemAmount_Slot" + slotIndex, 0);
		tag.putInt("ItemDamage_Slot" + slotIndex, 0);
	}

	@Inject(method = "writeNbt", at = @At("HEAD"))
	private void retrotweaks$writeCraftingGridToTag(NbtCompound tag, CallbackInfo info) {
		if (!Config.INVENTORY.craftingGridAsInventory) return;

		PlayerScreenHandler currentCraftingGrid = (PlayerScreenHandler) playerScreenHandler;

		if (null != currentCraftingGrid.craftingInput) {
			for (int slotIndex = 0; slotIndex < 4; ++slotIndex) {
				ItemStack itemInSlot = currentCraftingGrid.craftingInput.getStack(slotIndex);
				if (null == itemInSlot) {
					retrotweaks$clearSlotData(tag, slotIndex);
				} else {
					tag.putInt("ItemID_Slot" + slotIndex, itemInSlot.itemId);
					tag.putInt("ItemAmount_Slot" + slotIndex, itemInSlot.count);
					tag.putInt("ItemDamage_Slot" + slotIndex, itemInSlot.getDamage());
				}
			}
		} else {
			// Clear all slots
			retrotweaks$clearSlotData(tag, 0);
			retrotweaks$clearSlotData(tag, 1);
			retrotweaks$clearSlotData(tag, 2);
			retrotweaks$clearSlotData(tag, 3);
		}
	}

	@Inject(method = "readNbt", at = @At("HEAD"))
	private void retrotweaks$readCraftingGridFromTag(NbtCompound tag, CallbackInfo info) {
		if (!Config.INVENTORY.craftingGridAsInventory) return;

		PlayerScreenHandler currentCraftingGrid = (PlayerScreenHandler) playerScreenHandler;

		if (null != currentCraftingGrid.craftingInput) {
			for (int slotIndex = 0; slotIndex < 4; ++slotIndex) {
				int itemAmount = tag.getInt("ItemAmount_Slot" + slotIndex);

				if (0 != itemAmount) {
					ItemStack itemToAdd = new ItemStack(
						tag.getInt("ItemID_Slot" + slotIndex),
						itemAmount,
						tag.getInt("ItemDamage_Slot" + slotIndex));
					currentCraftingGrid.craftingInput.setStack(slotIndex, itemToAdd);
				}
			}
		}
	}
}
