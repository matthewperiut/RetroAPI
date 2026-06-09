package com.periut.retroapi.mixin.entity.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Multiplayer jitter fix: on a remote (client) world, cancel local physics movement for living
 * entities so the entity tracker's interpolation alone drives their position. Without this, the
 * client runs its own {@code move} physics every tick AND lerps toward the server's tracked
 * position - the two fight and tracked entities (mobs, other players) visibly jitter. The local
 * player is exempt ({@code ClientPlayerEntity}, which {@code MultiplayerClientPlayerEntity}
 * extends) - it is client-authoritative in b1.7.3; remote players ({@code OtherPlayerEntity})
 * extend {@code PlayerEntity} directly, so they stay server-driven. Non-living entities
 * (items, arrows, boats) keep local physics, matching vanilla expectations.
 *
 * <p>Uses the entity's own {@code world.isRemote} (no {@code Minecraft} reference) and lives in
 * the client mixin section. No-op in single-player ({@code isRemote} is false there).
 */
@Mixin(Entity.class)
public class EntityJitterFixMixin {
	@Inject(method = "move", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$cancelClientSidePhysics(double dx, double dy, double dz, CallbackInfo ci) {
		Entity self = (Entity) (Object) this;
		if (self.world != null && self.world.isRemote
			&& self instanceof LivingEntity
			&& !(self instanceof ClientPlayerEntity)) {
			ci.cancel();
		}
	}
}
