package com.periut.retrotweaks.feature.scoring.achievement;

import com.periut.retrotweaks.compat.ApiBridge;
import com.periut.retrotweaks.feature.scoring.Score;

import net.minecraft.achievement.Achievement;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

/**
 * "WAYS: 404" - WhatAreYouScoring's {@code Ways404Achievements}/{@code Ways404AchievementPage},
 * ported onto RetroAPI's registry. See {@link WaysBasicAchievements} for the general shape; this
 * page just has many more leaves, one per 404-challenge scoring rule in {@link Score#calculate404ChallengeScore}.
 *
 * <p>{@link #neverSleep} and {@link #neverWearArmor} are granted the moment the tracked event fires
 * at all (sleeping, wearing armour) - the opposite of what wins the challenge - matching the source
 * mod's own {@code BedBlockMixin}/{@code PlayerInventoryMixin}; see {@link ScoreAchievements#grantIf}.
 */
final class Ways404Achievements {

	private Ways404Achievements() {}

	static Achievement start;
	static Achievement craftBowAndArrows, craftBread, craftJackOLantern;
	static Achievement leatherArmor, ironArmor, goldArmor, diamondArmor;
	static Achievement craftLapisLazuliBlock, craftIronBlock, craftGoldBlock, craftDiamondBlock;
	static Achievement creeperLapisLazuliBlock, creeperIronBlock, creeperGoldBlock, creeperDiamondBlock;
	static Achievement neverSleep, neverWearArmor;
	static Achievement zombieKilled, skeletonKilled, spiderKilled, creeperKilled, zombiePigmanKilled, ghastKilled;
	static Achievement place8TypesOfWool, placeAllTypesOfWool, place32Glass, place20Bricks, placeACrashSlab;
	static Achievement break20SugarCanes, break32Cacti, break15Wheat, break3Pumpkins, breakAnObsidianBlock;
	static Achievement exitTheNether, placeGlowstoneInTheOverworld;

	/** The registered page, kept so {@link ScoreAchievements#applyPageVisibility()} can hide or
	 *  show it long after registration - the setting is a screen toggle, not a launch flag. */
	static Object page;

