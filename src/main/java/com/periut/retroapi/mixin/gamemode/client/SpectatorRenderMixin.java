package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A spectator is invisible to everyone but themselves.
 *
 * <p>Not {@code require = 0}: an injector that silently does not apply is how "spectators are still
 * visible" happens with nothing in the log to explain it. If the signature ever moves, this should
 * fail loudly at load.
 *
 * <p>Beta has no invisibility of any kind - no potion, no flag on the entity - so this is the whole
 * of it: the renderer is told to draw nothing. The player's own body is exempt because beta only
 * draws it in third person, where seeing yourself is the point of the mode.
 */
@Mixin(PlayerEntityRenderer.class)
public class SpectatorRenderMixin {

	@Inject(method = "render(Lnet/minecraft/entity/player/PlayerEntity;DDDFF)V", at = @At("HEAD"), cancellable = true)
	private void retroapi$hideSpectators(PlayerEntity player, double x, double y, double z, float yaw, float delta, CallbackInfo ci) {
		if (player == null || RetroGameModes.get(player) != RetroGameMode.SPECTATOR) {
			return;
		}
		if (player == self()) {
			return;
		}
		ci.cancel();
	}

	private static PlayerEntity self() {
		Object game = FabricLoader.getInstance().getGameInstance();
		return game instanceof Minecraft minecraft ? minecraft.player : null;
	}
}
