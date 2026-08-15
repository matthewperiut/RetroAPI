package com.periut.retrotweaks.mixin.server;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.world.PlayerSaveHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Removes a disconnecting player's vehicle from the world instead of leaving it behind, for real
 * multiplayer/hosted connections - the vehicle's own name/tag was already captured onto the player
 * by {@code PlayerVehicleMixin} when {@code savePlayerData} (wrapped below) ran. From AnnoyanceFix's
 * {@code server.ServerPlayerConnectionManagerMixin} and UniTweaksTelsAddons's identical
 * {@code vehicleloginlogoutfix.ServerPlayerConnectionManagerMixin}.
 */
@Mixin(PlayerManager.class)
public abstract class PlayerManagerVehicleMixin {

	@WrapOperation(
		method = "disconnect",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/PlayerSaveHandler;savePlayerData(Lnet/minecraft/entity/player/PlayerEntity;)V")
	)
	private void retrotweaks$removeVehicleOnLogout(PlayerSaveHandler instance, PlayerEntity playerEntity, Operation<Void> original) {
		original.call(instance, playerEntity);

		if (Config.MOBS.vehicleLogoutLoginFixes) {
			if (null != playerEntity.vehicle) {
				playerEntity.world.remove(playerEntity.vehicle);
			}
		}
	}
}
