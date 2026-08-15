package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Breaking a block in creative drops nothing, as in modern.
 *
 * <p>{@code Block.afterBreak} is the one method both sides run after a block is mined and is where
 * beta drops the items, so cancelling it there covers the client's own break path and the server's
 * {@code tryBreakBlock} alike - and leaves every other kind of break (explosions, {@code /setblock
 * destroy}, a piston) dropping normally, because none of them come through here with a player.
 */
@Mixin(Block.class)
public class CreativeNoDropsMixin {

	@Inject(method = "afterBreak", at = @At("HEAD"), cancellable = true)
	private void retroapi$noCreativeDrops(World world, PlayerEntity player, int x, int y, int z, int meta, CallbackInfo ci) {
		if (player != null && RetroGameModes.get(player) == RetroGameMode.CREATIVE) {
			ci.cancel();
		}
	}
}
