package com.periut.retrotweaks.feature.scoring.achievement;

import com.periut.retrotweaks.compat.ApiBridge;
import com.periut.retrotweaks.feature.scoring.Score;
import com.periut.retrotweaks.mixin.client.MinecraftAccessor;

import net.minecraft.achievement.Achievement;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.stat.Stats;

import java.time.Duration;

/**
 * "WAYS: Days" - WhatAreYouScoring's {@code WaysDaysAchievements}/{@code WaysDaysAchievementPage},
 * ported onto RetroAPI's registry. See {@link WaysBasicAchievements} for the general shape.
 *
 * <p>The two "real days played" milestones ({@link #real10Days}, {@link #real100Days}) are never
 * given a live description here, same as the source mod: they keep the static text from
 * {@code assets/retrotweaks/lang/en_US.lang} ("Play for 10/100 real days.") for their whole life.
 */
final class WaysDaysAchievements {

	private WaysDaysAchievements() {}

	static Achievement start;
	static Achievement minecraftDay;
	static Achievement minecraft100Days;
	static Achievement minecraftYear;
	static Achievement realDay;
	static Achievement real10Days;
	static Achievement real100Days;

	/** The registered page, kept so {@link ScoreAchievements#applyPageVisibility()} can hide or
	 *  show it long after registration - the setting is a screen toggle, not a launch flag. */
	static Object page;

	static void register() {
		page = ApiBridge.newAchievementPage("retrotweaks", "waysdays");
		if (page == null) return;

		start = ApiBridge.registerAchievement("retrotweaks", "start_days", "retrotweaks.start_days",
			0, 0, Item.BOOK, null);
		minecraftDay = ApiBridge.registerAchievement("retrotweaks", "minecraft_day", "retrotweaks.minecraft_day",
			0, 2, Item.IRON_INGOT, start);
		minecraft100Days = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "minecraft_100_days", "retrotweaks.minecraft_100_days", -2, 2, Item.GOLD_INGOT, minecraftDay));
		minecraftYear = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "minecraft_year", "retrotweaks.minecraft_year", -2, 0, Item.DIAMOND, minecraft100Days));
		realDay = ApiBridge.registerAchievement("retrotweaks", "real_day", "retrotweaks.real_day",
			0, -2, Block.IRON_BLOCK, start);
		real10Days = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "real_10_days", "retrotweaks.real_10_days", 2, -2, Block.GOLD_BLOCK, realDay));
		real100Days = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "real_100_days", "retrotweaks.real_100_days", 2, 0, Block.DIAMOND_BLOCK, real10Days));

		java.util.List<Achievement> present = new java.util.ArrayList<>();
		for (Achievement a : new Achievement[]{
				start, minecraftDay, minecraft100Days, minecraftYear, realDay, real10Days, real100Days }) {
			if (a != null) present.add(a);
		}
		ApiBridge.addAchievementsToPage(page, present.toArray(new Achievement[0]));
	}

	/** Mirrors WhatAreYouScoring's {@code WaysDaysAchievements.updateAchievementCounts()}. */
	static void refresh(PlayerEntity player, Score.Fields f, boolean showScore) {
		if (start == null) return;
		int daysSurvived = f.DAYS_PLAYED - f.LAST_DEATH_DAY;

		String startDesc = "Scores each in-game day survived (and tracks real world days).";
		if (showScore) {
			startDesc += "\n\nScore: " + ScoreAchievements.total(f.CurrentDaysScore, f.PrevCumulativeDaysScore);
		}
		ScoreAchievements.setDescription(start, startDesc);
		ScoreAchievements.setDescription(minecraftDay, "Current: " + daysSurvived);
		ScoreAchievements.setDescription(minecraft100Days, "Current: " + (daysSurvived / 100));
		ScoreAchievements.setDescription(minecraftYear, "Current: " + (daysSurvived / 365));

		long realDaysPlayed = realDaysPlayed();
		ScoreAchievements.setDescription(realDay, "Count: " + realDaysPlayed);
		// real10Days / real100Days keep their static lang description; see the class doc.

		ScoreAchievements.grantIf(player, start, true);
		ScoreAchievements.grantIf(player, minecraftDay, daysSurvived >= 1);
		ScoreAchievements.grantIf(player, minecraft100Days, daysSurvived >= 100);
		ScoreAchievements.grantIf(player, minecraftYear, daysSurvived >= 365);
		ScoreAchievements.grantIf(player, realDay, realDaysPlayed >= 1);
		ScoreAchievements.grantIf(player, real10Days, realDaysPlayed >= 10);
		ScoreAchievements.grantIf(player, real100Days, realDaysPlayed >= 100);
	}

	/** Real-world time played, from the client's own {@code Stats.PLAY_ONE_MINUTE} - client only,
	 *  same as the source mod's {@code ClientModHelper}; {@link #refresh} is only ever reached from
	 *  the (client-only) achievements screen. */
	private static long realDaysPlayed() {
		Minecraft minecraft = MinecraftAccessor.retrotweaks$getInstance();
		if (minecraft == null || minecraft.stats == null) return 0;
		return Duration.ofSeconds(minecraft.stats.get(Stats.PLAY_ONE_MINUTE) / 20L).toDays();
	}
}
