package com.periut.retroapi.mixin.gamerule.client;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code doImmediateRespawn}: no death screen, straight back into the world.
 *
 * <p>Does exactly what pressing the screen's own respawn button does, at the moment the screen is
 * built. The rule is read from the client's synced copy, which is why game rules are sent to clients
 * at all: the death screen is the client's decision and the server never sees it.
 *
 * <p>Declared as extending {@link Screen} so {@code minecraft} - which {@code Screen} owns, not
 * {@code DeathScreen} - is simply inherited. A {@code @Shadow} of it fails outright: mixin only
 * resolves shadows against the target class's own fields.
 */
@Mixin(DeathScreen.class)
public abstract class ImmediateRespawnMixin extends Screen {

	@Inject(method = "init", at = @At("TAIL"))
	private void retroapi$doImmediateRespawn(CallbackInfo ci) {
		if (!RetroGameRules.getBoolean(RetroGameRules.DO_IMMEDIATE_RESPAWN)) {
			return;
		}
		if (this.minecraft != null && this.minecraft.player != null) {
			this.minecraft.player.respawn();
			this.minecraft.setScreen(null);
		}
	}
}
