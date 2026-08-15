/*
 * Ported from MiscTweaks-StationAPI. When modernArmorDefensePoints is enabled, ArmorStatsMixin
 * gives armor items modern protection points instead of vanilla's shared protection table. This
 * mixin is the companion half: it makes the HUD armor bar's count (PlayerInventory.
 * getTotalArmorDurability()) reflect those protection points too, instead of vanilla's
 * damage-fraction formula. It does that by forcing ItemStack.getDamage2() to 0 for every armor
 * piece read inside getTotalArmorDurability, which collapses the wear term of the formula so the
 * result is driven purely by maxProtection.
 */
package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryArmorMixin {

	@WrapOperation(
			method = "getTotalArmorDurability",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getDamage2()I")
	)
	private int retrotweaks$modernArmorDurability(ItemStack instance, Operation<Integer> original) {
		if (Config.EQUIPMENT.modernArmorDefensePoints) {
			return 0;
		}
		return original.call(instance);
	}
}
