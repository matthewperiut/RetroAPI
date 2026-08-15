package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bows wear out, as they do in modern Minecraft. From MiscTweaks.
 *
 * <p>384 is the modern bow durability. The damage is applied at the point vanilla plays the shot
 * sound, so it only counts arrows actually fired.
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin extends Item {

	protected BowItemMixin(int id) {
		super(id);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void retrotweaks$giveBowDurability(CallbackInfo ci) {
		if (Config.EQUIPMENT.bowsHaveDurability) this.setMaxDamage(384);
	}

	@Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/Entity;Ljava/lang/String;FF)V"))
	private void retrotweaks$wearBowOnShot(ItemStack stack, World world, PlayerEntity user, CallbackInfoReturnable<ItemStack> cir) {
		if (Config.EQUIPMENT.bowsHaveDurability) stack.damage(1, user);
	}
}
