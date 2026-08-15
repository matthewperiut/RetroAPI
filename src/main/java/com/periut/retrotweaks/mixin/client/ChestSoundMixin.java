package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;

import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chests make a sound when opened and closed. From UniTweaks (More Sounds).
 *
 * <p>The Door setting uses the game's own door sounds; the Modern setting would need sound files
 * RetroTweaks does not ship, so it maps to the same pair rather than being silently silent.
 */
@Mixin(ClientPlayerEntity.class)
public class ChestSoundMixin {

	@Inject(method = "openChestScreen", at = @At("TAIL"))
	private void retrotweaks$chestOpenSound(Inventory inventory, CallbackInfo ci) {
		retrotweaks$play(Config.SOUNDS.chestSounds.openSound());
	}

	@Inject(method = "closeHandledScreen", at = @At("HEAD"))
	private void retrotweaks$chestCloseSound(CallbackInfo ci) {
		PlayerEntity player = (PlayerEntity) (Object) this;
		if (!(player.currentScreenHandler instanceof GenericContainerScreenHandler)) return;
		retrotweaks$play(Config.SOUNDS.chestSounds.closeSound());
	}

	private void retrotweaks$play(String sound) {
		if (!Config.SOUNDS.moreSounds || Config.SOUNDS.chestSounds == Enums.ChestSounds.NONE) return;
		if (sound == null || sound.isEmpty()) return;
		PlayerEntity player = (PlayerEntity) (Object) this;
		player.world.playSound(player, sound, 0.3F, 1.0F);
	}
}
