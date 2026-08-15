package com.periut.retrotweaks.mixin.client;

import com.periut.retroapi.config.ConfigNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Notices when the player joins or leaves a server, so {@link ConfigNetworking} can decide which world
 * options are allowed to run.
 *
 * <p>{@code setWorld} is the one place both transitions go through - a remote world arriving is a
 * join, and any world going away (including being replaced) is a leave. Watching the network
 * handler instead would miss singleplayer, which has to count as "leaving" too.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftServerSyncMixin {

	@Unique
	private boolean retrotweaks$onServer;

	@Inject(method = "setWorld(Lnet/minecraft/world/World;Ljava/lang/String;Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At("TAIL"))
	private void retrotweaks$worldChanged(World world, String message, PlayerEntity player, CallbackInfo ci) {
		boolean remote = world != null && world.isRemote;
		if (remote == retrotweaks$onServer) return;
		retrotweaks$onServer = remote;
		if (remote) {
			ConfigNetworking.onJoinServer();
		} else {
			ConfigNetworking.onLeaveServer();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void retrotweaks$tickSync(CallbackInfo ci) {
		if (retrotweaks$onServer) ConfigNetworking.tick();
	}
}
