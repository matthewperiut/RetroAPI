package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Armour durability and defence points. From MiscTweaks.
 *
 * <p>Vanilla gives each slot a different base durability (helmet 11, chestplate 16, leggings 15,
 * boots 13) and one shared protection table. Both options here move that towards modern Minecraft,
 * where every slot of a material shares a durability and protection varies by material.
 *
 * <p>These take effect when the item is constructed, which is why they need a restart.
 */
@Mixin(ArmorItem.class)
public abstract class ArmorStatsMixin extends Item {

	@Shadow @Final private static int[] PROTECTION_BY_SLOT;

	@Mutable
	@Shadow @Final public int maxProtection;

	protected ArmorStatsMixin(int id) {
		super(id);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void retrotweaks$modernArmorStats(int id, int type, int textureIndex, int equipmentSlot, CallbackInfo ci) {
		if (Config.EQUIPMENT.equalizeBaseArmorDurability) {
			// 14 is the average of vanilla's four per-slot values, so total set durability is
			// unchanged - it is just distributed evenly, as modern Minecraft does.
			this.setMaxDamage(14 * 3 << type);
		}
		if (Config.EQUIPMENT.modernArmorDefensePoints) {
			retrotweaks$applyModernProtection(type);
			this.maxProtection = PROTECTION_BY_SLOT[equipmentSlot];
		}
	}

	/** Modern protection per slot, indexed [helmet, chestplate, leggings, boots], by material. */
	private static void retrotweaks$applyModernProtection(int type) {
		int[] values = switch (type) {
			case 4 -> new int[]{2, 5, 3, 1};  // gold
			case 3 -> new int[]{3, 8, 6, 3};  // diamond
			case 2 -> new int[]{2, 6, 5, 2};  // iron
			case 1 -> new int[]{2, 5, 4, 1};  // chain
			default -> new int[]{1, 3, 2, 1}; // leather
		};
		System.arraycopy(values, 0, PROTECTION_BY_SLOT, 0, 4);
	}
}
