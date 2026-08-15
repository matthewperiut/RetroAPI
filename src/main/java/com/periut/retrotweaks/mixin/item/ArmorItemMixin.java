package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Right-clicking armour equips it, swapping with whatever is already in that slot.
 *
 * <p>From GameplayEssentials and UniTweaks. {@code use} is not declared on {@code ArmorItem}, so
 * this is an override merged into it rather than an inject.
 *
 * <p>The slot index is mirrored because {@code equipmentSlot} counts from the head down while the
 * inventory's armour array counts from the boots up.
 */
@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin extends Item {

	@Shadow
	@Final
	public int equipmentSlot;

	protected ArmorItemMixin(int id) {
		super(id);
	}

	@Override
	public ItemStack use(ItemStack stack, World world, PlayerEntity player) {
		if (!Config.GAMEPLAY.rightClickEquipArmor) return stack;

		int slot = Math.abs(this.equipmentSlot - 3);
		ItemStack worn = player.inventory.armor[slot];
		player.inventory.armor[slot] = stack.copy();
		if (worn == null) {
			// Nothing to give back, so the held stack is consumed outright.
			stack.count = 0;
			return stack;
		}
		return worn;
	}
}
