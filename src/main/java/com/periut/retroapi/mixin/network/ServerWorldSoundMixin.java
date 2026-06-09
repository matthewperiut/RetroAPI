package com.periut.retroapi.mixin.network;

import com.periut.retroapi.network.RetroAPINetworking;
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
 * Server -> client world-sound bridge. Beta 1.7.3's protocol has NO packet for arbitrary named
 * sounds - vanilla's {@code ServerWorldEventListener.playSound} body is EMPTY, so every
 * {@code world.playSound(...)} on a dedicated server (mob attack/projectile sounds, custom block
 * sounds, anything not covered by the entity-status hurt/death path) is silently dropped and
 * multiplayer plays mute. Fill the method in: send the sound over OSL to every player of this
 * world within vanilla hearing range ({@code 16 * volume} for volume > 1, else 16); the client
 * listener plays it through its own {@code world.playSound}.
 *
 * <p>Iterating {@code world.players} (this listener's own world) keys the send by dimension for
 * free - including RetroAPI's side-mapped modded dimensions, which register this same listener.
 */
@Mixin(ServerWorldEventListener.class)
public class ServerWorldSoundMixin {

	@Shadow private ServerWorld world;

	@Inject(method = "playSound", at = @At("HEAD"))
	private void retroapi$bridgeSound(String sound, double x, double y, double z, float volume, float pitch, CallbackInfo ci) {
		if (sound == null || sound.isEmpty()) return;
		double range = volume > 1.0F ? 16.0D * volume : 16.0D;
		double rangeSq = range * range;

		List<PlayerEntity> players = this.world.players;
		for (int i = 0; i < players.size(); i++) {
			PlayerEntity p = players.get(i);
			if (!(p instanceof ServerPlayerEntity sp)) continue;
			double dx = p.x - x, dy = p.y - y, dz = p.z - z;
			if (dx * dx + dy * dy + dz * dz > rangeSq) continue;
			ServerPlayNetworking.send(sp, RetroAPINetworking.PLAY_SOUND_CHANNEL, buf -> {
				buf.writeString(sound);
				buf.writeDouble(x);
				buf.writeDouble(y);
				buf.writeDouble(z);
				buf.writeFloat(volume);
				buf.writeFloat(pitch);
			});
		}
	}
}