	static void register() {
		page = ApiBridge.newAchievementPage("retrotweaks", "ways404");
		if (page == null) return;

		start = ApiBridge.registerAchievement("retrotweaks", "start_404", "retrotweaks.start_404", 0, 0, Item.BOOK, null);

		craftBowAndArrows = ApiBridge.registerAchievement("retrotweaks", "craft_bow_and_arrows", "retrotweaks.craft_bow_and_arrows", 2, -1, Item.BOW, start);
		craftBread = ApiBridge.registerAchievement("retrotweaks", "craft_bread", "retrotweaks.craft_bread", 2, -2, Item.BREAD, start);
		craftJackOLantern = ApiBridge.registerAchievement("retrotweaks", "craft_jack_o_lantern", "retrotweaks.craft_jack_o_lantern", 2, -3, Block.JACK_O_LANTERN, start);

		leatherArmor = ApiBridge.registerAchievement("retrotweaks", "leather_armor", "retrotweaks.leather_armor", 3, 0, Item.LEATHER_CHESTPLATE, start);
		ironArmor = ApiBridge.registerAchievement("retrotweaks", "iron_armor", "retrotweaks.iron_armor", 3, -1, Item.IRON_CHESTPLATE, start);
		goldArmor = ApiBridge.registerAchievement("retrotweaks", "gold_armor", "retrotweaks.gold_armor", 3, -2, Item.GOLDEN_CHESTPLATE, start);
		diamondArmor = ApiBridge.registerAchievement("retrotweaks", "diamond_armor", "retrotweaks.diamond_armor", 3, -3, Item.DIAMOND_CHESTPLATE, start);

		craftLapisLazuliBlock = ApiBridge.registerAchievement("retrotweaks", "craft_lapis_lazuli_block", "retrotweaks.craft_lapis_lazuli_block", 4, 0, Block.LAPIS_BLOCK, start);
		craftIronBlock = ApiBridge.registerAchievement("retrotweaks", "craft_iron_block", "retrotweaks.craft_iron_block", 4, -1, Block.IRON_BLOCK, start);
		craftGoldBlock = ApiBridge.registerAchievement("retrotweaks", "craft_gold_block", "retrotweaks.craft_gold_block", 4, -2, Block.GOLD_BLOCK, start);
		craftDiamondBlock = ApiBridge.registerAchievement("retrotweaks", "craft_diamond_block", "retrotweaks.craft_diamond_block", 4, -3, Block.DIAMOND_BLOCK, start);

		creeperLapisLazuliBlock = ApiBridge.registerAchievement("retrotweaks", "creeper_lapis_lazuli_block", "retrotweaks.creeper_lapis_lazuli_block", 5, 0, Item.DYE, craftLapisLazuliBlock);
		creeperIronBlock = ApiBridge.registerAchievement("retrotweaks", "creeper_iron_block", "retrotweaks.creeper_iron_block", 5, -1, Item.IRON_INGOT, craftIronBlock);
		creeperGoldBlock = ApiBridge.registerAchievement("retrotweaks", "creeper_gold_block", "retrotweaks.creeper_gold_block", 5, -2, Item.GOLD_INGOT, craftGoldBlock);
		creeperDiamondBlock = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "creeper_diamond_block", "retrotweaks.creeper_diamond_block", 5, -3, Item.DIAMOND, craftDiamondBlock));

		neverSleep = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "never_sleep", "retrotweaks.never_sleep", 4, 2, Item.BED, start));
		neverWearArmor = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "never_wear_armor", "retrotweaks.never_wear_armor", 5, 2, Item.CHAIN_CHESTPLATE, start));

		zombieKilled = ApiBridge.registerAchievement("retrotweaks", "zombie_killed", "retrotweaks.zombie_killed", 2, 2, Item.FEATHER, start);
		skeletonKilled = ApiBridge.registerAchievement("retrotweaks", "skeleton_killed", "retrotweaks.skeleton_killed", 1, 2, Item.BONE, start);
		spiderKilled = ApiBridge.registerAchievement("retrotweaks", "spider_killed", "retrotweaks.spider_killed", 0, 2, Item.STRING, start);
		creeperKilled = ApiBridge.registerAchievement("retrotweaks", "creeper_killed", "retrotweaks.creeper_killed", -1, 2, Item.GUNPOWDER, start);
		zombiePigmanKilled = ApiBridge.registerAchievement("retrotweaks", "zombie_pigman_killed", "retrotweaks.zombie_pigman_killed", -2, 2, Item.COOKED_PORKCHOP, start);
		ghastKilled = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "ghast_killed", "retrotweaks.ghast_killed", -3, 2, Item.SNOWBALL, start));

		place8TypesOfWool = ApiBridge.registerAchievement("retrotweaks", "place_8_types_of_wool", "retrotweaks.place_8_types_of_wool", 0, -2, Block.WOOL, start);
		placeAllTypesOfWool = ApiBridge.registerAchievement("retrotweaks", "place_all_types_of_wool", "retrotweaks.place_all_types_of_wool", -1, -2, Block.ROSE, start);
		place32Glass = ApiBridge.registerAchievement("retrotweaks", "place_32_glass", "retrotweaks.place_32_glass", 0, -3, Block.GLASS, start);
		place20Bricks = ApiBridge.registerAchievement("retrotweaks", "place_20_bricks", "retrotweaks.place_20_bricks", -1, -3, Block.BRICKS, start);
		placeACrashSlab = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "place_a_crash_slab", "retrotweaks.place_a_crash_slab", -2, -3, Block.SLAB, start));

		break20SugarCanes = ApiBridge.registerAchievement("retrotweaks", "break_20_sugar_canes", "retrotweaks.break_20_sugar_canes", -2, 0, Item.SUGAR_CANE, start);
		break32Cacti = ApiBridge.registerAchievement("retrotweaks", "break_32_cacti", "retrotweaks.break_32_cacti", -2, -1, Block.CACTUS, start);
		break15Wheat = ApiBridge.registerAchievement("retrotweaks", "break_15_wheat", "retrotweaks.break_15_wheat", -2, -2, Item.WHEAT, start);
		break3Pumpkins = ApiBridge.registerAchievement("retrotweaks", "break_3_pumpkins", "retrotweaks.break_3_pumpkins", -3, 0, Block.PUMPKIN, start);
		breakAnObsidianBlock = ApiBridge.registerAchievement("retrotweaks", "break_an_obsidian_block", "retrotweaks.break_an_obsidian_block", -3, -1, Block.OBSIDIAN, start);

		exitTheNether = ApiBridge.registerAchievement("retrotweaks", "exit_the_nether", "retrotweaks.exit_the_nether", -3, -2, Block.NETHER_PORTAL, breakAnObsidianBlock);
		placeGlowstoneInTheOverworld = ScoreAchievements.challenge(ApiBridge.registerAchievement(
			"retrotweaks", "place_glowstone_in_the_overworld", "retrotweaks.place_glowstone_in_the_overworld", -3, -3, Block.GLOWSTONE, exitTheNether));

		java.util.List<Achievement> present = new java.util.ArrayList<>();
		for (Achievement a : new Achievement[]{
				start, craftBowAndArrows, craftBread, craftJackOLantern,
				leatherArmor, ironArmor, goldArmor, diamondArmor,
				craftLapisLazuliBlock, craftIronBlock, craftGoldBlock, craftDiamondBlock,
				creeperLapisLazuliBlock, creeperIronBlock, creeperGoldBlock, creeperDiamondBlock,
				neverSleep, neverWearArmor,
				zombieKilled, skeletonKilled, spiderKilled, creeperKilled, zombiePigmanKilled, ghastKilled,
				place8TypesOfWool, placeAllTypesOfWool, place32Glass, place20Bricks, placeACrashSlab,
				break20SugarCanes, break32Cacti, break15Wheat, break3Pumpkins, breakAnObsidianBlock,
				exitTheNether, placeGlowstoneInTheOverworld }) {
			if (a != null) present.add(a);
		}
		ApiBridge.addAchievementsToPage(page, present.toArray(new Achievement[0]));
	}

	/** Mirrors WhatAreYouScoring's {@code Ways404Achievements.updateAchievementCounts()}. */
	static void refresh(PlayerEntity player, Score.Fields f, boolean showScore) {
		if (start == null) return;

		String startDesc = "Scores certain aspects of the game for the 404 challenge.";
		if (showScore) {
			startDesc += "\n\nScore: " + ScoreAchievements.total(f.Current404Score, f.PrevCumulative404Score);
		}
		ScoreAchievements.setDescription(start, startDesc);
		ScoreAchievements.grantIf(player, start, true);

		ScoreAchievements.setDescription(zombieKilled, "Count: " + f.ZOMBIE_KILLED);
		ScoreAchievements.setDescription(skeletonKilled, "Count: " + f.SKELETON_KILLED);
		ScoreAchievements.setDescription(spiderKilled, "Count: " + f.SPIDER_KILLED);
		ScoreAchievements.setDescription(creeperKilled, "Count: " + f.CREEPER_KILLED);
		ScoreAchievements.setDescription(ghastKilled, "Count: " + f.GHAST_KILLED);
		ScoreAchievements.setDescription(zombiePigmanKilled, "Count: " + f.ZOMBIE_PIGMAN_KILLED);
		ScoreAchievements.grantIf(player, zombieKilled, f.ZOMBIE_KILLED > 0);
		ScoreAchievements.grantIf(player, skeletonKilled, f.SKELETON_KILLED > 0);
		ScoreAchievements.grantIf(player, spiderKilled, f.SPIDER_KILLED > 0);
		ScoreAchievements.grantIf(player, creeperKilled, f.CREEPER_KILLED > 0);
		ScoreAchievements.grantIf(player, ghastKilled, f.GHAST_KILLED > 0);
		ScoreAchievements.grantIf(player, zombiePigmanKilled, f.ZOMBIE_PIGMAN_KILLED > 0);

		count(player, break15Wheat, "Break 15 wheat.", f.WHEAT_BROKEN, 15);
		count(player, break32Cacti, "Break 32 cacti.", f.CACTI_BROKEN, 32);
		count(player, break20SugarCanes, "Break 20 sugar canes.", f.SUGAR_CANES_BROKEN, 20);
		count(player, break3Pumpkins, "Break 3 pumpkins.", f.PUMPKINS_BROKEN, 3);
		count(player, place32Glass, "Place 32 glass.", f.GLASS_PLACED, 32);
		count(player, place20Bricks, "Place 20 bricks.", f.BRICKS_PLACED, 20);
		count(player, place8TypesOfWool, "Place 8 different wool types.", f.WOOL_TYPES_PLACED, 8);
		count(player, placeAllTypesOfWool, "Place all different wool types.", f.WOOL_TYPES_PLACED, 16);

		bit(player, craftBowAndArrows, "Craft a bow and 64 arrows.", 0xC0 == f.BOW_AND_ARROW_CRAFTING_BITFIELD);
		bit(player, craftLapisLazuliBlock, "Craft a lapiz lazuli block.", (0x0001 & f.MISC_CRAFTING_BITFIELD) != 0);
		bit(player, craftIronBlock, "Craft an iron block.", (0x0002 & f.MISC_CRAFTING_BITFIELD) != 0);
		bit(player, craftGoldBlock, "Craft a gold block.", (0x0004 & f.MISC_CRAFTING_BITFIELD) != 0);
		bit(player, craftDiamondBlock, "Craft a diamond block.", (0x0008 & f.MISC_CRAFTING_BITFIELD) != 0);
		bit(player, craftJackOLantern, "Craft a jack o' lantern.", (0x0010 & f.MISC_CRAFTING_BITFIELD) != 0);
		bit(player, craftBread, "Craft bread.", (0x0020 & f.MISC_CRAFTING_BITFIELD) != 0);

		bit(player, leatherArmor, "Craft a full set of leather armor.", (0x000F & f.ARMOR_CRAFTING_BITFIELD) == 0x000F);
		bit(player, ironArmor, "Craft a full set of iron armor.", (0x00F0 & f.ARMOR_CRAFTING_BITFIELD) == 0x00F0);
		bit(player, goldArmor, "Craft a full set of gold armor.", (0x0F00 & f.ARMOR_CRAFTING_BITFIELD) == 0x0F00);
		bit(player, diamondArmor, "Craft a full set of diamond armor.", (0xF000 & f.ARMOR_CRAFTING_BITFIELD) == 0xF000);

		bit(player, creeperIronBlock, "Have a creeper destroy an iron block.", (0x0001 & f.EXPLOSION_STATUS_BITFIELD) != 0);
		bit(player, creeperGoldBlock, "Have a creeper destroy a gold block.", (0x0002 & f.EXPLOSION_STATUS_BITFIELD) != 0);
		bit(player, creeperLapisLazuliBlock, "Have a creeper destroy a lapis lazuli block.", (0x0004 & f.EXPLOSION_STATUS_BITFIELD) != 0);
		bit(player, creeperDiamondBlock, "Have a creeper destroy a diamond block.", (0x0008 & f.EXPLOSION_STATUS_BITFIELD) != 0);

		boolean slept = (Score.Fields.HAS_PLAYER_SLEPT & f.OTHER_BITFIELD) != 0;
		ScoreAchievements.setDescription(neverSleep, "Never sleep. " + (slept ? "[Failed]" : "[Success!]"));
		ScoreAchievements.grantIf(player, neverSleep, slept);

		boolean wornArmor = (Score.Fields.HAS_PLAYER_WORN_ARMOR & f.OTHER_BITFIELD) != 0;
		ScoreAchievements.setDescription(neverWearArmor, "Never wear armor. " + (wornArmor ? "[Failed]" : "[Success!]"));
		ScoreAchievements.grantIf(player, neverWearArmor, wornArmor);

		bit(player, breakAnObsidianBlock, "Break an obsidian block.", (Score.Fields.HAS_OBSIDIAN_BEEN_BROKEN & f.OTHER_BITFIELD) != 0);
		bit(player, exitTheNether, "Exit the nether.", (Score.Fields.HAS_PLAYER_EXITED_THE_NETHER & f.OTHER_BITFIELD) != 0);
		bit(player, placeGlowstoneInTheOverworld, "Place a glowstone block in the overworld.", (Score.Fields.HAS_OVERWORLD_GLOWSTONE_BEEN_PLACED & f.OTHER_BITFIELD) != 0);
		bit(player, placeACrashSlab, "Place a crash slab.", (Score.Fields.HAS_CRASH_SLAB_BEEN_PLACED & f.OTHER_BITFIELD) != 0);
	}

	/** "<label> [Completed!]" / "<label> [Incomplete]" once {@code count} reaches {@code goal}. */
	private static void count(PlayerEntity player, Achievement achievement, String label, int count, int goal) {
		boolean done = count >= goal;
		ScoreAchievements.setDescription(achievement, label + (done ? " [Completed!]" : " [Incomplete]"));
		ScoreAchievements.grantIf(player, achievement, done);
	}

	/** Same as {@link #count}, for a flag that is already a plain boolean rather than a running count. */
	private static void bit(PlayerEntity player, Achievement achievement, String label, boolean done) {
		ScoreAchievements.setDescription(achievement, label + (done ? " [Completed!]" : " [Incomplete]"));
		ScoreAchievements.grantIf(player, achievement, done);
	}
}
