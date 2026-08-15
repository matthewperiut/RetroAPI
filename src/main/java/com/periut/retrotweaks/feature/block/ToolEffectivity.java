package com.periut.retrotweaks.feature.block;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.mixin.block.ToolItemAccessor;

import net.minecraft.block.Block;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;

/**
 * The blocks vanilla forgot to list as tool-appropriate. From AnnoyanceFix and UniTweaks.
 *
 * <p>Both source mods rewrote {@code AxeItem.axeEffectiveBlocks} and friends from inside the class
 * initialiser, which meant injecting into {@code <clinit>} at a specific field write. That is a
 * fragile place to inject and it fixed the list at startup, so a config change needed a restart.
 * Asking the question at mining time instead is a plain comparison on a hot-but-cheap path, costs
 * nothing measurable, and lets the options take effect immediately.
 *
 * <p>The pressure plate entries are swapped relative to AnnoyanceFix, which had axes listed against
 * the stone plate and pickaxes against the wooden one.
 */
public final class ToolEffectivity {

	private ToolEffectivity() {}

	/** True when {@code tool} should mine {@code block} at tool speed even though vanilla says no. */
	public static boolean isExtraEffective(Item tool, Block block) {
		if (block == null) return false;
		if (tool instanceof AxeItem) return axe(block);
		if (tool instanceof PickaxeItem) return pickaxe(block);
		if (tool instanceof ShovelItem) return shovel(block);
		return false;
	}

	private static boolean axe(Block block) {
		Config.AxeEffectivity axes = Config.BLOCKS.axes;
		if (block == Block.CRAFTING_TABLE) return axes.workbench;
		if (block == Block.NOTE_BLOCK) return axes.noteblock;
		if (block == Block.DOOR) return axes.woodDoor;
		if (block == Block.LADDER) return axes.ladders;
		if (block == Block.SIGN || block == Block.WALL_SIGN) return axes.signs;
		if (block == Block.WOODEN_PRESSURE_PLATE) return axes.woodPressurePlate;
		if (block == Block.JUKEBOX) return axes.jukebox;
		if (block == Block.WOODEN_STAIRS) return axes.woodStairs;
		if (block == Block.FENCE) return axes.fence;
		if (block == Block.PUMPKIN) return axes.pumpkin;
		if (block == Block.JACK_O_LANTERN) return axes.jackOLantern;
		if (block == Block.TRAPDOOR) return axes.trapdoor;
		return false;
	}

	private static boolean pickaxe(Block block) {
		Config.PickaxeEffectivity pickaxes = Config.BLOCKS.pickaxes;
		if (block == Block.DISPENSER) return pickaxes.dispenser;
		if (block == Block.RAIL) return pickaxes.normalRails;
		if (block == Block.DETECTOR_RAIL) return pickaxes.detectorRails;
		if (block == Block.POWERED_RAIL) return pickaxes.goldenRails;
		if (block == Block.FURNACE) return pickaxes.furnace;
		if (block == Block.LIT_FURNACE) return pickaxes.furnaceLit;
		if (block == Block.COBBLESTONE_STAIRS) return pickaxes.cobblestoneStairs;
		if (block == Block.STONE_PRESSURE_PLATE) return pickaxes.stonePressurePlate;
		if (block == Block.IRON_DOOR) return pickaxes.ironDoor;
		if (block == Block.REDSTONE_ORE) return pickaxes.redstoneOre;
		if (block == Block.LIT_REDSTONE_ORE) return pickaxes.redstoneOreLit;
		if (block == Block.BUTTON) return pickaxes.stoneButton;
		if (block == Block.BRICKS) return pickaxes.bricks;
		if (block == Block.SPAWNER) return pickaxes.mobSpawner;
		return false;
	}

	private static boolean shovel(Block block) {
		return block == Block.SOUL_SAND && Config.BLOCKS.shovels.soulSand;
	}

	/**
	 * True for the wooden slab: SLAB/DOUBLE_SLAB with meta 2. Stone/sandstone/cobble slabs (meta 0/1/3)
	 * share the same block id and are untouched. From AnnoyanceFix and UniTweaks
	 * (bugfixes/slabminingfix/MiningListener#isUsingEffectiveTool).
	 */
	public static boolean isWoodenSlab(Block block, int meta) {
		return Config.BUGFIXES.woodenSlabMiningFix && meta == 2 && (block == Block.SLAB || block == Block.DOUBLE_SLAB);
	}

	/**
	 * Mining speed for the held stack against a wooden slab: an axe gets its own speed, a pickaxe
	 * loses the stone-slab speed bonus it would otherwise inherit, anything else (including the bare
	 * hand) keeps whatever it would already get. From AnnoyanceFix and UniTweaks
	 * (bugfixes/slabminingfix/MiningListener#playerStrengthOnBlock).
	 */
	public static float woodenSlabSpeed(ItemStack stack, Block block) {
		if (stack == null) return 1.0F;
		Item item = stack.getItem();
		if (item instanceof AxeItem axe) return ((ToolItemAccessor) axe).retrotweaks$getMiningSpeed();
		if (item instanceof PickaxeItem) return 1.0F;
		return stack.getMiningSpeedMultiplier(block);
	}
}
