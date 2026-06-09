package com.periut.retroapi.mixin.dimension.client;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Singleplayer world-object hops for modded dimensions.
 *
 * <p><b>World load:</b> vanilla's {@code World(WorldStorage, ...)} constructor only special-cases
 * dimension id -1 (the nether) from level.dat, so the world always loads as the overworld for a
 * modded id. The player NBT restore ({@code PlayerDimensionMixin.readNbt}) then sets
 * {@code dimensionId} back to the modded serial id - leaving the player physically in the
 * overworld while their data says otherwise, which inverts portal direction logic. After
 * {@code startGame}, if the restored id is a registered modded dimension, hop into it.</p>
 *
 * <p><b>Respawn:</b> vanilla {@code respawnPlayer} calls {@code changeDimension()} when the
 * current dimension has no world spawn, and that method TOGGLES: {@code dim == -1 ? 0 : -1}.
 * Dying in a modded dimension would therefore respawn the player in the nether. Route a
 * modded-dimension player to the overworld instead.</p>
 *
 * <p>Both hops mirror vanilla {@code changeDimension()}'s full sequence (remove from old world,
 * reposition, setWorld, re-bind {@code player.world}, reposition + updateEntity) without the 8x
 * coordinate scaling. Skipping the re-bind leaves the player entity attached to the OLD world
 * object: rendering follows {@code Minecraft.world} while physics still query
 * {@code player.world} - invisible-terrain collisions.</p>
 */
@Mixin(Minecraft.class)
public abstract class MinecraftStartGameMixin {

	@Shadow public World world;
	@Shadow public ClientPlayerEntity player;

	@Shadow public abstract void setWorld(World world, String message, net.minecraft.entity.player.PlayerEntity player);

	@Inject(method = "startGame", at = @At("TAIL"))
	private void retroapi$restoreModdedDimension(String worldDir, String worldName, long seed, CallbackInfo ci) {
		if (this.world == null || this.player == null || this.world.isRemote) {
			return;
		}
		int dim = this.player.dimensionId;
		if (RetroDimensionRegistry.isVanillaId(dim)) {
			return;
		}
		DimensionRegistration reg = RetroDimensionRegistry.getBySerialId(dim);
		if (reg == null || this.world.dimension.id == dim) {
			return;
		}

		RetroAPI.LOGGER.info("Restoring player into modded dimension {} (serial id {})", reg.getId(), dim);
		retroapi$hop(dim, "Loading " + reg.getId().identifier());
	}

	/**
	 * Vanilla changeDimension() toggles -1 <-> 0; for a player in a modded dimension that sends
	 * them to the nether. Replace the whole method with an overworld hop in that case (respawn
	 * repositions to the spawn point afterwards).
	 */
	@Inject(method = "changeDimension", at = @At("HEAD"), cancellable = true)
	private void retroapi$leaveModdedDimension(CallbackInfo ci) {
		if (this.world == null || this.player == null || this.world.isRemote) {
			return;
		}
		int dim = this.player.dimensionId;
		if (RetroDimensionRegistry.isVanillaId(dim) || RetroDimensionRegistry.getBySerialId(dim) == null) {
			return;
		}
		RetroAPI.LOGGER.info("Leaving modded dimension (serial id {}) -> overworld", dim);
		retroapi$hop(0, "Leaving dimension");
		ci.cancel();
	}

	@Unique
	private void retroapi$hop(int dim, String message) {
		double x = this.player.x;
		double y = this.player.y;
		double z = this.player.z;

		this.player.dimensionId = dim;
		this.world.remove(this.player);
		this.player.dead = false;
		this.player.setPositionAndAnglesKeepPrevAngles(x, y, z, this.player.yaw, this.player.pitch);
		if (this.player.isAlive()) {
			this.world.updateEntity(this.player, false);
		}

		World newWorld = new World(this.world, Dimension.fromId(dim));
		this.setWorld(newWorld, message, this.player);

		this.player.world = this.world;
		if (this.player.isAlive()) {
			this.player.setPositionAndAngles(x, y, z, this.player.yaw, this.player.pitch);
			this.world.updateEntity(this.player, false);
		}
	}
}
