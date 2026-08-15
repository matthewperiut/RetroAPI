package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tools do not wear out in creative, which is modern's rule and beta has no notion of.
 *
 * <p>At {@code ItemStack.damage} rather than at the places that call it: mining, attacking, shearing,
 * flint and steel, a bow drawn, a mod's own tool - all of them arrive here, so stating it once covers
 * every one of them and anything added later.
 *
 * <p>The whole call is skipped, not just the breaking: a creative pickaxe that quietly accumulated
 * damage would be handed back to a player who left creative already half worn out.
 */
@Mixin(ItemStack.class)
public class CreativeNoDurabilityMixin {

	@Inject(method = "damage", at = @At("HEAD"), cancellable = true)
	private void retroapi$creativeSpendsNoDurability(int amount, Entity holder, CallbackInfo ci) {
		if (holder instanceof PlayerEntity player
			&& RetroGameModes.get(player) == RetroGameMode.CREATIVE) {
			ci.cancel();
		}
	}
}
