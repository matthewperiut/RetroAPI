package com.periut.retroapi.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.periut.retroapi.component.RetroTooltips;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/**
 * Turns beta's single-line item-name tooltip into a multi-line one, the clean way: it
 * wraps the game's OWN tooltip draw calls and expands them, rather than drawing a second
 * box on top. That matters because the previous over-draw approach left two artefacts the
 * user hit, a translucent vanilla box showing through behind the bigger one, and leaked
 * GL state (lighting/colour) into the world behind the GUI, tinting the sky.
 *
 * Here, vanilla draws its tooltip background with one {@code fillGradient} (the 2nd in
 * render) and the name with one {@code drawWithShadow}. We wrap both: the box is drawn
 * taller to fit the extra lines, and after the name we draw the extra lines with the same
 * call, same colours, same position, same GL state. Nothing else changes.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenTooltipMixin extends Screen {

	@Shadow
	protected abstract Slot getSlotAt(int x, int y);

	/** The extra lines for the currently-hovered stack, recomputed each frame at render HEAD. */
	@Unique
	private List<String> retroapi$lines = Collections.emptyList();

	@Inject(method = "render", at = @At("HEAD"))
	private void retroapi$prepTooltip(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		Slot slot = this.getSlotAt(mouseX, mouseY);
		if (slot != null && slot.hasStack()) {
			ItemStack stack = slot.getStack();
			List<String> extra = RetroTooltips.linesFor(stack);
			this.retroapi$lines = extra.isEmpty() ? Collections.emptyList() : extra;
		} else {
			this.retroapi$lines = Collections.emptyList();
		}
	}

	/**
	 * Expand the tooltip background (the 2nd fillGradient) to fit the extra lines: taller
	 * for the line count, and wider if any extra line is longer than the name. Vanilla
	 * sizes the box as [name x-1 .. nameWidth..x+3]; the text starts at x1+3, so the
	 * widest line plus the same 3px margin gives the new right edge.
	 */
	@WrapOperation(
		method = "render",
		at = @At(value = "INVOKE", ordinal = 1,
			target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;fillGradient(IIIIII)V"))
	private void retroapi$expandBox(HandledScreen self, int x1, int y1, int x2, int y2, int top, int bottom,
			Operation<Void> original) {
		int textX = x1 + 3;
		int widest = x2 - textX - 3; // the name width vanilla already measured
		for (String line : this.retroapi$lines) {
			widest = Math.max(widest, this.textRenderer.getWidth(line));
		}
		int right = textX + widest + 3;
		original.call(self, x1, y1, right, y2 + this.retroapi$lines.size() * 10, top, bottom);
	}

	/** Draw the name (original), then the extra lines below it in the same style. */
	@WrapOperation(
		method = "render",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/font/TextRenderer;drawWithShadow(Ljava/lang/String;III)V"))
	private void retroapi$drawLines(TextRenderer font, String name, int x, int y, int color,
			Operation<Void> original) {
		original.call(font, name, x, y, color);
		int lineY = y;
		for (String line : this.retroapi$lines) {
			lineY += 10;
			original.call(font, line, x, lineY, 0xAAAAAA);
		}
	}
}
