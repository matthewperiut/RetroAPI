package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.stat.PlayerStats;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Lets the stats file be read even when its checksum does not match. From MojangFix.
 *
 * <p>Vanilla hashes the stats file and throws the whole thing away if the hash is wrong. Any mod
 * that adds or reorders a stat changes that hash, so installing one silently wipes every statistic
 * the player has ever accumulated. Skipping the check keeps them.
 */
@Mixin(PlayerStats.class)
public class PlayerStatsChecksumMixin {

	@WrapOperation(method = "deserialize", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
	private static boolean retrotweaks$ignoreChecksum(String checksum, Object expected, Operation<Boolean> original) {
		return original.call(checksum, expected) || Config.BUGFIXES.disableStatsChecksumVerification;
	}
}
