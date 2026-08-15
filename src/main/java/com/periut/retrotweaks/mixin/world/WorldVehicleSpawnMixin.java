package com.periut.retrotweaks.mixin.world;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.vehicle.VehicleState;
import com.periut.retrotweaks.util.Players;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Completes the singleplayer half of the vehicle logout/login fix: {@code PlayerVehicleMixin}'s
 * {@code readNbt} may run before the saved vehicle's chunk has loaded the old entity back in, so it
 * only records what to look for. Once that entity actually gets spawned back into the world - by
 * chunk loading, restoring it from disk - this catches it and remounts the local player. From
 * AnnoyanceFix's {@code LevelMixin} and UniTweaksTelsAddons's identical {@code WorldMixin}.
 */
@Mixin(World.class)
public abstract class WorldVehicleSpawnMixin {

	@Shadow public boolean isRemote;

	@Shadow public List<Entity> entities;

	@Inject(
		method = "spawnEntity",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;notifyEntityAdded(Lnet/minecraft/entity/Entity;)V")
	)
	private void retrotweaks$remountSavedVehicle(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (  (Config.MOBS.vehicleLogoutLoginFixes)
		   && (VehicleState.isVehicleSaved)
		   && (!this.isRemote)
		) {
			for (int entityIndex = 0; entityIndex < this.entities.size(); entityIndex++) {
				Entity entityToCheck = this.entities.get(entityIndex);

				if (  (entityToCheck.getClass().equals(VehicleState.savedVehicleClass))
				   && (1 > Math.abs(entityToCheck.x - VehicleState.savedVehicleX))
				   && (1 > Math.abs(entityToCheck.y - VehicleState.savedVehicleY))
				   && (1 > Math.abs(entityToCheck.z - VehicleState.savedVehicleZ))
				) {
					PlayerEntity player = Players.local();
					if (null != player) {
						player.setVehicle(entityToCheck);
					}
					VehicleState.isVehicleSaved = false;
				}
			}
		}
	}
}
