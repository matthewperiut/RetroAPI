package com.periut.retroapi.mixin.gamemode;

import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Creative breaks everything in one tick, which is what modern means by creative.
 *
 * <p>Handing the player enormous hands was not enough, and could never have been: beta divides that
 * strength by the block's hardness AND by 100 rather than 30 when the player is not holding the right
 * tool, so obsidian, ores and every other hardness-times-wrong-tool block still took several ticks -
 * which is exactly the "instant break does not work on a lot of things" it looked like. Modern does
 * not scale anything here; {@code MultiPlayerGameMode} simply destroys the block when the player has
 * infinite materials, so the answer to "how far through is it" is "all of it", once, for any block.
 *
 * <p>Common code on purpose. The server works the progress out again from its own copy of this method
 * before it agrees a block is gone, so both sides have to reach the same answer or a creative player
 * would break blocks that came straight back.
 *
 * <p>Unbreakable blocks included - hardness below zero, which is bedrock and RetroAPI's own command
 * blocks. Modern creative breaks those too: its destroy path never consults hardness at all, it simply
 * removes the block when the player has infinite materials. Skipping them here is what left a creative
 * player unable to break a command block they had just placed.
 */
@Mixin(Block.class)
public class CreativeInstantBreakMixin {
	@Shadow protected float hardness;

	@Inject(method = "getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F",
		at = @At("HEAD"), cancellable = true)
	private void retroapi$creativeBreaksInstantly(PlayerEntity player, CallbackInfoReturnable<Float> cir) {
		if (player == null) {
			return;
		}
		if (RetroGameModes.get(player) == RetroGameMode.CREATIVE) {
			cir.setReturnValue(1.0F);
		}
	}
}
