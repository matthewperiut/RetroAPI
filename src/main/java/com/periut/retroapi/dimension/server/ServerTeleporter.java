package com.periut.retroapi.dimension.server;

import com.periut.retroapi.dimension.DimensionTeleporter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.play.PlayerRespawnPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.dimension.PortalForcer;

/**
 * Server-side dimension teleport - RetroAPI's own generalisation of vanilla
 * {@code PlayerManager.changePlayerDimension} over an arbitrary destination + scale. Touches
 * server-only classes ({@code ServerPlayerEntity}, {@code MinecraftServer}, {@code PlayerManager},
 * {@code ServerWorld}), so it is only ever referenced/loaded from the server entrypoint
 * ({@code RetroAPIServer}); the client never loads it.
 */
public final class ServerTeleporter implements DimensionTeleporter {
	@Override
	public void teleport(PlayerEntity playerEntity, int target, int moddedDim, double coordFactor, PortalForcer agent) {
		ServerPlayerEntity player = (ServerPlayerEntity) playerEntity;
		MinecraftServer server = player.server;
		PlayerManager playerManager = server.playerManager;
		ServerWorld oldWorld = server.getWorld(player.dimensionId);
		player.dimensionId = target;
		ServerWorld newWorld = server.getWorld(target);

		player.networkHandler.sendPacket(new PlayerRespawnPacket((byte) target));
		oldWorld.serverRemove(player);
		player.dead = false;

		double x = player.x * coordFactor;
		double z = player.z * coordFactor;
		player.setPositionAndAnglesKeepPrevAngles(x, player.y, z, player.yaw, player.pitch);
		if (player.isAlive()) {
			oldWorld.updateEntity(player, false);
		}

		if (player.isAlive()) {
			newWorld.spawnEntity(player);
			player.setPositionAndAnglesKeepPrevAngles(x, player.y, z, player.yaw, player.pitch);
			newWorld.updateEntity(player, false);
			newWorld.chunkCache.forceLoad = true;
			agent.moveToPortal(newWorld, player);
			newWorld.chunkCache.forceLoad = false;
		}

		playerManager.updatePlayerAfterDimensionChange(player);
		player.networkHandler.teleport(player.x, player.y, player.z, player.yaw, player.pitch);
		player.setWorld(newWorld);
		playerManager.sendWorldInfo(player, newWorld);
		playerManager.sendPlayerStatus(player);
	}
}
