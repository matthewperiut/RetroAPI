package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** {@code keepInventory}: dying scatters nothing, so everything is still there on respawn. */
@Mixin(PlayerInventory.class)
public class KeepInventoryMixin {

	@Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
	private void retroapi$keepInventory(CallbackInfo ci) {
		if (RetroGameRules.getBoolean(RetroGameRules.KEEP_INVENTORY)) {
			ci.cancel();
		}
	}
}
