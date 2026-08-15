package com.periut.retrotweaks.feature.scoring;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;


import net.minecraft.world.World;

/**
 * Score tracking and the sums that turn it into a number. Ported from WhatAreYouScoring.
 *
 * <p>Counters live in {@link Fields} and are saved with the player, so a score belongs to a world
 * rather than to the game session. The three scoring systems (basic, days survived and the 404
 * challenge) are independent and each can be switched off on its own.
 */
public final class Score {

	private Score() {}

	/**
	 * That player's counters. Never null for a real player; the cast is safe because the mixin
	 * that supplies the interface targets {@code PlayerEntity} itself.
	 */
	public static Fields of(net.minecraft.entity.player.PlayerEntity player) {
		return ((ScoreHolder) player).retrotweaks$score();
	}

	/** The local player's counters, or a throwaway empty set on a dedicated server. */
	public static Fields ofLocalPlayer() {
		net.minecraft.entity.player.PlayerEntity player = com.periut.retrotweaks.util.Players.local();
		return player == null ? EMPTY : of(player);
	}

	/** Stand-in so client-side display code never has to null-check. */
	private static final Fields EMPTY = new Fields();

    public static int calculateCombinedScore(Fields f, World world) {
        int combinedScore = 0;

        combinedScore += calculateBasicScore(f);
        combinedScore += calculateDaysScore(f);
        combinedScore += calculate404ChallengeScore(f, world);

        return combinedScore;
    }

    public static int calculateBasicScore(Fields f) {
        int basicScore = 0;

        basicScore += f.BLOCKS_PLACED;
        basicScore += f.BLOCKS_REMOVED;
        basicScore += f.MONSTER_MOBS_KILLED;
        basicScore += f.PASSIVE_MOBS_KILLED;

        return basicScore;
    }

    public static int calculateDaysScore(Fields f) {
        int daysScore = 0;

        int daysSurvived = (f.DAYS_PLAYED - f.LAST_DEATH_DAY);

        if (Config.SCORING.days.perDay) {
            daysScore += daysSurvived;
        }

        if (Config.SCORING.days.per100Days) {
            daysScore += ((daysSurvived / 100) * 25);
        }

        if (Config.SCORING.days.perYear) {
            daysScore += ((daysSurvived / 365) * 100);
        }

        return daysScore;
    }

