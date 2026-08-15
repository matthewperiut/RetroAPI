package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import com.periut.retrotweaks.mixin.client.MinecraftAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.MultiplayerClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ctrl + the drop key throws the whole stack rather than one item. From InventoryTweaks.
 *
 * <p>Done as two synthetic slot clicks - pick the stack up, then click outside the window - because
 * that is the only path the server already understands; a new packet would need the server to run
 * RetroTweaks too.
 *
 * <p>Targets both PlayerEntity (true singleplayer, via SingleplayerInteractionManager) and
 * MultiplayerClientPlayerEntity (any real multiplayer connection), because
 * MultiplayerClientPlayerEntity#dropSelectedItem is a full override that sends a packet directly
 * and never calls super - matching InventoryTweaks-StationAPI's and MouseTweaks-StationAPI's
 * PlayerMixin, which both mix into the networked player subclass as well as the base class.
 */
@Mixin({MultiplayerClientPlayerEntity.class, PlayerEntity.class})
public class PlayerDropStackMixin {

	@Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$dropWholeStack(CallbackInfo ci) {
		if (!Config.INVENTORY.modern.ctrlDropWholeStack) return;
		if (!Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && !Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) return;

		Minecraft minecraft = MinecraftAccessor.retrotweaks$getInstance();
		if (minecraft == null || minecraft.player == null || minecraft.interactionManager == null) return;

		PlayerEntity player = (PlayerEntity) (Object) this;
		// 36 is where the hotbar starts in the player screen handler; -999 means "outside the window".
		minecraft.interactionManager.clickSlot(0, 36 + player.inventory.selectedSlot, 0, false, minecraft.player);
		minecraft.interactionManager.clickSlot(0, -999, 0, false, minecraft.player);
		ci.cancel();
	}
}
