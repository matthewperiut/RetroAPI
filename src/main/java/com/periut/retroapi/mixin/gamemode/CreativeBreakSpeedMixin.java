package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Creative breaks anything instantly; adventure and spectator break nothing.
 *
 * <p>Beta works out breaking progress as strength-over-hardness, so the whole difference between the
 * modes is expressible as the strength of the player's hands - no second code path for "am I allowed
 * to mine this", and mods that alter hardness keep working unchanged.
 */
@Mixin(PlayerInventory.class)
public class CreativeBreakSpeedMixin {
	@Shadow public PlayerEntity player;

	@Inject(method = "getStrengthOnBlock", at = @At("HEAD"), cancellable = true)
	private void retroapi$gameModeBreakSpeed(Block block, CallbackInfoReturnable<Float> cir) {
		RetroGameMode mode = RetroGameModes.get(this.player);
		if (mode == RetroGameMode.CREATIVE) {
			// Enough that every vanilla hardness resolves to one tick, matching modern's instant break.
			cir.setReturnValue(1000.0F);
		} else if (mode.isReadOnly()) {
			cir.setReturnValue(0.0F);
		}
	}
}
