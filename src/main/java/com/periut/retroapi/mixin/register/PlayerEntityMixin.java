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
	 * Modern mineable semantics: a block in any {@code mineable/<tool>} tag drops only
	 * when a matching tool is held. Blocks in no tag keep vanilla behavior entirely.
	 */
	@Inject(method = "canHarvest", at = @At("HEAD"), cancellable = true)
	private void retroapi$mineableHarvest(Block block, CallbackInfoReturnable<Boolean> cir) {
		if (((RetroBlockAccess) block).isAlwaysDrops()) {
			return; // the alwaysDrops handler already answered true
		}
		Set<RetroTool> tools = RetroTags.mineableTools(block);
		com.periut.retroapi.tag.RetroToolTier required = RetroTags.requiredTier(block);
		if (tools.isEmpty() && required == com.periut.retroapi.tag.RetroToolTier.WOOD) {
			return; // untagged blocks keep vanilla behavior entirely
		}
		ItemStack held = this.inventory.getSelectedItem();
		Item heldItem = held != null ? held.getItem() : null;

		// Tool KIND: a block in a mineable tag drops only for a matching tool kind.
		if (!tools.isEmpty()) {
			RetroTool kind = RetroTool.of(heldItem);
			if (kind == null || !tools.contains(kind)) {
				cir.setReturnValue(false);
				return;
			}
		}
		// Tool TIER: a block in a needs_<tier>_tool tag also demands the material level
		// (declared via RetroItemAccess.tier, or inferred from a ToolItem's material).
		if (!com.periut.retroapi.tag.RetroToolTier.of(heldItem).isAtLeast(required)) {
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
			Float speed = retroapi$getToolSpeed(item);
			if (speed != null) {
				cir.setReturnValue(speed);
				return;
			}
		} else {
			Class<? extends Item> effectiveTool = access.getEffectiveTool();
			if (effectiveTool != null && effectiveTool.isInstance(item)) {
				Float speed = retroapi$getToolSpeed(item);
				if (speed != null) {
					cir.setReturnValue(speed);
					return;
				}
			}
		}

		// Tag path: held tool kind matches a mineable/<tool> tag containing this block.
		Set<RetroTool> tools = RetroTags.mineableTools(block);
		if (!tools.isEmpty()) {
			RetroTool kind = RetroTool.of(item);
			if (kind != null && tools.contains(kind)) {
				Float speed = retroapi$getToolSpeed(item);
				if (speed != null) {
					cir.setReturnValue(speed);
				}
			}
		}
	}

	@Unique
	private Float retroapi$getToolSpeed(Item item) {
		// Vanilla tool classes: sample the held item's speed against a reference block of
		// the matching material (gives the tool's material speed: wood 2 .. gold 12).
		if (item instanceof PickaxeItem) return this.inventory.getStrengthOnBlock(Block.STONE);
		if (item instanceof AxeItem) return this.inventory.getStrengthOnBlock(Block.PLANKS);
		if (item instanceof ShovelItem) return this.inventory.getStrengthOnBlock(Block.DIRT);
		if (item instanceof SwordItem) return 1.5f;
		// Custom ToolItem subclasses (declared via RetroItemAccess.tool): raw material speed.
		if (item instanceof ToolItem) return ((ToolItemAccessor) item).retroapi$getMiningSpeed();
		// Declared tools that are plain Items (incl. hoes, which beta gives no block speeds):
		// a modest boost so tagged blocks still feel responsive.
		if (RetroTool.of(item) != null) return 1.5f;
		return null;
	}
}
