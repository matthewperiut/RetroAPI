package com.periut.retrotweaks.mixin.server;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Skips loading any dimension id listed in {@link Config.System#disabledDimensions} on server boot.
 * From UniTweaks (General: Disabled Dimensions).
 *
 * <p>{@code loadWorld} guards its per-world chunk preparation loops with {@code this.running}; wrapping
 * that field read lets a matching world id short-circuit those loops (and, since they are {@code &&}
 * chained, the outer preparation for that world) without touching the loop structure itself.
 */
@Mixin(MinecraftServer.class)
public class DisabledDimensionsMixin {

	@WrapOperation(method = "loadWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;running:Z"))
	private boolean retrotweaks$disableDimensionLoad(MinecraftServer server, Operation<Boolean> original,
			@Local ServerWorld serverWorld, @Local(ordinal = 1) int worldId) {
		for (int i = 0; i < Config.SYSTEM.disabledDimensions.length; i++) {
			if (Config.SYSTEM.disabledDimensions[i] == worldId) {
				return false;
			}
		}

		return original.call(server);
	}
}
