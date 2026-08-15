package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.block.ToolEffectivity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SingleplayerInteractionManager;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client half of the wooden slab mining fix, for offline singleplayer: unlike real multiplayer and
 * hosted/LAN games (see the server-side {@code WoodenSlabMiningFixMixin}), a plain singleplayer world
 * never touches {@code ServerPlayerInteractionManager} at all - {@link SingleplayerInteractionManager}
 * both decides drops and scores mining progress by itself. From AnnoyanceFix and UniTweaks
 * (bugfixes/slabminingfix/MiningListener).
 *
 * <p>Two things here are only discoverable from the bytecode, and both crashed the game on entering
 * a world - which is the first moment this class is loaded, and therefore long after the smoke test's
 * "client reached the title screen" marker:
 *
 * <ul>
 * <li>Extends {@code InteractionManager} rather than {@code @Shadow}-ing {@code minecraft}. That
 *     field is declared {@code protected final} on the superclass, and {@code @Shadow} only resolves
 *     members the target class declares itself. Same inherited-member trap as {@code @Inject} on an
 *     inherited method, same cure.
 * <li>The {@code canHarvest} wrap targets {@code ClientPlayerEntity}, not {@code PlayerEntity}. The
 *     method is declared on {@code PlayerEntity}, but {@code Minecraft.player} is typed
 *     {@code ClientPlayerEntity}, so javac emitted
 *     {@code invokevirtual ClientPlayerEntity.canHarvest} and an injector aimed at the declaring
 *     class matches nothing. The two {@code Block.getHardness} wraps below are aimed at the
 *     declaring class correctly, because {@code Block.BLOCKS[i]} really is statically a
 *     {@code Block} - the owner is whatever the call site's static type is, not where the method
 *     lives.
 * </ul>
 */
@Mixin(SingleplayerInteractionManager.class)
public abstract class SingleplayerWoodenSlabMiningFixMixin extends InteractionManager {

	private SingleplayerWoodenSlabMiningFixMixin(Minecraft minecraft) {
		super(minecraft);
	}

	@Unique
	private int retrotweaks$breakingMeta;

	@Unique
	private int retrotweaks$targetX;

	@Unique
	private int retrotweaks$targetY;

	@Unique
	private int retrotweaks$targetZ;

	@Inject(method = "breakBlock", at = @At("HEAD"))
	private void retrotweaks$captureBreakingMeta(int x, int y, int z, int direction, CallbackInfoReturnable<Boolean> cir) {
		this.retrotweaks$breakingMeta = this.minecraft.world.getBlockMeta(x, y, z);
	}

	@WrapOperation(method = "breakBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ClientPlayerEntity;canHarvest(Lnet/minecraft/block/Block;)Z"))
	private boolean retrotweaks$woodenSlabHarvest(ClientPlayerEntity player, Block block, Operation<Boolean> original) {
		if (original.call(player, block)) return true;
		if (!Config.BUGFIXES.woodenSlabMiningFix) return false;
		return ToolEffectivity.isWoodenSlab(block, this.retrotweaks$breakingMeta);
	}

	@Inject(method = "attackBlock", at = @At("HEAD"))
	private void retrotweaks$captureAttackPos(int x, int y, int z, int direction, CallbackInfo ci) {
		this.retrotweaks$targetX = x;
		this.retrotweaks$targetY = y;
		this.retrotweaks$targetZ = z;
	}

	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"))
	private void retrotweaks$captureProgressPos(int x, int y, int z, int side, CallbackInfo ci) {
		this.retrotweaks$targetX = x;
		this.retrotweaks$targetY = y;
		this.retrotweaks$targetZ = z;
	}

	@WrapOperation(method = "attackBlock",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retrotweaks$speedAttack(Block block, PlayerEntity player, Operation<Float> original) {
		return retrotweaks$fixSpeed(block, player, original);
	}

	@WrapOperation(method = "processBlockBreakingAction",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getHardness(Lnet/minecraft/entity/player/PlayerEntity;)F"))
	private float retrotweaks$speedProgress(Block block, PlayerEntity player, Operation<Float> original) {
		return retrotweaks$fixSpeed(block, player, original);
	}

	@Unique
	private float retrotweaks$fixSpeed(Block block, PlayerEntity player, Operation<Float> original) {
		float value = original.call(block, player);
		if (!Config.BUGFIXES.woodenSlabMiningFix) return value;
		int meta = this.minecraft.world.getBlockMeta(this.retrotweaks$targetX, this.retrotweaks$targetY, this.retrotweaks$targetZ);
		if (!ToolEffectivity.isWoodenSlab(block, meta)) return value;
		float hardness = block.getHardness();
		if (hardness < 0.0F) return value;
		ItemStack stack = player.inventory.getSelectedItem();
		float baseSpeed = ToolEffectivity.woodenSlabSpeed(stack, block);
		float envMultiplier = 1.0F;
		if (player.isInFluid(Material.WATER)) envMultiplier /= 5.0F;
		if (!player.onGround) envMultiplier /= 5.0F;
		return baseSpeed * envMultiplier / hardness / 30.0F;
	}
}
