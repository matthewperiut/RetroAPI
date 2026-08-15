package com.periut.retrotweaks.feature.scoring.achievement;

import com.periut.retrotweaks.compat.ApiBridge;
import com.periut.retrotweaks.feature.scoring.Score;

import net.minecraft.achievement.Achievement;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

/**
 * "WAYS: Basic" - WhatAreYouScoring's {@code WaysBasicAchievements}/{@code WaysBasicAchievementPage},
 * ported onto RetroAPI's registry. Every field is {@code null} until {@link #register()} runs (which
 * only happens when RetroAPI is present - see {@link com.periut.retrotweaks.feature.scoring.achievement.ScoreAchievements#init()}).
 */
final class WaysBasicAchievements {

	private WaysBasicAchievements() {}

	static Achievement start;
	static Achievement blocksPlaced;
	static Achievement blocksRemoved;
	static Achievement monsterKills;
	static Achievement passiveKills;

	/** The registered page, kept so {@link ScoreAchievements#applyPageVisibility()} can hide or
	 *  show it long after registration - the setting is a screen toggle, not a launch flag. */
	static Object page;

	static void register() {
		page = ApiBridge.newAchievementPage("retrotweaks", "waysbasic");
		if (page == null) return;

		start = ApiBridge.registerAchievement("retrotweaks", "start_basic", "retrotweaks.start_basic",
			0, 0, Item.BOOK, null);
		blocksPlaced = ApiBridge.registerAchievement("retrotweaks", "block_placed", "retrotweaks.block_placed",
			2, 0, Block.COBBLESTONE, start);
		blocksRemoved = ApiBridge.registerAchievement("retrotweaks", "block_removed", "retrotweaks.block_removed",
			0, 2, Block.DIRT, start);
		monsterKills = ApiBridge.registerAchievement("retrotweaks", "monster_mob_killed", "retrotweaks.monster_mob_killed",
			0, -2, Item.BONE, start);
		passiveKills = ApiBridge.registerAchievement("retrotweaks", "passive_mob_killed", "retrotweaks.passive_mob_killed",
			-2, 0, Item.LEATHER, start);

		java.util.List<Achievement> present = new java.util.ArrayList<>();
		for (Achievement a : new Achievement[]{ start, blocksPlaced, blocksRemoved, monsterKills, passiveKills }) {
			if (a != null) present.add(a);
		}
		ApiBridge.addAchievementsToPage(page, present.toArray(new Achievement[0]));
	}

	/** Mirrors WhatAreYouScoring's {@code WaysBasicAchievements.updateAchievementCounts()}. */
	static void refresh(PlayerEntity player, Score.Fields f, boolean showScore) {
		if (start == null) return;

		String startDesc = "Scores each block placed/removed and each mob killed.";
		if (showScore) {
			startDesc += "\n\nScore: " + ScoreAchievements.total(f.CurrentBasicScore, f.PrevCumulativeBasicScore);
		}
		ScoreAchievements.setDescription(start, startDesc);
		ScoreAchievements.setDescription(blocksPlaced, "Count: " + f.BLOCKS_PLACED);
		ScoreAchievements.setDescription(blocksRemoved, "Count: " + f.BLOCKS_REMOVED);
		ScoreAchievements.setDescription(monsterKills, "Count: " + f.MONSTER_MOBS_KILLED);
		ScoreAchievements.setDescription(passiveKills, "Count: " + f.PASSIVE_MOBS_KILLED);

		ScoreAchievements.grantIf(player, start, true);
		ScoreAchievements.grantIf(player, blocksPlaced, f.BLOCKS_PLACED > 0);
		ScoreAchievements.grantIf(player, blocksRemoved, f.BLOCKS_REMOVED > 0);
		ScoreAchievements.grantIf(player, monsterKills, f.MONSTER_MOBS_KILLED > 0);
		ScoreAchievements.grantIf(player, passiveKills, f.PASSIVE_MOBS_KILLED > 0);
	}
}
