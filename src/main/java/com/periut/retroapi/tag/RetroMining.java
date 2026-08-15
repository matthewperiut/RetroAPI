package com.periut.retroapi.tag;

import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.register.item.RetroItemAccess;
import com.periut.retroapi.mixin.register.ToolItemAccessor;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;

import java.util.Collections;
import java.util.Set;

/**
 * What a tool does to a block: whether it harvests, and how fast. The whole of RetroAPI's answer, in one
 * place because it has to be asked from two.
 *
 * <p>Beta asks these questions on {@code PlayerEntity}, and RetroAPI answers there with a mixin. StationAPI
 * moves them: its flattening module gives {@code PlayerInventory} position-aware overloads that fire
 * {@code PlayerStrengthOnBlockEvent} and {@code IsPlayerUsingEffectiveToolEvent}, and the vanilla methods
 * RetroAPI hooked are simply no longer on the path. Two call sites, one set of rules; a second
 * implementation behind the StationAPI events is how the tag system came to be missing there entirely, so
 * both sides call this and neither owns the logic.
 */
public final class RetroMining {

	private RetroMining() {}

	/**
	 * Whether the held tool harvests this block, or {@code null} to leave the answer to the game.
	 *
	 * <p>Modern, DECOUPLED harvest semantics:
	 * <ul>
	 *   <li>A {@code mineable/<tool>} tag grants SPEED (see {@link #breakingSpeed}); on its own it does not
	 *       gate drops, exactly like modern Minecraft.</li>
	 *   <li>Whether a block needs a tool AT ALL comes from its material ({@code !isHandHarvestable()},
	 *       beta's own stone/metal rule) or from membership in a {@code needs_<tier>_tool} tag.</li>
	 * </ul>
	 * When a block does need a tool, the held item must be a matching KIND (one of the block's mineable
	 * tags) and of sufficient TIER. Blocks that need no tool, and blocks in no RetroAPI tag, are left to
	 * vanilla untouched. This is what makes a {@code .tool(PICKAXE).tier(IRON)} plain item work on the
	 * shipped vanilla block tags (stone, ores, ...) the same as on a modded ore.
	 */
	public static Boolean canHarvest(PlayerEntity player, Block block) {
		if (player == null || block == null) {
			return null;
		}
		if (((RetroBlockAccess) block).isAlwaysDrops()) {
			return Boolean.TRUE;
		}
		Set<RetroTool> requiredKinds = RetroTags.mineableTools(block);
		RetroToolTier requiredTier = RetroTags.requiredTier(block);
		boolean tagged = !requiredKinds.isEmpty() || requiredTier != RetroToolTier.WOOD;
		if (!tagged) {
			return null; // block not in any RetroAPI tag: vanilla decides entirely
		}
		// Decouple: a mineable tag alone does NOT require a tool. The block needs a tool only if its
		// material says so (stone/metal), or a needs_<tier>_tool tag does. Otherwise it drops by hand
		// and the tag only made it faster; leave that case to vanilla.
		boolean requiresTool = !block.material.isHandHarvestable() || requiredTier != RetroToolTier.WOOD;
		if (!requiresTool) {
			return null;
		}
		ItemStack held = player.inventory.getSelectedItem();
		Item heldItem = held != null ? held.getItem() : null;

		// Tool KIND: if the block declares mineable kinds, the held tool must be one of them.
		if (!requiredKinds.isEmpty() && Collections.disjoint(RetroTool.kindsOf(heldItem), requiredKinds)) {
			return Boolean.FALSE;
		}
		// Tool TIER: needs_<tier>_tool demands the material level (declared statically, per stack, or per
		// stack AND block via RetroItemAccess.tier, or inferred from a ToolItem's material).
		if (!RetroToolTier.of(held, block, player).isAtLeast(requiredTier)) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	/**
	 * How fast the held tool breaks this block, given the speed the game worked out on its own.
	 *
	 * @param vanilla the unmodified answer, returned untouched whenever nothing here applies
	 */
	public static float breakingSpeed(PlayerEntity player, Block block, float vanilla) {
		if (player == null || block == null || vanilla > 1.0f) {
			return vanilla;
		}
		ItemStack held = player.inventory.getSelectedItem();
		if (held == null) {
			return vanilla;
		}
		Item item = held.getItem();
		RetroBlockAccess access = (RetroBlockAccess) block;

		// Explicit per-block declarations win ties with the tag system.
		if (access.isAlwaysEffectiveTool()) {
			Float speed = toolSpeed(player, held, null);
			if (speed != null) {
				return speed;
			}
		} else {
			Class<? extends Item> effectiveTool = access.getEffectiveTool();
			if (effectiveTool != null && effectiveTool.isInstance(item)) {
				Float speed = toolSpeed(player, held, null);
				if (speed != null) {
					return speed;
				}
			}
		}

		// Tag path: held tool kind matches a mineable/<tool> tag containing this block.
		Set<RetroTool> tools = RetroTags.mineableTools(block);
		if (!tools.isEmpty() && !Collections.disjoint(RetroTool.kindsOf(item), tools)) {
			Float speed = toolSpeed(player, held, block);
			if (speed != null) {
				return speed;
			}
		}
		return vanilla;
	}

	private static Float toolSpeed(PlayerEntity player, ItemStack held, Block block) {
		Item item = held.getItem();
		// An explicitly declared speed wins: that is the whole point of building a tool from parameters
		// instead of a ToolMaterial (which mods cannot extend - it is an enum).
		float declared = ((RetroItemAccess) item).getMiningSpeed();
		if (declared > 0.0F && !RetroTool.kindsOf(item).isEmpty()) {
			return declared;
		}
		// Vanilla tool classes: sample the held item's speed against a reference block of
		// the matching material (gives the tool's material speed: wood 2 .. gold 12).
		if (item instanceof PickaxeItem) return player.inventory.getStrengthOnBlock(Block.STONE);
		if (item instanceof AxeItem) return player.inventory.getStrengthOnBlock(Block.PLANKS);
		if (item instanceof ShovelItem) return player.inventory.getStrengthOnBlock(Block.DIRT);
		if (item instanceof SwordItem) return 1.5f;
		// Custom ToolItem subclasses (declared via RetroItemAccess.tool): raw material speed.
		if (item instanceof ToolItem) return ((ToolItemAccessor) item).retroapi$getMiningSpeed();
		// Declared plain-Item tools: scale with the declared tier, so .tier(IRON) mines like an iron
		// tool (6x) instead of a flat token boost. Honors a dynamic or block-aware tier via the stack.
		if (!RetroTool.kindsOf(item).isEmpty()) {
			RetroToolTier tier = RetroToolTier.of(held, block, player);
			return tier == null ? 2.0f : tier.getMiningSpeed();
		}
		return null;
	}
}
