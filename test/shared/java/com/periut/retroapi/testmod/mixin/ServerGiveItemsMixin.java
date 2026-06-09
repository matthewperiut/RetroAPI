package com.periut.retroapi.testmod.mixin;

import com.periut.retroapi.testmod.TestPlayerSetup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dedicated-server hook for the one-shot test setup. {@code ServerPlayerEntity.tick()} is driven by
 * {@code world.tickEntities()} every server tick - unlike {@code PlayerEntity.tick}, which the server
 * only reaches via {@code playerTick} inside the movement-packet handler - so this fires reliably on
 * join regardless of what the client sends. The once-guard in {@link TestPlayerSetup} keeps the two
 * hooks from double-running.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerGiveItemsMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void retroapi_test$setUpTestPlayer(CallbackInfo ci) {
		TestPlayerSetup.run((PlayerEntity) (Object) this);
	}
}
