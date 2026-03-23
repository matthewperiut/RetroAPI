package com.periut.retroapi.mixin.client;

import com.periut.retroapi.RetroAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.PlayerRenderer;
import net.minecraft.entity.mob.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
@Environment(EnvType.CLIENT)
public class PlayerRendererMixin {

	@Unique
	private int retroapi$helmetItemId = 0;

	@Unique
	private int retroapi$heldItemId = 0;

	@Inject(
		method = "renderMore(Lnet/minecraft/entity/mob/player/PlayerEntity;F)V",
		at = @At("HEAD")
	)
	private void retroapi$captureItemIds(PlayerEntity player, float tickDelta, CallbackInfo ci) {
		// Capture helmet item id (armor slot 3 = head)
		if (player.inventory.armor[3] != null) {
			retroapi$helmetItemId = player.inventory.armor[3].getItem().id;
		} else {
			retroapi$helmetItemId = 0;
		}
		// Capture held item id
		if (player.inventory.getSelectedItem() != null) {
			retroapi$heldItemId = player.inventory.getSelectedItem().id;
		} else {
			retroapi$heldItemId = 0;
		}
	}

	@ModifyConstant(
		method = "renderMore(Lnet/minecraft/entity/mob/player/PlayerEntity;F)V",
		constant = @Constant(intValue = 256, ordinal = 0)
	)
	private int retroapi$fixHelmetBlockCheck(int original) {
		if (RetroAPI.isBlock(retroapi$helmetItemId)) {
			return retroapi$helmetItemId + 1;
		}
		return original;
	}

	@ModifyConstant(
		method = "renderMore(Lnet/minecraft/entity/mob/player/PlayerEntity;F)V",
		constant = @Constant(intValue = 256, ordinal = 1)
	)
	private int retroapi$fixHeldBlockCheck(int original) {
		if (RetroAPI.isBlock(retroapi$heldItemId)) {
			return retroapi$heldItemId + 1;
		}
		return original;
	}
}
