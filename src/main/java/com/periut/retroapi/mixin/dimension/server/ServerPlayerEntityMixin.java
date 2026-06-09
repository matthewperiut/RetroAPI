package com.periut.retroapi.mixin.dimension.server;

import com.periut.retroapi.dimension.HasTeleportationManager;
import com.periut.retroapi.dimension.TeleportationManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * When a server player's portal timer fires, vanilla calls
 * {@code PlayerManager.changePlayerDimension} (a hard-coded overworld&lt;-&gt;nether teleport). If the
 * player has a {@link TeleportationManager} attached (set by a {@link com.periut.retroapi.dimension.CustomPortal}),
 * route the transition through it instead so it reaches the modded destination; otherwise fall back to
 * vanilla. The manager is consumed (cleared) so a later vanilla portal isn't hijacked. Disabled under
 * StationAPI.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
	@Redirect(
		method = "playerTick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;changePlayerDimension(Lnet/minecraft/entity/player/ServerPlayerEntity;)V")
	)
	private void retroapi$customDimensionChange(PlayerManager playerManager, ServerPlayerEntity player) {
		HasTeleportationManager holder = (HasTeleportationManager) player;
		TeleportationManager manager = holder.getTeleportationManager();
		if (manager != null) {
			holder.setTeleportationManager(null);
			manager.switchDimension(player);
		} else {
			playerManager.changePlayerDimension(player);
		}
	}
}
