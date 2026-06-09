package com.periut.retroapi.testmod.mixin;

import com.periut.retroapi.testmod.TestPlayerSetup;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Single-player hook for the one-shot test setup (items/chests/achievement/zeve). Client-side mixin:
 * on a dedicated server {@code PlayerEntity.tick} is only reachable through the network handler's
 * movement-packet path, so the server uses {@link ServerGiveItemsMixin} on
 * {@code ServerPlayerEntity.tick} instead (world-tick driven, packet-independent). The once-guard
 * and the {@code isRemote} guard live in {@link TestPlayerSetup}.
 */
@Mixin(PlayerEntity.class)
public class GiveItemsMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void retroapi_test$setUpTestPlayer(CallbackInfo ci) {
		TestPlayerSetup.run((PlayerEntity) (Object) this);
	}
}
