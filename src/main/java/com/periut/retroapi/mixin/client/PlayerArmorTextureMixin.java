package com.periut.retroapi.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.periut.retroapi.register.item.RetroArmorTexture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Binds a {@link RetroArmorTexture} item's own worn texture in place of beta's fixed
 * {@code /armor/<name>_<layer>.png}, so modded armor sets show their own art on the player.
 * Wraps the one {@code bindTexture(String)} call inside the armor-pass binder; vanilla armor
 * and the rest of the render path are untouched. Uses {@code @WrapOperation} (not
 * {@code @Redirect}) so it composes with any other mod wrapping the same call instead of
 * claiming it exclusively.
 */
@Mixin(PlayerEntityRenderer.class)
@Environment(EnvType.CLIENT)
public class PlayerArmorTextureMixin {

	@WrapOperation(
		method = "bindTexture(Lnet/minecraft/entity/player/PlayerEntity;IF)Z",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/PlayerEntityRenderer;bindTexture(Ljava/lang/String;)V"))
	private void retroapi$customArmorTexture(PlayerEntityRenderer self, String vanillaPath,
			Operation<Void> original, PlayerEntity player, int pass, float delta) {
		ItemStack stack = player.inventory.getArmorStack(3 - pass);
		if (stack != null) {
			Item item = stack.getItem();
			if (item instanceof RetroArmorTexture custom) {
				original.call(self, custom.getArmorTexture(pass == 2 ? 2 : 1));
				return;
			}
		}
		original.call(self, vanillaPath);
	}
}
