/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.scoring.Score;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftScoringMixin {

    @Shadow public ClientPlayerEntity player;

    @Shadow
    public World world;

    @Inject(
            method = "startGame",
            at = @At("HEAD")
    )
    public void retrotweaks$createOrLoadWorld(String string, String string2, long l, CallbackInfo ci) {
        Score.resetFieldsOnDeath(Score.ofLocalPlayer(), null, true);
    }

    @Inject(
            method = "changeDimension",
            at = @At("HEAD"),
            cancellable = true
    )
    public void retrotweaks$changeDimension(CallbackInfo ci) {
        if (Config.SCORING.challenge404.enabled) {
            if (Score.ofLocalPlayer().HAS_PLAYER_EXITED_THE_NETHER != (Score.ofLocalPlayer().HAS_PLAYER_EXITED_THE_NETHER & Score.ofLocalPlayer().OTHER_BITFIELD)) {
                if (this.player.dimensionId == -1) {
                    Score.ofLocalPlayer().OTHER_BITFIELD |= Score.ofLocalPlayer().HAS_PLAYER_EXITED_THE_NETHER;
                    Score.ofLocalPlayer().Current404Score = Score.calculate404ChallengeScore(Score.ofLocalPlayer(), world);
                }
            }
        }
    }

    @Inject(
            method = "respawnPlayer",
            at = @At("RETURN"),
            cancellable = true
    )
    public void retrotweaks$respawnPlayer(boolean worldSpawn, int dimension, CallbackInfo ci) {
        Score.ofLocalPlayer().CurrentBasicScore = Score.calculateBasicScore(Score.ofLocalPlayer());
        Score.ofLocalPlayer().CurrentDaysScore = Score.calculateDaysScore(Score.ofLocalPlayer());
        Score.ofLocalPlayer().Current404Score = Score.calculate404ChallengeScore(Score.ofLocalPlayer(), world);
    }
}
