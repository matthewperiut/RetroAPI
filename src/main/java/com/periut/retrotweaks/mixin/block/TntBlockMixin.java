package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.TntBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Punching TNT lights it, as in Minecraft Classic. From BetaTweaks and UniTweaks.
 *
 * <p>Vanilla already lights TNT when it is hit while holding flint and steel, so telling it the
 * player is holding flint and steel is enough - no need to restate the ignition logic.
 */
@Mixin(TntBlock.class)
public class TntBlockMixin {

	@WrapOperation(method = "onBlockBreakStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getHand()Lnet/minecraft/item/ItemStack;"))
	private ItemStack retrotweaks$punchToIgnite(PlayerEntity player, Operation<ItemStack> original) {
		if (Config.GAMEPLAY.oldFeatures.punchTntToIgnite) return new ItemStack(Item.FLINT_AND_STEEL, 1);
		return original.call(player);
	}
}
