/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.block;

import com.periut.retrotweaks.feature.scoring.Score;
import com.periut.retrotweaks.util.Players;

import net.minecraft.block.CropBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(CropBlock.class)
public abstract class CropsBlockMixin extends PlantBlock {

	protected CropsBlockMixin(int id, int textureId) {
		super(id, textureId);
	}

	@Inject(method = "getDroppedItemId", at = @At("HEAD"))
	private void retrotweaks$getDroppedItemId(int blockMeta, Random random, CallbackInfoReturnable<Integer> cir) {
		if (blockMeta == 7) {
			PlayerEntity player = Players.local();
			if (null != player) {
				Score.Fields retrotweaks$s = Score.of(player);
				if (15 > retrotweaks$s.WHEAT_BROKEN) {
					retrotweaks$s.WHEAT_BROKEN++;
					if (15 == retrotweaks$s.WHEAT_BROKEN) {
						retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, player.world);
					}
				}
			}
		}
	}
}
