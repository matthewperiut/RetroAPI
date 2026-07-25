package com.periut.retroapi.mixin.network;

import com.periut.retroapi.network.RetroAPINetworking;
import com.periut.retroapi.particle.RetroParticles;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorldEventListener;
import net.minecraft.world.ServerWorld;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Server -&gt; client particle bridge, the exact counterpart of {@link ServerWorldSoundMixin}. Beta's
 * protocol has no particle packet and vanilla's {@code ServerWorldEventListener.addParticle} body is
 * EMPTY, so any particle a dedicated server spawns - a machine puffing smoke, a custom effect from a
 * block entity - is dropped and multiplayer sees nothing.
 *
 * <p>Fill it in: send name + position + velocity to every player of this world within
 * {@link RetroParticles#BRIDGE_RANGE}, and let the client replay it through its own world (which routes
 * registered names to RetroAPI particle factories and everything else to vanilla).
 */
@Mixin(ServerWorldEventListener.class)
public class ServerWorldParticleMixin {

	@Shadow private ServerWorld world;

	@Inject(method = "addParticle", at = @At("HEAD"))
	private void retroapi$bridgeParticle(String name, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
		if (name == null || name.isEmpty()) return;
		double rangeSq = RetroParticles.BRIDGE_RANGE * RetroParticles.BRIDGE_RANGE;

		List<PlayerEntity> players = this.world.players;
		for (int i = 0; i < players.size(); i++) {
			PlayerEntity p = players.get(i);
			if (!(p instanceof ServerPlayerEntity sp)) continue;
			double dx = p.x - x, dy = p.y - y, dz = p.z - z;
			if (dx * dx + dy * dy + dz * dz > rangeSq) continue;
			ServerPlayNetworking.send(sp, RetroAPINetworking.PARTICLE_CHANNEL, buf -> {
				buf.writeString(name);
				buf.writeDouble(x);
				buf.writeDouble(y);
				buf.writeDouble(z);
				buf.writeDouble(velocityX);
				buf.writeDouble(velocityY);
				buf.writeDouble(velocityZ);
			});
		}
	}
}
