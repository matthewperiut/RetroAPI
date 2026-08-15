package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Squid can be milked with a bucket, an old joke behaviour. From BetaTweaks and UniTweaksTelsAddons.
 *
 * <p>Injected rather than overridden so that another mod's {@code interact} still runs when this is
 * off, which an unconditional override would have swallowed.
 */
@Mixin(SquidEntity.class)
public class SquidEntityMixin {

	@Inject(method = "interact", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$milkSquid(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		if (!Config.GAMEPLAY.oldFeatures.milkSquids) return;
		ItemStack held = player.inventory.getSelectedItem();
		if (held == null || held.itemId != Item.BUCKET.id) return;
		player.inventory.setStack(player.inventory.selectedSlot, new ItemStack(Item.MILK_BUCKET));
		cir.setReturnValue(true);
	}
}
