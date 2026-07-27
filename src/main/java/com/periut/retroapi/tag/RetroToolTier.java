package com.periut.retroapi.tag;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool material tiers, mirroring modern Minecraft's {@code needs_<tier>_tool} tag family.
 * A block in {@code needs_iron_tool} only drops when the breaking tool's tier is at least
 * IRON (and the tool KIND still has to match a {@code mineable/<tool>} tag, exactly like
 * modern). Vanilla gold tools mine at wood tier, also like modern.
 *
 * <p>Item side: vanilla {@link ToolItem} subclasses infer their tier from their material's
 * mining level automatically; custom items declare one with
 * {@code RetroItemAccess.tier(RetroToolTier.IRON)} (chainable at registration or from a
 * constructor via {@code RetroItemAccess.of(this).tier(...)}).</p>
 *
 * <p>Block side: data files at {@code data/{ns}/tags/block/needs_stone_tool.json} (and
 * {@code needs_iron_tool}, {@code needs_diamond_tool}) in the modern format, or from code:
 * {@code RetroTags.addToTag(RetroToolTier.IRON.needsTag(), block)}.</p>
 *
 * <h2>Adding a tier</h2>
 * This used to be an enum, which made the five built-in tiers the only tiers that could ever exist: a
 * mod adding bronze between stone and iron, or mithril above diamond, had nowhere to put it and had to
 * pick the nearest vanilla tier and lose the distinction. It is a registry now, and the built-in tiers
 * are ordinary entries in it:
 *
 * <pre>
 * public static final RetroToolTier BRONZE =
 *     RetroToolTier.register("bronze", 1, 5.0F);   // between STONE (1, 4.0) and IRON (2, 6.0)
 * </pre>
 *
 * <p>The level is what {@link #isAtLeast} compares, so it decides what a tier can harvest, and the
 * mining speed is what a declared plain-Item tool of that tier breaks at. Levels do NOT have to be
 * unique: two tiers at the same level harvest the same blocks and can still differ in speed and in which
 * {@code needs_<name>_tool} tag they answer to. Register from your {@code initRetro()}, before anything
 * queries tags.
 *
 * <p>Being a class rather than an enum, this can no longer be used in a {@code switch} or an
 * {@code EnumSet}; compare with {@link #isAtLeast} or read {@link #getLevel()} instead.
 */
public final class RetroToolTier {

	private static final Map<String, RetroToolTier> BY_NAME = new LinkedHashMap<>();

	/**
	 * Not a tool at all: below every tier, so it satisfies no {@code needs_<tier>_tool} requirement -
	 * not even the implicit WOOD one that any tool-requiring block demands.
	 *
	 * <p>This is what a {@link Dynamic}/{@link Contextual} declaration returns to say "this tool cannot
	 * harvest THIS block", which {@code null} cannot express: {@code null} means "no opinion, fall
	 * through to the next declaration", and falling through lands on WOOD, so a lambda had no way to
	 * refuse. A drill bit that only bites certain ores is
	 * {@code .tier((stack, block, player) -> isOre(block) ? DIAMOND : NONE)}.
	 *
	 * <p>It does not stop a block being broken by hand - a block that needs no tool never consults a
	 * tier at all - it only means this item does not count as the tool such a block requires.
	 */
	public static final RetroToolTier NONE = register("none", -1, null, 1.0F);
	public static final RetroToolTier WOOD = register("wood", 0, null, 2.0F);
	public static final RetroToolTier STONE = register("stone", 1, "needs_stone_tool", 4.0F);
	public static final RetroToolTier IRON = register("iron", 2, "needs_iron_tool", 6.0F);
	public static final RetroToolTier DIAMOND = register("diamond", 3, "needs_diamond_tool", 8.0F);

	private final String name;
	private final int level;
	private final String tagName;
	private final float miningSpeed;

	private RetroToolTier(String name, int level, String tagName, float miningSpeed) {
		this.name = name;
		this.level = level;
		this.tagName = tagName;
		this.miningSpeed = miningSpeed;
	}

	/**
	 * Registers a tier, or returns the existing one if the name is taken (so a mod loaded twice, or two
	 * mods agreeing on "bronze", get the same tier rather than a duplicate).
	 *
	 * @param name        the tier's id, also the {@code needs_<name>_tool} tag it answers to
	 * @param level       what {@link #isAtLeast} compares; wood is 0, diamond is 3
	 * @param miningSpeed how fast a declared plain-Item tool of this tier breaks its blocks (wood is 2)
	 */
	public static synchronized RetroToolTier register(String name, int level, float miningSpeed) {
		return register(name, level, "needs_" + name + "_tool", miningSpeed);
	}

	/**
	 * {@link #register(String, int, float)} with an explicit tag name, or null for a tier that demands no
	 * tag at all (what WOOD and NONE do: everything already mines at wood tier).
	 */
	public static synchronized RetroToolTier register(String name, int level, String tagName,
			float miningSpeed) {
		RetroToolTier existing = BY_NAME.get(name);
		if (existing != null) {
			return existing;
		}
		RetroToolTier tier = new RetroToolTier(name, level, tagName, miningSpeed);
		BY_NAME.put(name, tier);
		return tier;
	}

	/** The registered tier with this name, or null. */
	public static RetroToolTier get(String name) {
		return BY_NAME.get(name);
	}

	/** Every registered tier, in registration order. */
	public static Collection<RetroToolTier> all() {
		return Collections.unmodifiableCollection(new ArrayList<>(BY_NAME.values()));
	}

	/** Every registered tier, in registration order. Kept for code written against the old enum. */
	public static RetroToolTier[] values() {
		return BY_NAME.values().toArray(new RetroToolTier[0]);
	}

	/** This tier's id, e.g. {@code "iron"}. */
	public String name() {
		return this.name;
	}

	/**
	 * Computes a tool's tier from the actual stack, for tools whose tier is not fixed: e.g. a
	 * multi-tool that levels up with use, or one whose tier rides its damage/NBT. Declare one with
	 * {@code RetroItemAccess.tier(stack -> ...)}; it is consulted per-harvest, so the answer can change
	 * at runtime. Return any {@link RetroToolTier}. This is the "dynamically picking the tier" hook.
	 */
	@FunctionalInterface
	public interface Dynamic {
		RetroToolTier tierFor(ItemStack stack);
	}

	/**
	 * A tier that also sees WHAT is being mined and WHO is mining it - the missing half of {@link Dynamic},
	 * which only got the stack and so could not express "diamond-tier on stone, wood-tier on everything
	 * else", a drill bit that only bites certain ores, or a tool that is weaker in the nether. Declared
	 * with {@code RetroItemAccess.tier((stack, block, player) -> ...)} and consulted per harvest, ahead of
	 * the stack-only and static forms.
	 *
	 * <pre>
	 * RetroItemAccess.of(this).tier((stack, block, player) -&gt; {
	 *     if (player != null &amp;&amp; player.world.dimension.isNether) return RetroToolTier.WOOD;
	 *     return block == Block.STONE ? RetroToolTier.DIAMOND : RetroToolTier.WOOD;
	 * });
	 * </pre>
	 *
	 * <p>The player is the one breaking the block, and is null only when something asks for a tier outside
	 * a harvest (a tooltip, another mod's query) - treat it as "no context", not as an error. For the
	 * block's position and metadata, which beta's harvest hooks do not carry, see {@link Positional}.
	 */
	@FunctionalInterface
	public interface Contextual {
		RetroToolTier tierFor(ItemStack stack, net.minecraft.block.Block block,
			net.minecraft.entity.player.PlayerEntity player);
	}

	/**
	 * A tier that also sees WHERE the block is, so it can read that block's state.
	 *
	 * <p>{@link Contextual} gets the {@code Block}, which is the block TYPE - every state of a block is
	 * the same {@code Block} object, so "diamond-tier on lit ore, wood-tier on unlit" was not expressible.
	 * Beta's harvest hooks genuinely do not carry a position, so RetroAPI captures the block the player is
	 * currently breaking (see {@code RetroBreakTarget}) and hands it over here.
	 *
	 * <pre>
	 * RetroItemAccess.of(this).tier((stack, block, player, world, x, y, z) -&gt; {
	 *     RetroBlockState state = RetroStates.get(world, x, y, z);
	 *     return state != null &amp;&amp; state.get(MyOre.RICH) ? RetroToolTier.DIAMOND : RetroToolTier.IRON;
	 * });
	 * </pre>
	 *
	 * <p>The world is null, and the coordinates are zero, when the tier is asked for outside an actual
	 * break (a tooltip, another mod's query, the inventory) - the same "no context" rule as the player in
	 * {@link Contextual}. Consulted ahead of every other form.
	 */
	@FunctionalInterface
	public interface Positional {
		RetroToolTier tierFor(ItemStack stack, net.minecraft.block.Block block,
			net.minecraft.entity.player.PlayerEntity player,
			net.minecraft.world.BlockView world, int x, int y, int z);
	}

	/** Numeric mining level, comparable to {@link ToolMaterial#getMiningLevel()}. */
	public int getLevel() {
		return level;
	}

	/** How fast a declared plain-Item tool of this tier breaks its blocks (vanilla wood is 2, diamond 8). */
	public float getMiningSpeed() {
		return miningSpeed;
	}

	/** The {@code needs_<tier>_tool} tag key, or null for WOOD (everything mines wood tier). */
	public RetroTagKey needsTag() {
		return tagName == null ? null : RetroTagKey.block(tagName);
	}

	/** True when a tool of this tier may harvest a block requiring the given tier. */
	public boolean isAtLeast(RetroToolTier required) {
		return level >= required.level;
	}

	/**
	 * The tier of an item, with no stack context: a dynamic tier declaration is ignored here (it needs
	 * a stack), an explicit {@code RetroItemAccess.tier(RetroToolTier)} declaration wins, vanilla tool
	 * items infer from their material's mining level, everything else is WOOD (matching modern, where a
	 * bare hand or a stick is a wood-tier "tool"). Prefer {@link #of(ItemStack)} in harvest code.
	 */
	public static RetroToolTier of(Item item) {
		if (item == null) {
			return WOOD;
		}
		com.periut.retroapi.register.item.RetroItemAccess access =
			(com.periut.retroapi.register.item.RetroItemAccess) item;
		RetroToolTier declared = access.getToolTier();
		if (declared != null) {
			return declared;
		}
		if (item instanceof ToolItem) {
			return fromLevel(((com.periut.retroapi.mixin.register.ToolItemAccessor) item)
				.retroapi$getToolMaterial().getMiningLevel());
		}
		return WOOD;
	}

	/**
	 * The tier of the tool in this stack. A {@code RetroItemAccess.tier(stack -> ...)} dynamic
	 * declaration wins (letting the tier change at runtime), then a static {@code .tier(...)}, then a
	 * vanilla {@code ToolItem}'s material level, else WOOD. This is the overload the harvest hook uses.
	 */
	public static RetroToolTier of(ItemStack stack) {
		if (stack == null) {
			return WOOD;
		}
		Item item = stack.getItem();
		if (item == null) {
			return WOOD;
		}
		Dynamic dynamic = ((com.periut.retroapi.register.item.RetroItemAccess) item).getToolTierDynamic();
		if (dynamic != null) {
			RetroToolTier tier = dynamic.tierFor(stack);
			if (tier != null) {
				return tier;
			}
		}
		return of(item);
	}

	/**
	 * The tier of the tool in this stack <em>for a specific block</em>, with no player context. Equivalent
	 * to {@code of(stack, block, null)}; use the three-argument form from anywhere that knows who is
	 * mining, so contextual tiers can see the world they are being asked about.
	 */
	public static RetroToolTier of(ItemStack stack, net.minecraft.block.Block block) {
		return of(stack, block, null);
	}

	/**
	 * The tier of the tool in this stack <em>for a specific block, in a specific player's hands</em>. A
	 * {@code RetroItemAccess.tier((stack, block, player, world, x, y, z) -> ...)} declaration wins, then
	 * the {@code (stack, block, player)} form, then the stack-only dynamic form, then a static tier, then
	 * a vanilla {@code ToolItem}'s material. This is the overload the harvest hooks use, so a tool's level
	 * can depend on what it is pointed at, on that block's state, and on where its wielder is standing.
	 *
	 * <p>The position comes from {@link com.periut.retroapi.tag.RetroBreakTarget}, which records the block
	 * the player is currently breaking - beta's own harvest hooks carry no coordinates.
	 */
	public static RetroToolTier of(ItemStack stack, net.minecraft.block.Block block,
			net.minecraft.entity.player.PlayerEntity player) {
		if (stack == null) {
			return WOOD;
		}
		Item item = stack.getItem();
		if (item == null) {
			return WOOD;
		}
		com.periut.retroapi.register.item.RetroItemAccess access =
			(com.periut.retroapi.register.item.RetroItemAccess) item;
		Positional positional = access.getToolTierPositional();
		if (positional != null) {
			RetroBreakTarget target = RetroBreakTarget.current(player, block);
			RetroToolTier tier = target != null
				? positional.tierFor(stack, block, player, target.world(), target.x(), target.y(), target.z())
				: positional.tierFor(stack, block, player, null, 0, 0, 0);
			if (tier != null) {
				return tier;
			}
		}
		Contextual contextual = access.getToolTierContextual();
		if (contextual != null) {
			RetroToolTier tier = contextual.tierFor(stack, block, player);
			if (tier != null) {
				return tier;
			}
		}
		return of(stack);
	}

	/**
	 * The tier for a numeric mining level: the registered tier whose level matches, or the highest
	 * registered tier at or below it. Negative levels are {@link #NONE}.
	 */
	public static RetroToolTier fromLevel(int level) {
		if (level < 0) {
			return NONE;
		}
		RetroToolTier best = WOOD;
		for (RetroToolTier tier : BY_NAME.values()) {
			if (tier.level == level) {
				return tier;
			}
			if (tier.level > best.level && tier.level < level) {
				best = tier;
			}
		}
		return best;
	}

	/**
	 * The bare tag name this tier demands ({@code "needs_iron_tool"}), or null for {@link #WOOD} and
	 * {@link #NONE}, which demand none. {@link #needsTag()} gives the same thing as a {@link RetroTagKey};
	 * this is here for code building tag ids of its own.
	 */
	public String getTagName() {
		return this.tagName;
	}

	@Override
	public String toString() {
		return this.name;
	}

	/** Every registered tier as a list, ordered by level then registration. */
	static List<RetroToolTier> ordered() {
		List<RetroToolTier> list = new ArrayList<>(BY_NAME.values());
		list.sort((a, b) -> Integer.compare(a.level, b.level));
		return list;
	}
}
