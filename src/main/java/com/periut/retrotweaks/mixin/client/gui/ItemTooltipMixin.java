package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.fishing.Fishing;
import com.periut.retrotweaks.mixin.item.FoodItemHealAccessor;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.FoodItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hover tooltips for food, showing how much it heals, and for fish, showing the size it was caught
 * at and, on a rare roll, a rarity label in place of the name. From FishinFoodTweaks.
 *
 * <p>Vanilla b1.7.3 is not tooltip-less: {@link HandledScreen#render} already draws a single-line
 * item-name tooltip - a {@code fillGradient} box (the 2nd one in {@code render}) sized to the name,
 * then the name itself via {@code TextRenderer.drawWithShadow}. An earlier version of this mixin drew
 * a second, independent box on top of that to fit the extra lines, and the two fought for depth: on
 * a tall fish tooltip the text ended up drawn underneath vanilla's own translucent box, and the extra
 * GL state changes leaked into the world behind the GUI. Instead, this wraps vanilla's own two draw
 * calls and expands them in place - the box grows to fit the extra lines (and widens if one is wider
 * than the name), the name draw is swapped for a rarity label when one applies, and the extra lines
 * are drawn 10px apart below it with that same call. Everything stays inside vanilla's own GL state
 * and depth, so there is only ever one box.
 */
@Mixin(HandledScreen.class)
public abstract class ItemTooltipMixin extends Screen {

	@Shadow
	protected abstract Slot getSlotAt(int x, int y);

	/** Replaces vanilla's own name line when non-null; null leaves vanilla's name draw untouched. */
	@Unique
	private String retrotweaks$nameOverride;

	/** Extra lines drawn below the name, recomputed each frame at render HEAD. */
	@Unique
	private List<String> retrotweaks$extraLines = Collections.emptyList();

	@Inject(method = "render", at = @At("HEAD"))
	private void retrotweaks$prepTooltip(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		this.retrotweaks$nameOverride = null;
		this.retrotweaks$extraLines = Collections.emptyList();
		if (!Config.FISHING.foodHealingTooltips && !Config.FISHING.fishHealingTooltip && !Config.FISHING.randomFishSizes) return;
		// A held stack follows the cursor and sits over the tooltip; vanilla itself skips drawing its
		// own tooltip in that case (see the getCursorStack() == null check around its draw calls).
		if (this.minecraft.player.inventory.getCursorStack() != null) return;

		Slot slot = getSlotAt(mouseX, mouseY);
		if (slot == null || !slot.hasStack()) return;
		retrotweaks$prepareLines(slot.getStack());
	}

	@Unique
	private void retrotweaks$prepareLines(ItemStack stack) {
		List<String> lines = new ArrayList<>();
		Item item = Item.ITEMS[stack.itemId];

		if (Fishing.isFish(stack)) {
			if (Config.FISHING.randomFishSizes) {
				// The damage value is the size in tenths of a centimetre; an un-rolled stack (damage 0,
				// e.g. from creative or another mod) is treated as a middling 25cm fish.
				int fishSize = stack.getDamage() != 0 ? stack.getDamage() : 250;
				this.retrotweaks$nameOverride = retrotweaks$fishRarityLabel(stack, fishSize);
				if (Config.FISHING.fishHealingTooltip) {
					// FishinFoodTweaks halves the raw heal() amount for display in every tooltip branch
					// (FoodBaseMixin#getTooltip); Fishing.healingFor is also the amount actually healed, so
					// only the shown number is halved here, not the value itself.
					lines.add("§7Restores " + (Fishing.healingFor(stack) / 2.0) + " health");
				}
				// Shown whenever random fish sizes are on, regardless of the healing tooltip toggle.
				lines.add("§7Size: " + (fishSize / 10.0) + "cm");
			} else if (Config.FISHING.fishHealingTooltip) {
				lines.add("§7Restores " + (Fishing.healingFor(stack) / 2.0) + " health");
			}
		} else if (Config.FISHING.foodHealingTooltips && item instanceof FoodItem) {
			lines.add("§7Restores " + (((FoodItemHealAccessor) item).retrotweaks$getHealthRestored() / 2.0) + " health");
		}
		this.retrotweaks$extraLines = lines;
	}

	/**
	 * The rarity label for a fish that crosses a size threshold, colour-coded as in FishinFoodTweaks,
	 * or null when no threshold applies - which leaves vanilla's own name draw alone.
	 */
	@Unique
	private String retrotweaks$fishRarityLabel(ItemStack stack, int fishSize) {
		boolean raw = Fishing.isRawFish(stack);
		if (fishSize >= 990) {
			if (fishSize >= 1090) {
				return raw ? "§bLegendary Fish" : "§bCooked Legendary Fish";
			}
			return raw ? "§6Rare Fish" : "§6Cooked Rare Fish";
		}
		if (fishSize >= 690 && fishSize <= 700) {
			return raw ? "§eLucky Fish" : "§eCooked Lucky Fish";
		}
		return null;
	}

	/**
	 * Expand vanilla's tooltip background (the 2nd {@code fillGradient} in {@code render}) to fit the
	 * extra lines: taller for the line count, and wider if any extra line - or the rarity label - is
	 * longer than the name vanilla already measured. Vanilla sizes the box as [x1-3 .. x1+name+3];
	 * text starts at x1+3, so the widest line plus that same 3px margin gives the new right edge.
	 */
	@WrapOperation(
		method = "render",
		at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;fillGradient(IIIIII)V"))
	private void retrotweaks$expandBox(HandledScreen self, int x1, int y1, int x2, int y2, int top, int bottom,
			Operation<Void> original) {
		int textX = x1 + 3;
		int widest = x2 - textX - 3; // the name width vanilla already measured
		if (this.retrotweaks$nameOverride != null) {
			widest = Math.max(widest, this.textRenderer.getWidth(this.retrotweaks$nameOverride));
		}
		for (String line : this.retrotweaks$extraLines) {
			widest = Math.max(widest, this.textRenderer.getWidth(line));
		}
		int right = textX + widest + 3;
		original.call(self, x1, y1, right, y2 + this.retrotweaks$extraLines.size() * 10, top, bottom);
	}

	/** Draw the name (swapped for the rarity label if one applies), then the extra lines below it. */
	@WrapOperation(
		method = "render",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;III)V"))
	private void retrotweaks$drawLines(TextRenderer font, String name, int x, int y, int color,
			Operation<Void> original) {
		original.call(font, this.retrotweaks$nameOverride != null ? this.retrotweaks$nameOverride : name, x, y, color);
		int lineY = y;
		for (String line : this.retrotweaks$extraLines) {
			lineY += 10;
			original.call(font, line, x, lineY, 0xAAAAAA);
		}
	}
}
