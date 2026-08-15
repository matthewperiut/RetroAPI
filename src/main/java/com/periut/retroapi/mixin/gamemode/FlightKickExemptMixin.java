package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.commands.util.ServerUtil;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * "Flying is not enabled on this server" does not apply to the people who are meant to be flying.
 *
 * <p>Beta counts the ticks a player spends with no block under them and disconnects them after eighty,
 * unless {@code allow-flight} is set in server.properties. That switch is all-or-nothing: turning it on
 * to let a creative player fly also invites every survival player to. Modern has no such trade because
 * it asks the player's abilities instead - a creative or spectating player is simply never counted, and
 * neither is anyone the server trusts.
 *
 * <p>So this answers the same question the same way: the server's own {@code flightEnabled} is read as
 * true for a player who is opped, in creative, or spectating, which takes the "reset the counter"
 * branch vanilla already has. Everyone else is left exactly as beta had them - the anti-fly kick is
 * untouched for the players it was written for, and {@code allow-flight} still works as before.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class FlightKickExemptMixin {

	@Shadow private ServerPlayerEntity player;

	@Redirect(method = "onPlayerMove",
		at = @At(value = "FIELD", target = "Lnet/minecraft/server/MinecraftServer;flightEnabled:Z"))
	private boolean retroapi$allowIntendedFlight(MinecraftServer server) {
		if (server.flightEnabled) {
			return true;
		}
		if (player == null || player.name == null) {
			return false;
		}

		final RetroGameMode mode = RetroGameModes.get(player.name);
		return mode == RetroGameMode.CREATIVE || mode == RetroGameMode.SPECTATOR
			|| ServerUtil.isOp(player.name);
	}
}
