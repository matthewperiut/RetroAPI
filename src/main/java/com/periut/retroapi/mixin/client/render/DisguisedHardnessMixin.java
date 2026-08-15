package com.periut.retroapi.mixin.client.render;

import com.periut.retroapi.mixin.client.InteractionManagerAccessor;
import com.periut.retroapi.register.block.RetroDisguises;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SingleplayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * How fast a disguised block gives way: the worn block's speed, not the frame's. See
 * {@link com.periut.retroapi.register.block.RetroBlockDisguise}.
 *
 * <p>{@code getHardness(PlayerEntity)} returns the fraction of the block broken this tick, and it folds
 * together the block's own hardness and how well the held tool bites it. Asking the worn block instead of
 * the frame gets both at once: a frame clad in stone takes as long as stone and rewards a pickaxe for it,
 * and one clad in wool comes apart as fast as wool.
 *
 * <p>Its own class, separate from the sound hook in the same method, because StationAPI replaces this call
 * and not that one, so the two have to be switchable independently. It is disabled under StationAPI by
 * {@code RetroAPIMixinPlugin} rather than made optional: an optional injection that silently stops
 * applying is how the break sound went unnoticed for a whole release.
 */
@Mixin(SingleplayerInteractionManager.class)
@Environment(EnvType.CLIENT)
public class DisguisedHardnessMixin {

	@Redirect(method = "processBlockBreakingAction",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retroapi$disguisedHardness(Block block, PlayerEntity player, int x, int y, int z, int side) {
		Minecraft minecraft = ((InteractionManagerAccessor) this).retroapi$minecraft();
		Block worn = minecraft == null || minecraft.world == null
			? null
			: RetroDisguises.at(minecraft.world, x, y, z);
		return worn == null ? block.getHardness(player) : RetroDisguises.breakingDelta(player, block, worn);
	}
}
