/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.scoring.Score;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryScoringMixin {

    @Shadow public ItemStack[] armor;

    @Shadow public PlayerEntity player;

    @Inject(method = "getTotalArmorDurability", at = @At("HEAD"))
    public void retrotweaks$getArmorDurability(CallbackInfoReturnable<Integer> cir) {
        if (Config.SCORING.challenge404.enabled) {
            Score.Fields retrotweaks$s = Score.of(this.player);
            if (retrotweaks$s.HAS_PLAYER_WORN_ARMOR != (retrotweaks$s.HAS_PLAYER_WORN_ARMOR & retrotweaks$s.OTHER_BITFIELD)) {
                for (int var4 = 0; var4 < this.armor.length; ++var4) {
                    if (this.armor[var4] != null && this.armor[var4].getItem() instanceof ArmorItem) {
                        retrotweaks$s.OTHER_BITFIELD |= retrotweaks$s.HAS_PLAYER_WORN_ARMOR;
                        retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
                    }
                }
            }
        }
    }
}
