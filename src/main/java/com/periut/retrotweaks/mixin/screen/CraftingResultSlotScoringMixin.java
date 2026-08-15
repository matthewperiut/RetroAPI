/*
 * Ported from WhatAreYouScoring by Telvarost. The scoring rules are unchanged; the achievement
 * pages it registered are not carried over, as they need an item/achievement registry API.
 *
 * The source mod listened for StationAPI's ItemUsedInCraftingEvent, which does not exist without
 * StationAPI. CraftingResultSlot#onTakeItem is the vanilla call site the event was firing from:
 * every recipe result grab lands here, on both the crafting table and the 2x2 player grid, so it
 * is the correct one-mixin replacement for that listener.
 */
package com.periut.retrotweaks.mixin.screen;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.scoring.Score;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fills in the bow/arrow, misc and armour crafting bitfields for the 404 challenge score (up to
 * 138 points), which without this hook are never assigned from gameplay. From WhatAreYouScoring's
 * {@code ItemUsedInCraftingListener}.
 */
@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotScoringMixin {

	@Shadow private PlayerEntity player;

	@Inject(method = "onTakeItem", at = @At("HEAD"))
	private void retrotweaks$onTakeItem(ItemStack stack, CallbackInfo ci) {
		if (!Config.SCORING.challenge404.enabled) return;
		if (null == stack) return;

		Score.Fields retrotweaks$s = Score.of(this.player);

		if (0x00C0 != retrotweaks$s.BOW_AND_ARROW_CRAFTING_BITFIELD) {
			if (stack.itemId == Item.ARROW.id) {
				int arrowIncrement = 0x007F & retrotweaks$s.BOW_AND_ARROW_CRAFTING_BITFIELD;
				if (64 > arrowIncrement) {
					arrowIncrement = arrowIncrement + 4;
					retrotweaks$s.BOW_AND_ARROW_CRAFTING_BITFIELD &= 0x0080;
					retrotweaks$s.BOW_AND_ARROW_CRAFTING_BITFIELD |= arrowIncrement;
					retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				}
				return;
			} else if (stack.itemId == Item.BOW.id) {
				retrotweaks$s.BOW_AND_ARROW_CRAFTING_BITFIELD |= 0x0080;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			}
		}

		if (0x003F != retrotweaks$s.MISC_CRAFTING_BITFIELD) {
			if (stack.itemId == Block.LAPIS_BLOCK.id) {
				retrotweaks$s.MISC_CRAFTING_BITFIELD |= 0x0001;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Block.IRON_BLOCK.id) {
				retrotweaks$s.MISC_CRAFTING_BITFIELD |= 0x0002;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Block.GOLD_BLOCK.id) {
				retrotweaks$s.MISC_CRAFTING_BITFIELD |= 0x0004;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Block.DIAMOND_BLOCK.id) {
				retrotweaks$s.MISC_CRAFTING_BITFIELD |= 0x0008;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Block.JACK_O_LANTERN.id) {
				retrotweaks$s.MISC_CRAFTING_BITFIELD |= 0x0010;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.BREAD.id) {
				retrotweaks$s.MISC_CRAFTING_BITFIELD |= 0x0020;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			}
		}

		if (0xFFFF != retrotweaks$s.ARMOR_CRAFTING_BITFIELD) {
			if (stack.itemId == Item.LEATHER_HELMET.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0008;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.LEATHER_CHESTPLATE.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0004;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.LEATHER_LEGGINGS.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0002;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.LEATHER_BOOTS.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0001;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.IRON_HELMET.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0080;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.IRON_CHESTPLATE.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0040;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.IRON_LEGGINGS.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0020;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.IRON_BOOTS.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0010;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.GOLDEN_HELMET.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0800;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.GOLDEN_CHESTPLATE.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0400;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.GOLDEN_LEGGINGS.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0200;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.GOLDEN_BOOTS.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x0100;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.DIAMOND_HELMET.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x8000;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.DIAMOND_CHESTPLATE.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x4000;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.DIAMOND_LEGGINGS.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x2000;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			} else if (stack.itemId == Item.DIAMOND_BOOTS.id) {
				retrotweaks$s.ARMOR_CRAFTING_BITFIELD |= 0x1000;
				retrotweaks$s.Current404Score = Score.calculate404ChallengeScore(retrotweaks$s, this.player.world);
				return;
			}
		}
	}
}
