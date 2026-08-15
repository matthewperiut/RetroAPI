/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.scoring.Score;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
@Mixin(DeathScreen.class)
public class DeathScreenScoreMixin extends Screen {

    public DeathScreenScoreMixin() {
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/DeathScreen;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
                    ordinal = 1
            ), require = 0)
    private void retrotweaks$renderDeathScreenText(DeathScreen instance, TextRenderer textRenderer, String text, int centerX, int y, int color, Operation<Void> original) {
        ArrayList<String> scoresToDisplay = new ArrayList<>();
        int totalBasicScore    = Score.ofLocalPlayer().CurrentBasicScore;
        int totalDaysScore     = Score.ofLocalPlayer().CurrentDaysScore;
        int total404Score      = Score.ofLocalPlayer().Current404Score;

        if (Config.SCORING.difficultyDeathMultiplier) {
            totalBasicScore    += Score.ofLocalPlayer().PrevCumulativeBasicScore;
            totalDaysScore     += Score.ofLocalPlayer().PrevCumulativeDaysScore;
            total404Score      += Score.ofLocalPlayer().PrevCumulative404Score;
        }

        if (Enums.ScoreDisplay.COMBINED == Config.SCORING.deathScoreDisplayMode) {
            int totalCombinedScore = 0;

            if (Config.SCORING.basic.enabled) {
                totalCombinedScore += totalBasicScore;
            }

            if (Config.SCORING.days.enabled) {
                totalCombinedScore += totalDaysScore;
            }

            if (Config.SCORING.challenge404.enabled) {
                totalCombinedScore += total404Score;
            }

            instance.drawCenteredTextWithShadow(textRenderer, "Total: §7" + totalCombinedScore, centerX, y, color);
        } else if (Enums.ScoreDisplay.LISTED == Config.SCORING.deathScoreDisplayMode) {
            if (Config.SCORING.basic.enabled) {
                scoresToDisplay.add("Score: §e" + totalBasicScore);
            }

            if (Config.SCORING.days.enabled) {
                scoresToDisplay.add("Days: §b" + totalDaysScore);
            }

            if (Config.SCORING.challenge404.enabled) {
                scoresToDisplay.add("Challenge: §c" + total404Score);
            }

            retrotweaks$drawScores(textRenderer, scoresToDisplay, centerX, y, color);
        } else if (Enums.ScoreDisplay.CUSTOM == Config.SCORING.deathScoreDisplayMode) {
            if (  Config.SCORING.basic.enabled
               && Config.SCORING.customDeathDisplay.basic
            ) {
                scoresToDisplay.add("Score: §e" + totalBasicScore);
            }

            if (  Config.SCORING.days.enabled
               && Config.SCORING.customDeathDisplay.days
            ) {
                scoresToDisplay.add("Days: §b" + totalDaysScore);
            }

            if (  Config.SCORING.challenge404.enabled
               && Config.SCORING.customDeathDisplay.challenge404
            ) {
                scoresToDisplay.add("Challenge: §c" + total404Score);
            }

            retrotweaks$drawScores(textRenderer, scoresToDisplay, centerX, y, color);
        } else {
            original.call(instance, textRenderer, text, centerX, y, color);
        }
    }

    @Unique
    private void retrotweaks$drawScores(TextRenderer textRenderer, ArrayList<String> scoresToDisplay, int centerX, int y, int color) {
        if (2 < scoresToDisplay.size()) {
            this.drawCenteredTextWithShadow(textRenderer, scoresToDisplay.get(0), centerX, y - 10, color);
            this.drawCenteredTextWithShadow(textRenderer, scoresToDisplay.get(1), centerX, y, color);
            this.drawCenteredTextWithShadow(textRenderer, scoresToDisplay.get(2), centerX, y + 10, color);
        } else if (1 < scoresToDisplay.size()) {
            this.drawCenteredTextWithShadow(textRenderer, scoresToDisplay.get(0), centerX, y - 5, color);
            this.drawCenteredTextWithShadow(textRenderer, scoresToDisplay.get(1), centerX, y + 5, color);
        } else if (!scoresToDisplay.isEmpty()) {
            this.drawCenteredTextWithShadow(textRenderer, scoresToDisplay.get(0), centerX, y, color);
        } else {
            /* Do nothing */
        }
    }
}