package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.client.gui.ExtraButton;
import com.periut.retrotweaks.compat.ApiBridge;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.Option;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Splits vanilla's "Advanced OpenGL" button into two live toggles - RGSS AA and Mipmaps - when
 * RetroDragon 0.1.5+ is installed, via {@link ApiBridge}'s reflective handles onto RetroDragon's
 * {@code RetroSettings} API. RetroDragon absent, or present but older than 0.1.5 (no
 * {@code RetroSettings} class to find), {@link ApiBridge#hasRetroDragonSettings()} is false and this
 * mixin does nothing: the screen is exactly what {@link VideoOptionsScreenMixin} and vanilla already
 * built.
 *
 * <p><b>Why Advanced OpenGL specifically.</b> RetroDragon's own {@code TerrainAppearance} repurposed
 * it: its original job, hardware occlusion queries, does not exist on RetroDragon's WebGPU backend,
 * so RetroDragon reads {@code GameOptions.advancedOpengl} as a combined on/off for RGSS and mipmaps
 * together. This mixin gives the player independent control of the two - which is strictly more than
 * one checkbox could ever express - so vanilla's single flag is no longer reachable as its own
 * setting while these two buttons are on screen. See {@link #retrotweaks$splitAdvancedOpenGl} for what
 * that means for the flag's saved value.
 *
 * <p><b>Why this runs at TAIL, not by touching {@code VIDEO_OPTIONS}.</b> RGSS and Mipmaps are not
 * vanilla {@code GameOptions} fields - their state lives entirely in RetroDragon's own
 * {@code RetroSettings}, with no need for RetroTweaks to model, save or load them at all. Modelling
 * them as {@link Option} constants (as {@code OptionMixin} does for RetroTweaks' own video settings)
 * would mean teaching vanilla's {@code GameOptions.getString/setInt} switches about two more enum
 * values for no benefit. Instead this mixin waits for {@link VideoOptionsScreenMixin}'s HEAD
 * injection and vanilla's own {@code init()} loop to finish building every button - Advanced OpenGL,
 * every RetroTweaks row, and Done - at their normal positions, then removes that one button and
 * re-flows the grid around the two that replace it. Operating on the real, already-placed widgets
 * (rather than predicting where they would land) is what keeps this correct regardless of which
 * optional RetroTweaks video rows happen to be enabled, and at any GUI scale.
 *
 * <p><b>Why a re-flow and not a downward shift.</b> The first version pushed every button below the
 * old row down by one row, which left a visible hole: the two replacements stack vertically, taking
 * a cell and the cell directly beneath it, so the cell BESIDE the lower one had nothing in it while
 * everything after had moved a whole row further down. This is a two-column grid filled in index
 * order, so the correct move is to advance the later buttons by one CELL and let the first of them
 * fill that hole.
 *
 * <p><b>Why Done is clamped rather than just shifted.</b> Vanilla anchors Done to {@code height/6 +
 * 168}, which at ScreenScaler's smallest legal scaledHeight leaves it 12px clear of the bottom edge -
 * less than the 24px row this adds. Shifting it by a whole row therefore pushed it off the window
 * entirely, with nothing to bring it back. See {@link #retrotweaks$splitAdvancedOpenGl}.
 */
@Environment(EnvType.CLIENT)
@Mixin(VideoOptionsScreen.class)
public abstract class VideoOptionsScreenRetroDragonMixin extends Screen {

	/** One grid row on this screen. Matches vanilla's own {@code 24 * (i >> 1)} in {@code init()}. */
	@Unique
	private static final int ROW_HEIGHT = 24;

	/** Room a 20px button plus a margin needs at the bottom of the screen - the floor Done is held to. */
	@Unique
	private static final int BOTTOM_MARGIN = 26;

	@Inject(method = "init", at = @At("TAIL"))
	private void retrotweaks$splitAdvancedOpenGl(CallbackInfo ci) {
		if (!ApiBridge.hasRetroDragonSettings()) return;

		int advancedOpenGlId = Option.ADVANCED_OPENGL.getId();
		ButtonWidget advanced = null;
		for (ButtonWidget button : this.buttons) {
			if (button.id == advancedOpenGlId) {
				advanced = button;
				break;
			}
		}
		if (advanced == null) return; // defensive - VIDEO_OPTIONS always includes it today

		this.buttons.remove(advanced);
		int x = advanced.x;
		int y = advanced.y;

		// This screen is a TWO COLUMN grid filled in index order - vanilla's init() places option i
		// at (width/2 - 155 + i % 2 * 160, height/6 + 24 * (i >> 1)) - so adding one button has to
		// advance everything after it by one CELL, not one row.
		//
		// Shifting whole rows instead left a visible hole: the two new buttons stack vertically, so
		// they take the Advanced OpenGL cell and the cell directly below it (same column, next row),
		// and the cell BESIDE the lower one stays empty while every later button drops a full row.
		// Re-flowing fills that cell with whatever came next instead.
		int gridIndex = retrotweaks$cellIndex(advanced);
		int stackedBelow = gridIndex + 2; // same column, next row - where the Mipmaps button goes

		List<ButtonWidget> trailing = new ArrayList<>();
		for (ButtonWidget button : this.buttons) {
			if (retrotweaks$isGridButton(button) && retrotweaks$cellIndex(button) > gridIndex) trailing.add(button);
		}
		trailing.sort(Comparator.comparingInt(this::retrotweaks$cellIndex));

		int oldRows = retrotweaks$rowsFor(gridIndex + 1 + trailing.size());
		int cell = gridIndex + 1;
		for (ButtonWidget button : trailing) {
			if (cell == stackedBelow) cell++; // reserved for the second new button
			button.x = retrotweaks$cellX(cell);
			button.y = retrotweaks$cellY(cell);
			cell++;
		}
		int newRows = retrotweaks$rowsFor(Math.max(cell, stackedBelow + 1));

		// Done is placed outside the grid at a fixed offset, so it only moves if the grid genuinely
		// grew a row - which it does not always, since the re-flow reuses the hole first.
		//
		// CLAMPED, because that fixed offset has no headroom to give. Vanilla puts Done at
		// height/6 + 168, which on ScreenScaler's smallest legal scaledHeight of 240 lands at y=208
		// with its bottom edge at 228 - 12px of clearance. Shifting it a full 24px row for the Mipmaps
		// button put it at 232, i.e. entirely below the window, and it stayed there because nothing else
		// on this screen re-places it. The grid's own last row ends far above BOTTOM_MARGIN even at that
		// height (Mipmaps' bottom edge is 156), so pinning Done there cannot collide with it.
		if (newRows > oldRows) {
			int shift = (newRows - oldRows) * ROW_HEIGHT;
			int lowestY = this.height - BOTTOM_MARGIN;
			for (ButtonWidget button : this.buttons) {
				if (!retrotweaks$isGridButton(button) && button.y > y) {
					button.y = Math.min(button.y + shift, lowestY);
				}
			}
		}

		// RetroDragon's TerrainAppearance still reads GameOptions.advancedOpengl as a master gate on
		// top of both RGSS and mipmaps (see the class javadoc). With its button gone, force it true
		// once here so a "false" left over from before RetroDragon was installed - or from vanilla's
		// own default - can never silently null out both new buttons while they read "ON". This is
		// the moment the coarse flag stops being a player-facing setting of its own and becomes an
		// implementation detail of the two finer ones; it is still saved to options.txt like any
		// other vanilla value (Done still calls GameOptions.save()), and if RetroDragon is later
		// removed the value is simply "on", with vanilla's own button back to change it.
		if (this.minecraft != null && this.minecraft.options != null) {
			this.minecraft.options.advancedOpengl = true;
		}

		this.buttons.add(new ExtraButton(x, y, 150, 20, retrotweaks$rgssLabel(), button -> {
			ApiBridge.setRgss(!ApiBridge.isRgss());
			button.text = retrotweaks$rgssLabel();
		}));
		this.buttons.add(new ExtraButton(x, y + ROW_HEIGHT, 150, 20, retrotweaks$mipmapLabel(), button -> {
			ApiBridge.setMipmap(!ApiBridge.isMipmap());
			button.text = retrotweaks$mipmapLabel();
		}));
	}

	// ---------------------------------------------------------------- grid geometry
	// Mirrors vanilla VideoOptionsScreen.init()'s own placement so a re-flow lands buttons exactly
	// where that loop would have put them, rather than on a parallel guess at the layout.

	@Unique
	private int retrotweaks$cellX(int index) {
		return this.width / 2 - 155 + index % 2 * 160;
	}

	@Unique
	private int retrotweaks$cellY(int index) {
		return this.height / 6 + ROW_HEIGHT * (index >> 1);
	}

	/** A button belongs to the grid when it sits on one of the two column x positions. */
	@Unique
	private boolean retrotweaks$isGridButton(ButtonWidget button) {
		return button.x == retrotweaks$cellX(0) || button.x == retrotweaks$cellX(1);
	}

	@Unique
	private int retrotweaks$cellIndex(ButtonWidget button) {
		int row = (button.y - this.height / 6) / ROW_HEIGHT;
		int column = button.x == retrotweaks$cellX(1) ? 1 : 0;
		return row * 2 + column;
	}

	/** Rows a grid of {@code cells} cells occupies, two per row. */
	@Unique
	private static int retrotweaks$rowsFor(int cells) {
		return (cells + 1) / 2;
	}

	@Unique
	private static String retrotweaks$rgssLabel() {
		return "RGSS AA: " + (ApiBridge.isRgss() ? "ON" : "OFF");
	}

	@Unique
	private static String retrotweaks$mipmapLabel() {
		return "Mipmaps: " + (ApiBridge.isMipmap() ? "ON" : "OFF");
	}
}
