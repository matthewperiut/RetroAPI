package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.CreativeItemUse;
import net.minecraft.client.InteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Creative spends nothing it places, eats or empties - client side.
 *
 * <p>The base manager, not the singleplayer and multiplayer ones: both override these two methods to
 * send their packets and then call {@code super}, so the body that actually spends the stack is here
 * and one mixin covers all three. See {@link CreativeItemUse} for what is put back and why a bucket
 * needs more than the count.
 */
@Mixin(InteractionManager.class)
public class CreativeNoConsumeMixin {

	@Unique private CreativeItemUse.Snapshot retroapi$held;

	@Inject(method = "interactBlock", at = @At("HEAD"))
	private void retroapi$rememberForBlock(PlayerEntity player, World world, ItemStack stack,
			int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		retroapi$held = CreativeItemUse.before(player);
	}

	@Inject(method = "interactBlock", at = @At("RETURN"))
	private void retroapi$restoreAfterBlock(PlayerEntity player, World world, ItemStack stack,
			int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {
		CreativeItemUse.after(player, retroapi$held);
		retroapi$held = null;
	}

	@Inject(method = "interactItem", at = @At("HEAD"))
	private void retroapi$rememberForItem(PlayerEntity player, World world, ItemStack stack,
			CallbackInfoReturnable<Boolean> cir) {
		retroapi$held = CreativeItemUse.before(player);
	}

	@Inject(method = "interactItem", at = @At("RETURN"))
	private void retroapi$restoreAfterItem(PlayerEntity player, World world, ItemStack stack,
			CallbackInfoReturnable<Boolean> cir) {
		CreativeItemUse.after(player, retroapi$held);
		retroapi$held = null;
	}
}
