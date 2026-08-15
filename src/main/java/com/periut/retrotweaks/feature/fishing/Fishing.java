package com.periut.retrotweaks.feature.fishing;

import com.periut.retrotweaks.RetroTweaks;
import com.periut.retrotweaks.compat.ApiBridge;
import com.periut.retrotweaks.config.Config;

import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Fish get a size, and the size decides how much they feed you. Ported from FishinFoodTweaks.
 *
 * <p>The size is stored in the stack's damage value, so it needs no new item and no registry: a
 * fish caught with RetroTweaks installed is still an ordinary fish to anything else.
 *
 * <p>Sizes come from a gamma distribution, which gives the long right tail a catch record wants -
 * most fish middling, the occasional monster. FishinFoodTweaks used Apache Commons Math for this;
 * the twenty lines of Marsaglia-Tsang below do the same job without the dependency.
 *
 * <p>FishinFoodTweaks also added four non-vanilla fish item types (common/river/sea/ocean, each raw
 * and cooked). Those need an item registry a vanilla install does not have; when RetroAPI is
 * installed it provides exactly that, so {@link #registerNonVanillaFish()} registers them through
 * {@link ApiBridge#registerRetroFoodItem}, and {@link #RAW}/{@link #COOKED} stay all-null (and
 * {@link Config.FishingAndFood#nonVanillaFish} stays forced off - see {@link
 * com.periut.retroapi.config.Opt#requires()}) when it is not.
 */
public final class Fishing {

	private Fishing() {}

	/** Largest ordinary fish. Oceanic fish go beyond this. */
	private static final int MAX_NORMAL_SIZE = 700;

	/** Water blocks that count as "open water" for the biggest fish. */
	private static final int OCEAN_WATER_BLOCKS = 250;

	/** How far the surface search will walk before it stops caring. */
	private static final int SURFACE_SEARCH_LIMIT = 350;

	// ================================================================ non-vanilla fish (RetroAPI)

	/**
	 * The four non-vanilla fish types, common/river/sea/ocean, in the order FishinFoodTweaks' own
	 * {@code Fish} class declared them - the same order {@link #applyNonVanillaType} and
	 * {@link #applyNonVanillaTypeOnly} roll a {@code fishType} index into. Every slot is {@code null}
	 * until {@link #registerNonVanillaFish()} has found RetroAPI and succeeded for all eight items;
	 * {@link #nonVanillaFishAvailable()} is the thing to check before reading these.
	 */
	public static final Item[] RAW = new Item[4];
	public static final Item[] COOKED = new Item[4];

	private static final String[] ITEM_NAMES = {"common_fish", "river_fish", "sea_fish", "ocean_fish"};
	/** Texture name prefixes, copied byte-for-byte from FishinFoodTweaks' own assets. */
	private static final String[] TEXTURE_NAMES = {"sepia", "salmon", "violet", "ocean"};

	private static boolean nonVanillaFishAvailable;

	/**
	 * Registers the four non-vanilla fish types (eight items: raw + cooked each) through RetroAPI,
	 * if it is installed. Called once, unconditionally, from {@link
	 * com.periut.retrotweaks.compat.retroapi.RetroTweaksRetroInitializer#initRetro()} - RetroAPI's
	 * own {@code retroapi} entrypoint, not the mod's plain {@code main} entry point, so this always
	 * runs after RetroAPI's item registry is actually ready. The items have to exist before {@link
	 * Config.FishingAndFood#nonVanillaFish} can ever do anything, and
	 * existing is not the same as being used: whether a caught fish actually becomes one of these is
	 * still decided per catch, by that same option, in {@link #applyNonVanillaType}.
	 *
	 * <p>Healing is size-scaled the same way {@code FoodItemMixin} scales the two vanilla fish: each
	 * item is registered with RetroFood health {@code 0} (a no-op) and does the real heal itself, in
	 * its {@code onEaten} effect, so the amount can depend on {@link Config.FishingAndFood#randomFishSizes}
	 * and the size written into the stack's damage value - RetroAPI's own eat hook has no way to
	 * express that from a fixed number.
	 */
	public static void registerNonVanillaFish() {
		if (!ApiBridge.hasRetroItemRegistry()) return;
		boolean allRegistered = true;
		for (int i = 0; i < ITEM_NAMES.length; i++) {
			String texture = TEXTURE_NAMES[i];
			Item raw = ApiBridge.registerRetroFoodItem("raw_" + ITEM_NAMES[i], texture + "_raw_fish", 1, 0,
				(stack, world, player) -> player.heal(healAmount(stack, 2)));
			Item cooked = ApiBridge.registerRetroFoodItem("cooked_" + ITEM_NAMES[i], texture + "_cooked_fish", 1, 0,
				(stack, world, player) -> player.heal(healAmount(stack, 5)));
			if (raw == null || cooked == null) {
				allRegistered = false;
				continue;
			}
			RAW[i] = raw;
			COOKED[i] = cooked;
		}
		nonVanillaFishAvailable = allRegistered;
		if (allRegistered) {
			RetroTweaks.LOGGER.info("RetroAPI detected - registered FishinFoodTweaks' four non-vanilla fish types.");
		} else {
			RetroTweaks.LOGGER.warn("RetroAPI is installed but registering the non-vanilla fish types did not "
				+ "fully succeed; they will not appear this session.");
		}
	}

	/** True once all eight non-vanilla fish items exist. See {@link #registerNonVanillaFish()}. */
	public static boolean nonVanillaFishAvailable() {
		return nonVanillaFishAvailable;
	}

	/** How much a fish of this size heals when {@link Config.FishingAndFood#randomFishSizes} is off. */
	private static int healAmount(ItemStack stack, int baseHeal) {
		return Config.FISHING.randomFishSizes ? healingFor(stack) : baseHeal;
	}

	/** Index into {@link #RAW}/{@link #COOKED} for a non-vanilla raw fish item id, or -1. */
	private static int nonVanillaRawIndex(int itemId) {
		for (int i = 0; i < RAW.length; i++) {
			if (RAW[i] != null && RAW[i].id == itemId) return i;
		}
		return -1;
	}

	private static int nonVanillaCookedIndex(int itemId) {
		for (int i = 0; i < COOKED.length; i++) {
			if (COOKED[i] != null && COOKED[i].id == itemId) return i;
		}
		return -1;
	}

	/** Which of the four non-vanilla species a raw fish id is, or -1 for vanilla RAW_FISH / anything
	 * else. Used by {@code FurnaceFishSizeMixin} to cook the right species back out. */
	public static int rawFishType(int itemId) {
		return nonVanillaRawIndex(itemId);
	}

	// ================================================================ catching

	/**
	 * Rolls a catch: a size (if {@link Config.FishingAndFood#randomFishSizes} is on) and, when the
	 * non-vanilla fish exist and {@link Config.FishingAndFood#nonVanillaFish} is on, a species -
	 * exactly FishinFoodTweaks' {@code fishinFoodTweaks_use}, split into named steps.
	 *
	 * @param caught the freshly spawned catch; its {@link ItemEntity#stack} may be replaced entirely
	 *               when a non-vanilla species is rolled, not just have its damage value set
	 * @param world  the world, used to measure the water around the bobber
	 */
	public static void rollCatch(ItemEntity caught, World world, double x, double y, double z) {
		ItemStack stack = caught.stack;
		if (stack == null) return;
		// Only a fresh vanilla catch is ours to roll; something else may already have changed it.
		if (stack.itemId != Item.RAW_FISH.id && stack.itemId != Item.COOKED_FISH.id) return;

		int water = measureWaterSurface(world, x, y, z);
		Random random = world.random;
		boolean nonVanilla = nonVanillaFishAvailable && Config.FISHING.nonVanillaFish;

		if (Config.FISHING.randomFishSizes) {
			double scale = 0.125 + water / 10000.0;
			SizeRoll roll = rollSize(random, scale, water);
			int size = roll.size;
			// A record-sized (990cm+) fish is always the vanilla species, non-vanilla fish or not -
			// FishinFoodTweaks only ever swaps the species for an ordinary-sized catch.
			if (nonVanilla && !roll.special) {
				size = applyNonVanillaType(caught, water, random, size);
			}
			caught.stack.setDamage(size);
		} else if (nonVanilla) {
			applyNonVanillaTypeOnly(caught, water, random);
		}
	}

	/** A rolled size, and whether it crossed the "special" (lucky/rare/legendary) threshold. */
	private static final class SizeRoll {
		int size;
		boolean special;
	}

	private static SizeRoll rollSize(Random random, double scale, int water) {
		if (Config.FISHING.biggerFish && water >= OCEAN_WATER_BLOCKS) {
			SizeRoll roll = new SizeRoll();
			int size = (int) Math.round(1000.0 * sampleGamma(random, 2.0, scale));
			if (size >= 990) roll.special = true;
			if (size >= 1000) {
				// The top of the range is spread out so record-sized catches vary in the last digits
				// rather than all landing on exactly 1000.
				size = 1000 + random.nextInt(10) * 10 + random.nextInt(10);
				if (size == 1099) {
					// Nudge the max roll so 1100 is actually reachable, not just 1099.
					size = size + random.nextInt(2);
				}
			} else if (size < 100) {
				return normalSizeRoll(random, scale);
			}
			roll.size = size;
			return roll;
		}
		return normalSizeRoll(random, scale);
	}

	private static SizeRoll normalSizeRoll(Random random, double scale) {
		SizeRoll roll = new SizeRoll();
		int size = (int) Math.round(600.0 * sampleGamma(random, 2.0, scale));
		if (size >= 550) {
			size = MAX_NORMAL_SIZE - 100 + random.nextInt(10) * 10 + random.nextInt(10);
			if (size == 699) {
				// Nudge the max roll so 700 is actually reachable, not just 699.
				size = size + random.nextInt(2);
			}
			if (size >= 690 && size <= 700) roll.special = true;
		} else {
			// Nothing is a zero-centimetre fish; the floor keeps small catches plausible.
			size = size + 100;
		}
		roll.size = size;
		return roll;
	}

	/**
	 * Picks a species weighted by how much open water surrounds the bobber, swaps {@code caught}'s
	 * stack to it, and returns the size after that species' own adjustment - a big lake fish reads
	 * differently to a small river one even at the same underlying roll. From FishinFoodTweaks'
	 * {@code fishinFoodTweaks_use} (the {@code enableRandomFishSizes} branch).
	 */
	private static int applyNonVanillaType(ItemEntity caught, int water, Random random, int size) {
		int fishType;
		if (Config.FISHING.biggerFish && water >= OCEAN_WATER_BLOCKS) {
			fishType = random.nextInt(4);
			if (Config.FISHING.calculateWaterSurfaceSize && fishType == 1) fishType = random.nextInt(4);
		} else if (water >= 150) {
			fishType = random.nextInt(3);
		} else if (water >= 50) {
			fishType = random.nextInt(2);
		} else {
			fishType = 0;
		}

		caught.stack = new ItemStack(RAW[fishType], 1);

		switch (fishType) {
			case 0:
				if (size >= 400) size = (int) (size * 0.50);
				else if (size >= 300) size = (int) (size * 0.75);
				break;
			case 1:
				if (size >= 350) size = (int) (size * 0.8);
				else size = (int) ((size + 10) * 1.1);
				break;
			case 2:
				if (size >= 450) size = (int) (size * 0.9);
				else size = (int) ((size + 50) * 1.1);
				break;
			case 3:
			default:
				if (size >= 500) {
					if (size < 600) size = (int) (size * 1.1);
				} else {
					size = (int) ((size + 125) * 1.1);
				}
				break;
		}
		return size;
	}

	/**
	 * Picks a species (or leaves the vanilla catch alone) when sizes are off. From FishinFoodTweaks'
	 * {@code fishinFoodTweaks_use} (the {@code !enableRandomFishSizes} branch).
	 */
	private static void applyNonVanillaTypeOnly(ItemEntity caught, int water, Random random) {
		int fishType;
		if (water >= 250) {
			fishType = random.nextInt(5);
			if (Config.FISHING.calculateWaterSurfaceSize && fishType == 1) fishType = random.nextInt(5);
		} else if (water >= 150) {
			fishType = random.nextInt(4);
		} else if (water >= 50) {
			fishType = random.nextInt(3);
		} else {
			fishType = random.nextInt(2);
		}
		fishType -= 1;
		if (fishType >= 0 && fishType < RAW.length) {
			caught.stack = new ItemStack(RAW[fishType], 1);
		}
	}

	// ================================================================ shared with cooking/tooltips/healing

	/** How much health a fish of this size restores. */
	public static int healingFor(ItemStack stack) {
		int size = stack.getDamage() != 0 ? stack.getDamage() : 250;
		// Cooking doubles the value, as it does for every other food in the game.
		double divisor = isRawFish(stack) ? 100.0 : 50.0;
		return (int) Math.floor(size / divisor);
	}

	public static boolean isRawFish(ItemStack stack) {
		return stack != null && (stack.itemId == Item.RAW_FISH.id || nonVanillaRawIndex(stack.itemId) >= 0);
	}

	public static boolean isCookedFish(ItemStack stack) {
		return stack != null && (stack.itemId == Item.COOKED_FISH.id || nonVanillaCookedIndex(stack.itemId) >= 0);
	}

	public static boolean isFish(ItemStack stack) {
		return isRawFish(stack) || isCookedFish(stack);
	}

	/**
	 * Counts water blocks outwards from the bobber in eight directions. Cheap and good enough: it
	 * tells a pond from a lake from an ocean, which is all the size roll needs.
	 */
	private static int measureWaterSurface(World world, double x, double y, double z) {
		if (!Config.FISHING.calculateWaterSurfaceSize) return OCEAN_WATER_BLOCKS;

		int blockX = (int) Math.floor(x);
		int blockY = (int) Math.floor(y);
		int blockZ = (int) Math.floor(z);

		int found = 0;
		int surfaceY = blockY;
		if (isWater(world, blockX, blockY, blockZ)) {
			found = 1;
		} else if (isWater(world, blockX, blockY - 1, blockZ)) {
			// The bobber floats in the block above the surface as often as not.
			surfaceY = blockY - 1;
			found = 1;
		}
		if (found == 0) return 0;

		int[][] directions = {{1, 1}, {1, 0}, {1, -1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, 1}, {0, -1}};
		for (int[] direction : directions) {
			int searchX = blockX;
			int searchZ = blockZ;
			while (found < SURFACE_SEARCH_LIMIT) {
				searchX += direction[0];
				searchZ += direction[1];
				if (!isWater(world, searchX, surfaceY, searchZ)) break;
				found++;
			}
		}
		return found;
	}

	private static boolean isWater(World world, int x, int y, int z) {
		int id = world.getBlockId(x, y, z);
		return id == Block.WATER.id || id == Block.FLOWING_WATER.id;
	}

	/**
	 * Marsaglia and Tsang's gamma sampler. Valid for shape &gt;= 1, which is all this needs
	 * (shape is always 2), so the shape-boosting branch of the full algorithm is left out.
	 */
	private static double sampleGamma(Random random, double shape, double scale) {
		double d = shape - 1.0 / 3.0;
		double c = 1.0 / Math.sqrt(9.0 * d);
		while (true) {
			double x;
			double v;
			do {
				x = random.nextGaussian();
				v = 1.0 + c * x;
			} while (v <= 0.0);
			v = v * v * v;
			double u = random.nextDouble();
			double xSquared = x * x;
			if (u < 1.0 - 0.0331 * xSquared * xSquared) return d * v * scale;
			if (Math.log(u) < 0.5 * xSquared + d * (1.0 - v + Math.log(v))) return d * v * scale;
		}
	}
}
