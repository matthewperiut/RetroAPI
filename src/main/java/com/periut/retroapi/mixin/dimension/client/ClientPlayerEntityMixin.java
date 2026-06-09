package com.periut.retroapi.mixin.dimension.client;

import com.periut.retroapi.dimension.HasTeleportationManager;
import com.periut.retroapi.dimension.TeleportationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client equivalent of {@link com.periut.retroapi.mixin.dimension.server.ServerPlayerEntityMixin}:
 * when the local player's portal timer fires, vanilla calls {@code Minecraft.changeDimension} (the
 * hard-coded overworld&lt;-&gt;nether swap). If a {@link TeleportationManager} is attached, run it
 * instead so single-player / connected clients reach the modded destination; otherwise fall back to
 * vanilla. Disabled under StationAPI.
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
	@Redirect(
		method = "tickMovement",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;changeDimension()V")
	)
	private void retroapi$customDimensionChange(Minecraft minecraft) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		HasTeleportationManager holder = (HasTeleportationManager) self;
		TeleportationManager manager = holder.getTeleportationManager();
		if (manager != null) {
			holder.setTeleportationManager(null);
			manager.switchDimension(self);
		} else {
			minecraft.changeDimension();
		}
	}
}
