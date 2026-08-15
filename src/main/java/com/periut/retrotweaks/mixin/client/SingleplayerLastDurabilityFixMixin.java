package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.block.Block;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SingleplayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client half of the last durability point fix, for offline singleplayer: unlike real multiplayer and
 * hosted/LAN games (see the server-side {@code LastDurabilityFixMixin}), a plain singleplayer world
 * never touches {@code ServerPlayerInteractionManager} at all - {@link SingleplayerInteractionManager}
 * decides drops by itself. From UniTweaks
 * (mixin/bugfixes/lastdurabilityfix/SingleplayerInteractionManagerMixin).
 *
 * <p>Extends {@code InteractionManager} rather than {@code @Shadow}-ing {@code minecraft}: that field
 * is declared {@code protected final} on the superclass, and {@code @Shadow} only resolves members the
 * target class declares itself - same inherited-member trap as {@code SingleplayerWoodenSlabMiningFixMixin}.
 */
@Mixin(SingleplayerInteractionManager.class)
public abstract class SingleplayerLastDurabilityFixMixin extends InteractionManager {

	private SingleplayerLastDurabilityFixMixin(Minecraft minecraft) {
		super(minecraft);
	}

	@Unique
	private boolean retrotweaks$afterBreakHandled = false;

	@Inject(method = "breakBlock", at = @At(value = "HEAD"))
	private void retrotweaks$resetFlag(int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir) {
		retrotweaks$afterBreakHandled = false;
	}

	@Inject(method = "breakBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;onRemoved(Lnet/minecraft/entity/player/PlayerEntity;)V", shift = At.Shift.BEFORE))
	private void retrotweaks$callAfterBreak(int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir,
			@Local(ordinal = 4) int blockId, @Local(ordinal = 5) int blockMeta, @Local(ordinal = 6) int breakResult, @Local(ordinal = 7) int canHarvestResult) {
		if (!Config.BUGFIXES.lastDurabilityFix) {
			return;
		}

		if (breakResult == 1 && canHarvestResult == 1) {
			Block.BLOCKS[blockId].afterBreak(this.minecraft.world, this.minecraft.player, x, y, z, blockMeta);
			retrotweaks$afterBreakHandled = true;
		}
	}

	@WrapWithCondition(method = "breakBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;afterBreak(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;IIII)V"))
	private boolean retrotweaks$cancelAfterBreak(Block instance, World world, PlayerEntity player, int x, int y, int z, int meta) {
		if (!Config.BUGFIXES.lastDurabilityFix) {
			return true;
		}

		return !retrotweaks$afterBreakHandled;
	}
}
