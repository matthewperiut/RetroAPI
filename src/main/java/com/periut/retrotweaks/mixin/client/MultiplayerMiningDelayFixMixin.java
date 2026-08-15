package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MultiplayerInteractionManager;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Multiplayer mining delay fix: on real multiplayer, {@code processBlockBreakingAction} only scores
 * mining progress while {@code breakingBlock} is true, which vanilla sets with a random delay before
 * the first hit lands. Forcing the flag true removes that delay. From UniTweaks
 * (bugfixes/miningdelayfix/MultiplayerInteractionManagerMixin).
 *
 * <p>Some servers may treat the removed delay as a hack, hence the toggle defaulting off.
 */
@Mixin(MultiplayerInteractionManager.class)
public class MultiplayerMiningDelayFixMixin extends InteractionManager {

	public MultiplayerMiningDelayFixMixin(Minecraft minecraft) {
		super(minecraft);
	}

	@SuppressWarnings("SimplifiableConditionalExpression")
	@WrapOperation(method = "processBlockBreakingAction",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/MultiplayerInteractionManager;breakingBlock:Z", opcode = Opcodes.GETFIELD, ordinal = 0))
	private boolean retrotweaks$removeRandomDelay(MultiplayerInteractionManager instance, Operation<Boolean> original) {
		return Config.BUGFIXES.miningDelayFix ? true : original.call(instance);
	}
}
