package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sneaking places a block against an interactive one instead of opening it. From UniTweaks.
 *
 * <p>Reporting the block as air for the placement check is enough: vanilla then treats the click as
 * a placement rather than a use, and every other rule (reach, replaceability) is untouched.
 *
 * <p>The blacklist is block ids rather than StationAPI identifiers, so it needs no API. Ids are what
 * the config file already holds for the disabled-dimensions list.
 */
@Mixin(InteractionManager.class)
public class InteractionManagerMixin {

	@Shadow @Final protected Minecraft minecraft;

	@WrapOperation(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockId(III)I"))
	private int retrotweaks$shiftPlacing(World world, int x, int y, int z, Operation<Integer> original) {
		int blockId = original.call(world, x, y, z);
		if (!Config.GAMEPLAY.shiftPlacing) return blockId;
		if (this.minecraft.player == null || this.minecraft.player.getHand() == null) return blockId;
		if (!this.minecraft.player.isSneaking()) return blockId;
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
