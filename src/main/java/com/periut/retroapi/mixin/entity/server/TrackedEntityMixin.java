package com.periut.retroapi.mixin.entity.server;

import com.periut.retroapi.entity.EntitySpawnCodec;
import com.periut.retroapi.entity.RetroEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.entity.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Substitutes the spawn packet for modded entities. Vanilla's {@code createAddEntityPacket} throws
 * {@code IllegalArgumentException} for any unrecognized (modded) entity type, so:
 * <ul>
 *   <li>{@code createAddEntityPacket} HEAD returns {@code null} for RetroAPI entities (dodging the throw);</li>
 *   <li>the spawn {@code sendPacket} call in {@code updateListener} is redirected to send the entity over the
 *       RetroAPI OSL channel instead - which also keeps the {@code null} packet from reaching the connection
 *       ({@code Connection.sendPacket} dereferences {@code packet.size()}).</li>
 * </ul>
 * The player is still added to the tracker's listener set first, so vanilla position/velocity/equipment
 * updates flow normally once the client has the entity. Mirrors StationAPI's {@code TrackedEntityMixin}.
 */
@Mixin(EntityTrackerEntry.class)
public abstract class TrackedEntityMixin {

	@Shadow public Entity currentTrackedEntity;

	@Inject(method = "createAddEntityPacket", at = @At("HEAD"), cancellable = true)
	private void retroapi$noVanillaSpawn(CallbackInfoReturnable<Packet> cir) {
		if (RetroEntities.isRetroEntity(this.currentTrackedEntity)) {
			cir.setReturnValue(null);
		}
	}

	@Redirect(
		method = "updateListener",
		at = @At(
			value = "INVOKE",
			ordinal = 0,
			target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"
		)
	)
	private void retroapi$spawnViaOsl(ServerPlayNetworkHandler handler, Packet packet, ServerPlayerEntity player) {
		if (RetroEntities.isRetroEntity(this.currentTrackedEntity)) {
			EntitySpawnCodec.sendSpawn(player, this.currentTrackedEntity);
		} else {
			handler.sendPacket(packet);
		}
	}
}
