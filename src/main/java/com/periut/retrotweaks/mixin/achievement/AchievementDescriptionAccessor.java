package com.periut.retrotweaks.mixin.achievement;

import net.minecraft.achievement.Achievement;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes vanilla {@code Achievement}'s private {@code translationKey} field, which
 * {@code getTranslatedDescription()} returns verbatim whenever no {@code AchievementStatFormatter}
 * is set on the achievement (true of every achievement {@link
 * com.periut.retrotweaks.feature.scoring.achievement.ScoreAchievements} registers). Writing
 * to it live-updates the shown description with no translation file involved - the same trick
 * WhatAreYouScoring's own {@code AchievementAccessor} used against StationAPI's copy of this class.
 *
 * <p>Targets vanilla {@code Achievement} directly, so - unlike the RetroAPI bridge - this mixin
 * applies on every install; it simply has nothing to update when {@code ScoreAchievements} never
 * built any achievements (RetroAPI absent).
 */
@Mixin(Achievement.class)
public interface AchievementDescriptionAccessor {

	@Accessor("translationKey")
	void retrotweaks$setDescription(String description);
}
