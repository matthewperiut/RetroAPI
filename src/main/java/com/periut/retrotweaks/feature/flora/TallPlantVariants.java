package com.periut.retrotweaks.feature.flora;

/**
 * The three variants of block 31 - dead shrub, short grass and fern - in one place: their metas and
 * their names.
 *
 * <h2>One representation, three names</h2>
 *
 * <p>A tall plant is always {@code itemId 31, damage 0/1/2}, on a bare loader and under either API.
 * That is vanilla's own answer for wool, dye and saplings, it needs no registry, and - the point -
 * a world stays readable when a mod is removed. {@link com.periut.retrotweaks.mixin.item.TallPlantSubtypesMixin}
 * is what makes those three metas behave as real subtypes, and
 * {@link com.periut.retrotweaks.mixin.item.TallPlantItemNameMixin} is what names them apart.
 *
 * <h2>Naming them, without minting anything</h2>
 *
 * <p>The names live in RetroAPI's own {@code minecraft:} table
 * ({@link com.periut.retroapi.commands.argument.VanillaIds}), which maps a name to an id AND a
 * meta - so {@code minecraft:fern} resolves to {@code (31, 2)} the same way {@code minecraft:red_wool}
 * resolves to {@code (35, 14)}, and {@code /give}, the item argument and the creative search all read
 * that one table.
 *
 * <p>This used to be three registered "alias" items - one per name, each rewritten back to
 * {@code (31, meta)} inside {@code ItemStack}'s constructor - because a registry maps a name to an
 * {@code Item} with nowhere to put a damage value. Merging into RetroAPI removed the need for the
 * whole layer: three registry slots, an item class, a StationAPI registration listener and a hook on
 * every stack constructed in the game, replaced by three lines in a lookup table. They were also
 * three extra entries for the creative search to find, which is how the same plant showed up twice.
 */
public final class TallPlantVariants {

	private TallPlantVariants() {}

	/** Meta 0 of block 31. Named apart from block 32's "Dead Bush", which shares its sprite. */
	public static final int DEAD_SHRUB = 0;
	public static final int SHORT_GRASS = 1;
	public static final int FERN = 2;

	/** Highest meta block 31 has a texture for. Metas above this are not variants at all. */
	public static final int MAX_META = FERN;

	/**
	 * Identifier paths, indexed by meta, as registered in RetroAPI's {@code minecraft:} table. The
	 * namespace is {@code minecraft} rather than {@code retrotweaks} on purpose: these are vanilla
	 * blocks that vanilla simply never named, and both APIs already call block 31 as a whole
	 * {@code minecraft:grass}. Neither name collides - {@code minecraft:dead_bush} is block 32, a
	 * different block.
	 */
	public static final String[] PATHS = { "dead_shrub", "short_grass", "fern" };

	/** Translation keys, indexed by meta. {@code .name} is appended by the lookup, as vanilla does. */
	public static final String[] TRANSLATION_KEYS = {
		"tile.retrotweaks.dead_shrub",
		"tile.retrotweaks.short_grass",
		"tile.retrotweaks.fern",
	};

	/**
	 * English names, indexed by meta, used when nothing has loaded a lang file - see
	 * {@link com.periut.retrotweaks.mixin.client.TallPlantNameShimMixin}. A real
	 * translation always wins over these.
	 */
	public static final String[] NAMES = { "Dead Shrub", "Short Grass", "Fern" };

	/** True when {@code meta} is one of the three real variants. */
	public static boolean isVariant(int meta) {
		return meta >= 0 && meta <= MAX_META;
	}
}
