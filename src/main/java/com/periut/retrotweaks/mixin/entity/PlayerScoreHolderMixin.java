package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.feature.scoring.Score;
import com.periut.retrotweaks.feature.scoring.ScoreHolder;

import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Gives every player their own score counters.
 *
 * <p>WhatAreYouScoring kept these in statics, which is invisible in singleplayer and wrong the
 * moment a second player joins: both would share one set of counters, so one player's mining would
 * raise the other's score and either one dying would reset both.
 */
@Mixin(PlayerEntity.class)
public class PlayerScoreHolderMixin implements ScoreHolder {

	@Unique
	private final Score.Fields retrotweaks$score = new Score.Fields();

	@Override
	public Score.Fields retrotweaks$score() {
		return retrotweaks$score;
	}
}
