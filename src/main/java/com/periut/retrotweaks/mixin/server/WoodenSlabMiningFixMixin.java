package com.periut.retrotweaks.mixin.server;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.block.ToolEffectivity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wooden slab mining fix: SLAB/DOUBLE_SLAB with meta 2 (the wood variant) is mineable by hand or axe
 * as if it were an effective tool, and gets an axe's own speed instead of the stone pickaxe bonus it
 * inherits from sharing a block id with the stone/sandstone/cobble slabs. From AnnoyanceFix and
 * UniTweaks (bugfixes/slabminingfix/MiningListener).
 *
 * <p>Both source mods fired this from StationAPI events ({@code IsPlayerUsingEffectiveToolEvent},
 * {@code PlayerStrengthOnBlockEvent}) that carry the mining position with them - StationAPI patches
 * the vanilla harvest/speed calls to attach it. RetroTweaks has no such event, so the position is
 * captured here instead: right before a block is turned to air in {@code tryBreakBlock} (for the
 * harvest check), and via a tiny {@code @Inject}-at-HEAD in each of the methods that score mining
 * progress (for the speed check), since those methods get x/y/z either as parameters or as this
 * class's own {@code miningX/Y/Z} fields depending on which one is running.
 *
 * <p>This is the server half, authoritative for real multiplayer and any hosted/LAN game; see
 * {@code SingleplayerWoodenSlabMiningFixMixin} for the client half offline singleplayer uses instead.
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class WoodenSlabMiningFixMixin {

	@Shadow
	private ServerWorld world;

	@Shadow
	private int miningX;

	@Shadow
	private int miningY;

	@Shadow
	private int miningZ;

	@Unique
	private int retrotweaks$breakingMeta;

	@Unique
	private int retrotweaks$targetX;

	@Unique
	private int retrotweaks$targetY;

	@Unique
	private int retrotweaks$targetZ;

	@Inject(method = "tryBreakBlock", at = @At("HEAD"))
	private void retrotweaks$captureBreakingMeta(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		this.retrotweaks$breakingMeta = this.world.getBlockMeta(x, y, z);
	}

	@WrapOperation(method = "tryBreakBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;canHarvest(Lnet/minecraft/block/Block;)Z"))
	private boolean retrotweaks$woodenSlabHarvest(PlayerEntity player, Block block, Operation<Boolean> original) {
		if (original.call(player, block)) return true;
		if (!Config.BUGFIXES.woodenSlabMiningFix) return false;
		return ToolEffectivity.isWoodenSlab(block, this.retrotweaks$breakingMeta);
	}

	@Inject(method = "onBlockBreakingAction", at = @At("HEAD"))
	private void retrotweaks$captureStartPos(int x, int y, int z, int direction, CallbackInfo ci) {
		this.retrotweaks$targetX = x;
		this.retrotweaks$targetY = y;
		this.retrotweaks$targetZ = z;
	}

	@Inject(method = "continueMining", at = @At("HEAD"))
	private void retrotweaks$captureContinuePos(int x, int y, int z, CallbackInfo ci) {
		this.retrotweaks$targetX = x;
		this.retrotweaks$targetY = y;
		this.retrotweaks$targetZ = z;
	}

	@WrapOperation(method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retrotweaks$speedUpdate(Block block, PlayerEntity player, Operation<Float> original) {
		return retrotweaks$fixSpeed(block, player, this.miningX, this.miningY, this.miningZ, original.call(block, player));
	}

	@WrapOperation(method = "onBlockBreakingAction",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retrotweaks$speedStart(Block block, PlayerEntity player, Operation<Float> original) {
		return retrotweaks$fixSpeed(block, player, this.retrotweaks$targetX, this.retrotweaks$targetY, this.retrotweaks$targetZ, original.call(block, player));
	}

	@WrapOperation(method = "continueMining",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retrotweaks$speedContinue(Block block, PlayerEntity player, Operation<Float> original) {
		return retrotweaks$fixSpeed(block, player, this.retrotweaks$targetX, this.retrotweaks$targetY, this.retrotweaks$targetZ, original.call(block, player));
	}

	@Unique
	private float retrotweaks$fixSpeed(Block block, PlayerEntity player, int x, int y, int z, float original) {
		if (!Config.BUGFIXES.woodenSlabMiningFix) return original;
		int meta = this.world.getBlockMeta(x, y, z);
		if (!ToolEffectivity.isWoodenSlab(block, meta)) return original;
		float hardness = block.getHardness();
		if (hardness < 0.0F) return original;
		ItemStack stack = player.inventory.getSelectedItem();
		float baseSpeed = ToolEffectivity.woodenSlabSpeed(stack, block);
		float envMultiplier = 1.0F;
		if (player.isInFluid(Material.WATER)) envMultiplier /= 5.0F;
		if (!player.onGround) envMultiplier /= 5.0F;
		return baseSpeed * envMultiplier / hardness / 30.0F;
	}
}
