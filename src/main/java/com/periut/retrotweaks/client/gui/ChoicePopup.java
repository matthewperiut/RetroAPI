package com.periut.retrotweaks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.function.IntConsumer;

/**
 * The dropdown behind every multiple-choice option: every value at once, with the current one ticked.
 *
 * <p>These used to be cycle buttons - click to advance, Shift-click to go back - which is fine for two
 * values and awful for eight, because the only way to find out what a setting can be is to click it
 * until it comes back round. Showing the whole list costs one extra click and answers that question
 * immediately.
 *
 * <p>Scrolls by whole rows rather than by pixels, so no row is ever drawn half-height and nothing has
 * to be clipped; the lists are short enough that smooth scrolling would buy nothing.
 */
@Environment(EnvType.CLIENT)
public final class ChoicePopup extends Popup {

	private static final int MAX_VISIBLE_ROWS = 12;

	private final String[] labels;
	private final int selected;
	private final IntConsumer onPick;

	private int visibleRows;
	private int scrollRows;
	/** Guards the "open with the current value in view" jump so it happens once, not every frame - the
	 *  user scrolling back to the top must not be undone on the next one. */
	private boolean scrollSeeded;

	public ChoicePopup(String title, String[] labels, int selected, IntConsumer onPick) {
		super(title);
		this.labels = labels;
		this.selected = selected;
		this.onPick = onPick;
	}

	@Override
	public void layout(ListScreen screen) {
		int content = screen.gWidth(title);
		for (String label : labels) {
			content = Math.max(content, screen.gWidth(label) + ListScreen.CHECKBOX_SIZE + 4);
		}
		width = Math.min(Math.max(150, content + PAD * 2 + 6), Math.max(120, screen.screenWidth() - 16));

		int roomForRows = (screen.screenHeight() - TITLE_HEIGHT - PAD - 8) / ListScreen.ROW_HEIGHT;
		visibleRows = Math.max(1, Math.min(Math.min(labels.length, MAX_VISIBLE_ROWS), roomForRows));
		height = TITLE_HEIGHT + visibleRows * ListScreen.ROW_HEIGHT + PAD;
		placeAtBottom(screen);

		// Open with the current value on screen rather than always at the top.
		if (!scrollSeeded) {
			scrollSeeded = true;
			if (selected >= visibleRows) scrollRows = selected - visibleRows + 1;
		}
		clampScroll();
	}

	private void clampScroll() {
		scrollRows = Math.max(0, Math.min(labels.length - visibleRows, scrollRows));
	}

	private int listTop() {
		return y + TITLE_HEIGHT + 1;
	}

	@Override
	public void render(ListScreen screen, int mouseX, int mouseY) {
		drawFrame(screen);
		boolean scrollable = labels.length > visibleRows;
		int right = x + width - PAD - (scrollable ? 6 : 0);

		for (int slot = 0; slot < visibleRows; slot++) {
			int index = scrollRows + slot;
			if (index >= labels.length) break;
			int rowY = listTop() + slot * ListScreen.ROW_HEIGHT;
			boolean hovered = mouseX >= x + 1 && mouseX < x + width - 1
				&& mouseY >= rowY && mouseY < rowY + ListScreen.ROW_HEIGHT;
			int color = hovered ? ListScreen.TEXT_HOVER : ListScreen.TEXT;
			if (hovered) screen.gFill(x + 1, rowY, x + width - 1, rowY + ListScreen.ROW_HEIGHT, ListScreen.ROW_HOVER_BG);
			screen.drawCheckbox(x + PAD, rowY + 2, index == selected, true, hovered, color);
			int textX = x + PAD + ListScreen.CHECKBOX_SIZE + 4;
			screen.gText(screen.trim(labels[index], right - textX), textX, rowY + 2, color);
		}

		if (scrollable) {
			int trackX = x + width - PAD;
			int trackTop = listTop();
			int trackHeight = visibleRows * ListScreen.ROW_HEIGHT;
			screen.gFill(trackX, trackTop, trackX + 3, trackTop + trackHeight, ListScreen.SCROLL_TRACK);
			int thumbHeight = Math.max(8, trackHeight * visibleRows / labels.length);
			int thumbY = trackTop + (trackHeight - thumbHeight) * scrollRows / (labels.length - visibleRows);
			screen.gFill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, ListScreen.SCROLL_THUMB);
		}
	}

	@Override
	public void mouseClicked(ListScreen screen, int mouseX, int mouseY, int button) {
		if (button != 0) return;
		if (!inside(mouseX, mouseY)) {
			screen.closePopup();
			return;
		}
		int slot = (mouseY - listTop()) / ListScreen.ROW_HEIGHT;
		if (slot < 0 || slot >= visibleRows) return;
		int index = scrollRows + slot;
		if (index < 0 || index >= labels.length) return;
		screen.playClick();
		screen.closePopup();
		onPick.accept(index);
	}

	@Override
	public void scrolled(float notches) {
		scrollRows -= Math.round(notches);
		clampScroll();
	}
}