    public static int calculate404ChallengeScore(Fields f, World world) {
        double challenge404Score = 0;

        if (Config.SCORING.challenge404.scoreMobKills) {
            challenge404Score += (0.5 * f.ZOMBIE_KILLED);
            challenge404Score += (1 * f.SKELETON_KILLED);
            challenge404Score += (1 * f.SPIDER_KILLED);
            challenge404Score += (2 * f.CREEPER_KILLED);
            challenge404Score += (3 * f.ZOMBIE_PIGMAN_KILLED);
            challenge404Score += (6 * f.GHAST_KILLED);
        }

        if (15 <= f.WHEAT_BROKEN) {
            challenge404Score += 10;
        }

        if (32 <= f.CACTI_BROKEN) {
            challenge404Score += 4;
        }

        if (20 <= f.SUGAR_CANES_BROKEN) {
            challenge404Score += 4;
        }

        if (3 <= f.PUMPKINS_BROKEN) {
            challenge404Score += 2;
        }

        if (32 <= f.GLASS_PLACED) {
            challenge404Score += 5;
        }

        if (20 <= f.BRICKS_PLACED) {
            challenge404Score += 10;
        }

        if (8 <= f.WOOL_TYPES_PLACED) {
            challenge404Score += 10;
        }

        if (16 <= f.WOOL_TYPES_PLACED) {
            challenge404Score += 15;
        }

        if (0xC0 == f.BOW_AND_ARROW_CRAFTING_BITFIELD) {
            challenge404Score += 10;
        }

        if (0x0001 == (0x0001 & f.MISC_CRAFTING_BITFIELD)) {
            challenge404Score += 5;
        }

        if (0x0002 == (0x0002 & f.MISC_CRAFTING_BITFIELD)) {
            challenge404Score += 2;
        }

        if (0x0004 == (0x0004 & f.MISC_CRAFTING_BITFIELD)) {
            challenge404Score += 5;
        }

        if (0x0008 == (0x0008 & f.MISC_CRAFTING_BITFIELD)) {
            challenge404Score += 20;
        }

        if (0x0010 == (0x0010 & f.MISC_CRAFTING_BITFIELD)) {
            challenge404Score += 2;
        }

        if (0x0020 == (0x0020 & f.MISC_CRAFTING_BITFIELD)) {
            challenge404Score += 4;
        }

        if (0x000F == (0x000F & f.ARMOR_CRAFTING_BITFIELD)) {
            challenge404Score += 10;
        }

        if (0x00F0 == (0x00F0 & f.ARMOR_CRAFTING_BITFIELD)) {
            challenge404Score += 10;
        }

        if (0x0F00 == (0x0F00 & f.ARMOR_CRAFTING_BITFIELD)) {
            challenge404Score += 20;
        }

        if (0xF000 == (0xF000 & f.ARMOR_CRAFTING_BITFIELD)) {
            challenge404Score += 50;
        }

        if (0x0001 == (0x0001 & f.EXPLOSION_STATUS_BITFIELD)) {
            challenge404Score += 10;
        }

        if (0x0002 == (0x0002 & f.EXPLOSION_STATUS_BITFIELD)) {
            challenge404Score += 10;
        }

        if (0x0004 == (0x0004 & f.EXPLOSION_STATUS_BITFIELD)) {
            challenge404Score += 10;
        }

        if (0x0008 == (0x0008 & f.EXPLOSION_STATUS_BITFIELD)) {
            challenge404Score += 20;
        }

        if (Fields.HAS_PLAYER_SLEPT != (Fields.HAS_PLAYER_SLEPT & f.OTHER_BITFIELD)) {
            if (true == Config.SCORING.challenge404.positiveNeverSleepWearArmor) {
                challenge404Score += 15;
            }
        } else {
            if (false == Config.SCORING.challenge404.positiveNeverSleepWearArmor) {
                challenge404Score -= 15;
            }
        }

        if (Fields.HAS_PLAYER_WORN_ARMOR != (Fields.HAS_PLAYER_WORN_ARMOR & f.OTHER_BITFIELD)) {
            if (true == Config.SCORING.challenge404.positiveNeverSleepWearArmor) {
                challenge404Score += 15;
            }
        } else {
            if (false == Config.SCORING.challenge404.positiveNeverSleepWearArmor) {
                challenge404Score -= 15;
            }
        }

        if (Fields.HAS_OBSIDIAN_BEEN_BROKEN == (Fields.HAS_OBSIDIAN_BEEN_BROKEN & f.OTHER_BITFIELD)) {
            challenge404Score += 5;
        }

        if (Fields.HAS_PLAYER_EXITED_THE_NETHER == (Fields.HAS_PLAYER_EXITED_THE_NETHER & f.OTHER_BITFIELD)) {
            challenge404Score += 15;
        }

        if (Fields.HAS_OVERWORLD_GLOWSTONE_BEEN_PLACED == (Fields.HAS_OVERWORLD_GLOWSTONE_BEEN_PLACED & f.OTHER_BITFIELD)) {
            challenge404Score += 10;
        }

        if (Fields.HAS_CRASH_SLAB_BEEN_PLACED == (Fields.HAS_CRASH_SLAB_BEEN_PLACED & f.OTHER_BITFIELD)) {
            challenge404Score += 15;
        }

        if (Config.SCORING.challenge404.hardModeMultiplier) {
            if (null != world && 3 <= world.difficulty) {
                challenge404Score *= 1.5;
            }
        }

        return (int)Math.round(challenge404Score);
    }

