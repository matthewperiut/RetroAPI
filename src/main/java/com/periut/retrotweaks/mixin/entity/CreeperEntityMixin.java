/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.scoring.Score;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin {

    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/mob/CreeperEntity;isCharged()Z"
            )
    )
    public void retrotweaks$tryAttackBeforeCreateExplosion(Entity other, float distance, CallbackInfo ci) {
        if (Config.SCORING.challenge404.enabled) {
            Score.ofLocalPlayer().DETECT_CREEPER_EXPLOSION++;
        }
    }

}
