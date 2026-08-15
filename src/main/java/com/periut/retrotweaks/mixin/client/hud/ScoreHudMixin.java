/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 */
package com.periut.retrotweaks.mixin.client.hud;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.scoring.Score;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.ScreenScaler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class ScoreHudMixin extends DrawContext {

    @Shadow
    private Minecraft minecraft;

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/player/ClientPlayerEntity;getSleepTimer()I",
                    ordinal = 0
            ),
            cancellable = true, require = 0)
    public void retrotweaks$renderScore(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.minecraft.player.health > 0 && !this.minecraft.options.debugHud) {
            TextRenderer textRenderer = this.minecraft.textRenderer;
            ScreenScaler screenScaler = new ScreenScaler(this.minecraft.options, this.minecraft.displayWidth, this.minecraft.displayHeight);
            int centerX = screenScaler.getScaledWidth() / 2;
            int color = 0xFFFFFF;
            int y = 2;
            ArrayList<String> scoresToDisplay = new ArrayList<>();
            int totalBasicScore    = Score.ofLocalPlayer().CurrentBasicScore;
            int totalDaysScore     = Score.ofLocalPlayer().CurrentDaysScore;
            int total404Score      = Score.ofLocalPlayer().Current404Score;

            if (Config.SCORING.difficultyDeathMultiplier) {
                totalBasicScore    += Score.ofLocalPlayer().PrevCumulativeBasicScore;
                totalDaysScore     += Score.ofLocalPlayer().PrevCumulativeDaysScore;
                total404Score      += Score.ofLocalPlayer().PrevCumulative404Score;
            }

            if (Enums.ScoreDisplay.COMBINED == Config.SCORING.hudScoreDisplayMode) {
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

                this.drawCenteredTextWithShadow(textRenderer, "Total: §7" + totalCombinedScore, centerX, y, color);
            } else if (Enums.ScoreDisplay.LISTED == Config.SCORING.hudScoreDisplayMode) {
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
            } else if (Enums.ScoreDisplay.CUSTOM == Config.SCORING.hudScoreDisplayMode) {
                if (  Config.SCORING.basic.enabled
                   && Config.SCORING.customHudDisplay.basic
                ) {
                    scoresToDisplay.add("Score: §e" + totalBasicScore);
                }

                if (  Config.SCORING.days.enabled
                   && Config.SCORING.customHudDisplay.days
                ) {
                    scoresToDisplay.add("Days: §b" + totalDaysScore);
                }

                if (  Config.SCORING.challenge404.enabled
                   && Config.SCORING.customHudDisplay.challenge404
                ) {
                    scoresToDisplay.add("Challenge: §c" + total404Score);
                }

                retrotweaks$drawScores(textRenderer, scoresToDisplay, centerX, y, color);
            } else {
                /* Do nothing */
            }
        }
    }

    @Unique
    private void retrotweaks$drawScores(TextRenderer textRenderer, ArrayList<String> scoresToDisplay, int centerX, int y, int color) {
        if (2 < scoresToDisplay.size()) {
            this.drawCenteredTextWithShadow(
                    textRenderer,
                    scoresToDisplay.get(0) + "§7 - §f" + scoresToDisplay.get(1) + "§7 - §f" + scoresToDisplay.get(2),
                    centerX, y, color);
        } else if (1 < scoresToDisplay.size()) {
            this.drawCenteredTextWithShadow(
                    textRenderer,
                    scoresToDisplay.get(0) + "§7 - §f" + scoresToDisplay.get(1),
                    centerX, y, color);
        } else if (!scoresToDisplay.isEmpty()) {
            this.drawCenteredTextWithShadow(
                    textRenderer,
                    scoresToDisplay.get(0),
                    centerX, y, color);
        } else {
            /* Do nothing */
        }
    }
}
