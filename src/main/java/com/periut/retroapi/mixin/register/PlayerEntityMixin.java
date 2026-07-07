package com.periut.retroapi.mixin.register;

import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.tag.RetroTags;
import com.periut.retroapi.tag.RetroTool;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

	@Shadow public PlayerInventory inventory;

	@Inject(method = "canHarvest", at = @At("HEAD"), cancellable = true)
	private void retroapi$alwaysDrops(Block block, CallbackInfoReturnable<Boolean> cir) {
		if (((RetroBlockAccess) block).isAlwaysDrops()) {
			cir.setReturnValue(true);
		}
	}

	/**
	 * Modern, DECOUPLED harvest semantics:
	 * <ul>
	 *   <li>A {@code mineable/<tool>} tag grants SPEED (in the breaking-speed hook below); on its own it
	 *       does not gate drops, exactly like modern Minecraft.</li>
	 *   <li>Whether a block needs a tool AT ALL comes from its material ({@code !isHandHarvestable()},
	 *       beta's own stone/metal rule) or from membership in a {@code needs_<tier>_tool} tag.</li>
	 * </ul>
	 * When a block does need a tool, the held item must be a matching KIND (one of the block's mineable
	 * tags) and of sufficient TIER. Blocks that need no tool, and blocks in no RetroAPI tag, are left to
	 * vanilla untouched. This is what makes a {@code .tool(PICKAXE).tier(IRON)} plain item work on the
	 * shipped vanilla block tags (stone, ores, ...) the same as on a modded ore.
	 */
	@Inject(method = "canHarvest", at = @At("HEAD"), cancellable = true)
	private void retroapi$mineableHarvest(Block block, CallbackInfoReturnable<Boolean> cir) {
		if (((RetroBlockAccess) block).isAlwaysDrops()) {
			return; // the alwaysDrops handler already answered true
		}
		Set<RetroTool> requiredKinds = RetroTags.mineableTools(block);
		com.periut.retroapi.tag.RetroToolTier requiredTier = RetroTags.requiredTier(block);
		boolean tagged = !requiredKinds.isEmpty()
			|| requiredTier != com.periut.retroapi.tag.RetroToolTier.WOOD;
		if (!tagged) {
			return; // block not in any RetroAPI tag: vanilla decides entirely
		}
		// Decouple: a mineable tag alone does NOT require a tool. The block needs a tool only if its
		// material says so (stone/metal), or a needs_<tier>_tool tag does. Otherwise it drops by hand
		// and the tag only made it faster; leave that case to vanilla.
		boolean requiresTool = !((Block) block).material.isHandHarvestable()
			|| requiredTier != com.periut.retroapi.tag.RetroToolTier.WOOD;
		if (!requiresTool) {
			return;
		}
		ItemStack held = this.inventory.getSelectedItem();
		Item heldItem = held != null ? held.getItem() : null;

		// Tool KIND: if the block declares mineable kinds, the held tool must be one of them.
		if (!requiredKinds.isEmpty()) {
			Set<RetroTool> heldKinds = RetroTool.kindsOf(heldItem);
			if (java.util.Collections.disjoint(heldKinds, requiredKinds)) {
				cir.setReturnValue(false);
				return;
			}
		}
		// Tool TIER: needs_<tier>_tool demands the material level (declared statically or dynamically via
		// RetroItemAccess.tier, or inferred from a ToolItem's material).
		if (!com.periut.retroapi.tag.RetroToolTier.of(held).isAtLeast(requiredTier)) {
			cir.setReturnValue(false);
			return;
		}
		cir.setReturnValue(true);
	}

	@Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
	private void retroapi$effectiveTool(Block block, CallbackInfoReturnable<Float> cir) {
		RetroBlockAccess access = (RetroBlockAccess) block;
		if (cir.getReturnValue() > 1.0f) return;

		ItemStack held = this.inventory.getSelectedItem();
		if (held == null) return;
		Item item = held.getItem();

		// Explicit per-block declarations win ties with the tag system.
		if (access.isAlwaysEffectiveTool()) {
			Float speed = retroapi$getToolSpeed(held);
			if (speed != null) {
				cir.setReturnValue(speed);
				return;
			}
		} else {
			Class<? extends Item> effectiveTool = access.getEffectiveTool();
			if (effectiveTool != null && effectiveTool.isInstance(item)) {
				Float speed = retroapi$getToolSpeed(held);
				if (speed != null) {
					cir.setReturnValue(speed);
					return;
				}
			}
		}

		// Tag path: held tool kind matches a mineable/<tool> tag containing this block.
		Set<RetroTool> tools = RetroTags.mineableTools(block);
		if (!tools.isEmpty() && !java.util.Collections.disjoint(RetroTool.kindsOf(item), tools)) {
			Float speed = retroapi$getToolSpeed(held);
			if (speed != null) {
				cir.setReturnValue(speed);
			}
		}
	}

	@Unique
	private Float retroapi$getToolSpeed(ItemStack held) {
		Item item = held.getItem();
		// Vanilla tool classes: sample the held item's speed against a reference block of
		// the matching material (gives the tool's material speed: wood 2 .. gold 12).
		if (item instanceof PickaxeItem) return this.inventory.getStrengthOnBlock(Block.STONE);
		if (item instanceof AxeItem) return this.inventory.getStrengthOnBlock(Block.PLANKS);
		if (item instanceof ShovelItem) return this.inventory.getStrengthOnBlock(Block.DIRT);
		if (item instanceof SwordItem) return 1.5f;
		// Custom ToolItem subclasses (declared via RetroItemAccess.tool): raw material speed.
		if (item instanceof ToolItem) return ((ToolItemAccessor) item).retroapi$getMiningSpeed();
		// Declared plain-Item tools: scale with the declared tier, so .tier(IRON) mines like an iron
		// tool (6x) instead of a flat token boost. Honors a dynamic tier via the stack.
		if (!RetroTool.kindsOf(item).isEmpty()) {
			return retroapi$tierSpeed(com.periut.retroapi.tag.RetroToolTier.of(held));
		}
		return null;
	}

	/** Vanilla ToolMaterial mining speeds by tier: wood 2, stone 4, iron 6, diamond 8. */
	@Unique
	private static float retroapi$tierSpeed(com.periut.retroapi.tag.RetroToolTier tier) {
		switch (tier) {
			case DIAMOND: return 8.0f;
			case IRON: return 6.0f;
			case STONE: return 4.0f;
			default: return 2.0f;
		}
	}
}
