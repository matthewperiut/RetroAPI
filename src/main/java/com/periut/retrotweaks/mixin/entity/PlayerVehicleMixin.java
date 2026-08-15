package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.vehicle.VehicleHolder;
import com.periut.retrotweaks.feature.vehicle.VehicleState;
import com.periut.retrotweaks.util.Players;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Saves the player's vehicle (boat/minecart) to their own NBT on logout/save, instead of just
 * dropping it, and remounts it on login. From AnnoyanceFix ({@code PlayerBaseMixin}) and
 * UniTweaksTelsAddons ({@code vehicleloginlogoutfix.PlayerBaseMixin}), which ship the identical
 * fix - one config option covers both.
 *
 * <p>The multiplayer half of the remount (spawning the saved vehicle on login) lives in
 * {@code ServerLoginVehicleMixin}, and vehicle removal on logout in {@code PlayerManagerVehicleMixin} -
 * both server-only, since Fabric only applies the "server" mixin set on a dedicated/hosted server,
 * never on the client process that hosts singleplayer's integrated server. The world-entities search
 * below, plus {@code WorldVehicleSpawnMixin}, is what covers singleplayer instead: it is common code,
 * applied on every environment.
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerVehicleMixin extends LivingEntity implements VehicleHolder {

	@Unique
	private static final String NULL_AS_STRING = "null";

	@Unique
	private String retrotweaks$vehicleName = NULL_AS_STRING;

	@Unique
	private NbtCompound retrotweaks$vehicleTag = new NbtCompound();

	public PlayerVehicleMixin(World world) {
		super(world);
	}

	@Override
	public String retrotweaks$vehicleName() {
		return retrotweaks$vehicleName;
	}

	@Override
	public void retrotweaks$vehicleName(String vehicleName) {
		retrotweaks$vehicleName = vehicleName;
	}

	@Override
	public NbtCompound retrotweaks$vehicleTag() {
		return retrotweaks$vehicleTag;
	}

	@Override
	public void retrotweaks$vehicleTag(NbtCompound vehicleTag) {
		retrotweaks$vehicleTag = vehicleTag;
	}

	@WrapOperation(
		method = "interact",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;interact(Lnet/minecraft/entity/player/PlayerEntity;)Z")
	)
	private boolean retrotweaks$captureVehicleOnInteract(Entity instance, PlayerEntity playerEntity, Operation<Boolean> original) {
		boolean canInteract = original.call(instance, playerEntity);

		if (Config.MOBS.vehicleLogoutLoginFixes && canInteract) {
			retrotweaks$vehicleName = (this.vehicle != null) ? EntityRegistry.getId(this.vehicle) : NULL_AS_STRING;
			if (retrotweaks$vehicleName != null && !retrotweaks$vehicleName.equals(NULL_AS_STRING)) {
				instance.write(retrotweaks$vehicleTag);
			}
		}

		return canInteract;
	}

	@Inject(method = "writeNbt", at = @At("HEAD"))
	private void retrotweaks$writeVehicleToTag(NbtCompound tag, CallbackInfo info) {
		if (!Config.MOBS.vehicleLogoutLoginFixes) return;

		retrotweaks$vehicleName = (this.vehicle != null) ? EntityRegistry.getId(this.vehicle) : NULL_AS_STRING;
		if (null != retrotweaks$vehicleName) {
			tag.putString("VehicleName", retrotweaks$vehicleName);
			if (!retrotweaks$vehicleName.equals(NULL_AS_STRING)) {
				this.vehicle.write(retrotweaks$vehicleTag);
				tag.put("VehicleTag", retrotweaks$vehicleTag);
			}
		}
	}

	@Inject(method = "readNbt", at = @At("HEAD"))
	private void retrotweaks$readVehicleFromTag(NbtCompound tag, CallbackInfo info) {
		if (!Config.MOBS.vehicleLogoutLoginFixes) return;

		retrotweaks$vehicleName = tag.getString("VehicleName");
		if (retrotweaks$vehicleName != null && !retrotweaks$vehicleName.equals(NULL_AS_STRING)) {
			retrotweaks$vehicleTag = tag.getCompound("VehicleTag");
		}

		// Find the saved vehicle if on singleplayer.
		if (world.isRemote) return;
		PlayerEntity singlePlayer = Players.local();
		if (null == singlePlayer) return;
		if (null == retrotweaks$vehicleTag) return;
		if (retrotweaks$vehicleName == null || retrotweaks$vehicleName.equals(NULL_AS_STRING)) return;
		Entity vehicle = EntityRegistry.create(retrotweaks$vehicleName, world);
		if (null != vehicle) {
			VehicleState.isVehicleSaved = true;
			Entity savedVehicle = null;

			try {
				vehicle.read(retrotweaks$vehicleTag);
			} catch (Exception ex) {
				vehicle.setPositionAndAnglesKeepPrevAngles(x, y, z, yaw, pitch);
				System.out.println("Failed to read vehicle data");
			}

			VehicleState.savedVehicleClass = vehicle.getClass();
			VehicleState.savedVehicleX = vehicle.x;
			VehicleState.savedVehicleY = vehicle.y;
			VehicleState.savedVehicleZ = vehicle.z;

			// Search for the vehicle.
			for (int entityIndex = 0; entityIndex < world.entities.size(); entityIndex++) {
				Entity entityToCheck = world.entities.get(entityIndex);

				if (  (entityToCheck.getClass().equals(VehicleState.savedVehicleClass))
				   && (1 > Math.abs(entityToCheck.x - vehicle.x))
				   && (1 > Math.abs(entityToCheck.y - vehicle.y))
				   && (1 > Math.abs(entityToCheck.z - vehicle.z))
				) {
					savedVehicle = entityToCheck;
				}
			}

			if (null != savedVehicle) {
				setVehicle(savedVehicle);
				VehicleState.isVehicleSaved = false;
			}
		}
	}
}
