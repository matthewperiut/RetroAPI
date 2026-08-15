package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adventure and spectator may not place blocks.
 *
 * <p>Hung on the item's own use rather than on either interaction manager: there is one of those per
 * side and they disagree about method names, while every placement in the game - hand, dispenser,
 * command - funnels through here.
 */
@Mixin(net.minecraft.item.BlockItem.class)
public class ReadOnlyInteractionMixin {

	@Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$readOnlyPlacement(ItemStack stack, PlayerEntity player, World world,
			int x, int y, int z, int face, CallbackInfoReturnable<Boolean> cir) {
		if (player != null && RetroGameModes.get(player).isReadOnly()) {
			cir.setReturnValue(false);
		}
	}
}