    public static void resetFieldsOnDeath(Fields f, World world, boolean isWorldLoad) {
        /** - Reset Basic Score Fields */
        if (isWorldLoad) {
            f.CumulativeBasicScore = 0;
            f.CurrentBasicScore = 0;
        } else {
            f.CurrentBasicScore = calculateBasicScore(f);
        }
        f.BLOCKS_PLACED = 0;
        f.BLOCKS_REMOVED = 0;
        f.MONSTER_MOBS_KILLED = 0;
        f.PASSIVE_MOBS_KILLED = 0;

        /** - Reset Days Score Fields */
        if (isWorldLoad) {
            f.CumulativeDaysScore = 0;
            f.CurrentDaysScore = 0;
            f.LAST_DEATH_DAY = 0;
            f.DAYS_PLAYED = 0;
            f.PREV_DAYS_PLAYED = 0;
        } else {
            f.CurrentDaysScore = calculateDaysScore(f);
            f.LAST_DEATH_DAY = (int) Math.floor(world.getProperties().getTime() / 24000);
        }

        /** - Reset 404 Challenge Score Fields */
        if (isWorldLoad) {
            f.Cumulative404Score = 0;
            f.Current404Score = 0;
        } else {
            f.Current404Score = calculate404ChallengeScore(f, world);
        }
        f.ZOMBIE_KILLED = 0;
        f.SKELETON_KILLED = 0;
        f.SPIDER_KILLED = 0;
        f.CREEPER_KILLED = 0;
        f.GHAST_KILLED = 0;
        f.ZOMBIE_PIGMAN_KILLED = 0;
        f.WHEAT_BROKEN = 0;
        f.CACTI_BROKEN = 0;
        f.SUGAR_CANES_BROKEN = 0;
        f.PUMPKINS_BROKEN = 0;
        f.GLASS_PLACED = 0;
        f.BRICKS_PLACED = 0;
        f.WOOL_TYPES_PLACED = 0;
        f.WOOL_PLACED_BITFIELD = 0;
        f.BOW_AND_ARROW_CRAFTING_BITFIELD = 0;
        f.MISC_CRAFTING_BITFIELD = 0;
        f.ARMOR_CRAFTING_BITFIELD = 0;
        f.EXPLOSION_STATUS_BITFIELD = 0;
        f.OTHER_BITFIELD = 0;
    }

    /**
	 * One player's score counters.
	 *
	 * <p>Instance state rather than statics: on a dedicated server every player has their own
	 * score, and the original mod's static counters meant all of them shared one - two players
	 * mining would inflate each other's score and one death would reset everybody.
	 */
	public static final class Fields {

		/** The counters as they are saved and loaded; see {@code PlayerScoringMixin}. */
        public Integer DETECT_CREEPER_EXPLOSION = 0;

        /** - Basic helper fields */
        public Integer PrevCumulativeBasicScore = 0;
        public Integer CumulativeBasicScore = 0;
        public Integer CurrentBasicScore = 0;
        public Integer BLOCKS_PLACED = 0;
        public Integer BLOCKS_REMOVED = 0;
        public Integer MONSTER_MOBS_KILLED = 0;
        public Integer PASSIVE_MOBS_KILLED = 0;

        /** - Days helper fields */
        public Integer PrevCumulativeDaysScore = 0;
        public Integer CumulativeDaysScore = 0;
        public Integer CurrentDaysScore = 0;
        public Integer LAST_DEATH_DAY = 0;
        public Integer DAYS_PLAYED = 0;
        public Integer PREV_DAYS_PLAYED = 0;

        /** - 404 Challenge helper fields */
        public Integer PrevCumulative404Score = 0;
        public Integer Cumulative404Score = 0;
        public Integer Current404Score = 0;
        public Integer ZOMBIE_KILLED = 0;
        public Integer SKELETON_KILLED = 0;
        public Integer SPIDER_KILLED = 0;
        public Integer CREEPER_KILLED = 0;
        public Integer GHAST_KILLED = 0;
        public Integer ZOMBIE_PIGMAN_KILLED = 0;
        public Integer WHEAT_BROKEN = 0;
        public Integer CACTI_BROKEN = 0;
        public Integer SUGAR_CANES_BROKEN = 0;
        public Integer PUMPKINS_BROKEN = 0;
        public Integer GLASS_PLACED = 0;
        public Integer BRICKS_PLACED = 0;
        public Integer WOOL_TYPES_PLACED = 0;
        public Integer WOOL_PLACED_BITFIELD = 0x0000;
        public Integer BOW_AND_ARROW_CRAFTING_BITFIELD = 0x0000;
        public Integer MISC_CRAFTING_BITFIELD = 0x0000;
        public Integer ARMOR_CRAFTING_BITFIELD = 0x0000;
        public Integer EXPLOSION_STATUS_BITFIELD = 0x0000;
        public Integer OTHER_BITFIELD = 0x0000;
        public static final Integer HAS_PLAYER_SLEPT = 0x0001;
        public static final Integer HAS_PLAYER_WORN_ARMOR = 0x0002;
        public static final Integer HAS_OBSIDIAN_BEEN_BROKEN = 0x0004;
        public static final Integer HAS_PLAYER_EXITED_THE_NETHER = 0x0008;
        public static final Integer HAS_OVERWORLD_GLOWSTONE_BEEN_PLACED = 0x0010;
        public static final Integer HAS_CRASH_SLAB_BEEN_PLACED = 0x0020;
        public Boolean IS_PLAYER_IN_NETHER = false;
    }
}
