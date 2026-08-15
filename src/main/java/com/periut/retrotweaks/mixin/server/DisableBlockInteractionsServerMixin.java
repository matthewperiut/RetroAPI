package com.periut.retrotweaks.mixin.server;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The server-side fallback for "Disable Block Interactions With Keybind": the dedicated client key
 * has no meaning to the server, so on the actual connection used for any real game (multiplayer, and
 * the integrated server singleplayer runs on) sneaking does the same job instead. From
 * GameplayEssentials.
 */
@Mixin(ServerPlayerInteractionManager.class)
public class DisableBlockInteractionsServerMixin {

	@Shadow
	public PlayerEntity player;

	@WrapOperation(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I"))
	private int retrotweaks$disableBlockInteractionsKeybind(World world, int x, int y, int z, Operation<Integer> original) {
		if (Config.GAMEPLAY.disableBlockInteractionsKeybind
			&& this.player.isSneaking()
			&& !(this.player.getHand() == null)
		) {
			return 0;
		}

		return original.call(world, x, y, z);
	}
}
