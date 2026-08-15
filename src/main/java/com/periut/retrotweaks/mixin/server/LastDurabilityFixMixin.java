package com.periut.retrotweaks.mixin.server;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Last durability point fix: normally {@code tryBreakBlock} calls {@code ItemStack.onRemoved} (which
 * can destroy the tool) before {@code Block.afterBreak} (which drops the block, and for some blocks
 * checks the tool that is doing the harvesting). If the tool breaks on that exact swing, its item is
 * gone by the time {@code afterBreak} looks for it, and the block fails to drop. The fix runs
 * {@code afterBreak} once, early, right before {@code onRemoved} instead of after, then cancels the
 * later vanilla call to avoid dropping the block twice. From UniTweaks
 * (mixin/bugfixes/lastdurabilityfix/ServerPlayerInteractionManagerMixin).
 *
 * <p>This is the server half, authoritative for real multiplayer and any hosted/LAN game; see
 * {@code SingleplayerLastDurabilityFixMixin} for the client half offline singleplayer uses instead.
 */
@Mixin(ServerPlayerInteractionManager.class)
public class LastDurabilityFixMixin {
	@Shadow
	public PlayerEntity player;
	@Shadow
	private ServerWorld world;

	@Unique
	private boolean retrotweaks$afterBreakHandled = false;

	@Inject(method = "tryBreakBlock", at = @At(value = "HEAD"))
	private void retrotweaks$resetFlag(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		retrotweaks$afterBreakHandled = false;
	}

	@Inject(method = "tryBreakBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;onRemoved(Lnet/minecraft/entity/player/PlayerEntity;)V", shift = At.Shift.BEFORE))
	private void retrotweaks$callAfterBreak(int x, int y, int z, CallbackInfoReturnable<Boolean> cir,
			@Local(ordinal = 3) int blockId, @Local(ordinal = 4) int blockMeta, @Local(ordinal = 5) int finishMiningResult) {
		if (!Config.BUGFIXES.lastDurabilityFix) {
			return;
		}

		if (finishMiningResult == 1 && this.player.canHarvest(Block.BLOCKS[blockId])) {
			Block.BLOCKS[blockId].afterBreak(this.world, this.player, x, y, z, blockMeta);
			retrotweaks$afterBreakHandled = true;
		}
	}

	@WrapWithCondition(method = "tryBreakBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;IIII)V"))
	private boolean retrotweaks$cancelAfterBreak(Block instance, World world, PlayerEntity player, int x, int y, int z, int meta) {
		if (!Config.BUGFIXES.lastDurabilityFix) {
			return true;
		}

		return !retrotweaks$afterBreakHandled;
	}
}
