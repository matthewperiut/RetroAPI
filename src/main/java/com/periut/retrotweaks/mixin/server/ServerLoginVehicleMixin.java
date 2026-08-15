package com.periut.retrotweaks.mixin.server;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.vehicle.VehicleHolder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerLoginNetworkHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Spawns a joining player's saved vehicle back in and remounts them, for real multiplayer/hosted
 * connections. From AnnoyanceFix's {@code server.ServerPacketHandlerMixin} and UniTweaksTelsAddons's
 * identical {@code vehicleloginlogoutfix.ServerLoginNetworkHandlerMixin}.
 *
 * <p>Player data (including the vehicle name/tag saved by {@code PlayerVehicleMixin}) is loaded
 * earlier in {@code accept}, before this runs; this only has to recreate the entity and mount it.
 */
@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginVehicleMixin {

	@WrapOperation(
		method = "accept",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;initScreenHandler()V")
	)
	private void retrotweaks$respawnVehicleOnLogin(ServerPlayerEntity instance, Operation<Void> original) {
		original.call(instance);

		if (Config.MOBS.vehicleLogoutLoginFixes) {
			VehicleHolder holder = (VehicleHolder) instance;
			String vehicleName = holder.retrotweaks$vehicleName();
			if (!vehicleName.equals("null")) {
				NbtCompound vehicleTag = holder.retrotweaks$vehicleTag();
				if (vehicleTag != null) {
					Entity vehicle = EntityRegistry.create(vehicleName, instance.world);
					if (null != vehicle) {
						try {
							vehicle.read(vehicleTag);
						} catch (Exception ex) {
							vehicle.setPositionAndAnglesKeepPrevAngles(instance.x, instance.y, instance.z, instance.yaw, instance.pitch);
							System.out.println("Failed to read vehicle data");
						}
						instance.world.spawnEntity(vehicle);
						instance.setVehicle(vehicle);
					}
				}
			}
		}
	}
}
