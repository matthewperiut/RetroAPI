package com.periut.retrotweaks.mixin.server;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The server-side half of "Shift Placing": the client half in
 * {@link com.periut.retrotweaks.mixin.client.InteractionManagerMixin} only predicts the
 * placement locally, so on the actual connection used for any real game (multiplayer, and the
 * integrated server singleplayer runs on) the server must independently decide the same thing or the
 * two sides desync. Same pattern as {@link DisableBlockInteractionsServerMixin}. From UniTweaks.
 */
@Mixin(ServerPlayerInteractionManager.class)
public class ShiftPlacingServerMixin {

	@Shadow
	public PlayerEntity player;

	@WrapOperation(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I"))
	private int retrotweaks$shiftPlacing(World world, int x, int y, int z, Operation<Integer> original) {
		int blockId = original.call(world, x, y, z);
		if (!Config.GAMEPLAY.shiftPlacing) return blockId;
		if (this.player == null || this.player.getHand() == null) return blockId;
		if (!this.player.isSneaking()) return blockId;
		if (retrotweaks$isBlacklisted(blockId)) return blockId;
		return 0;
	}

	@Unique
	private static boolean retrotweaks$isBlacklisted(int blockId) {
		for (Integer id : Config.GAMEPLAY.shiftPlacingBlacklist) {
			if (id != null && id == blockId) return true;
		}
		return false;
	}
}
