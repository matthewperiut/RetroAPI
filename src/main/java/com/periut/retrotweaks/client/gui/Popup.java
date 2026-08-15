package com.periut.retrotweaks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.input.Keyboard;

/**
 * A modal overlay drawn on top of a {@link ListScreen}.
 *
 * <p>Anything a row cannot say in twelve pixels happens here instead of on a screen of its own:
 * picking one of several values, typing a string, dragging a number. A separate {@code Screen} would
 * work too - that is what the string editor used to be - but pushing and popping a screen loses the
 * list's scroll position and makes changing three settings in a row feel like navigating a menu tree.
 *
 * <p>Subclasses draw through {@link ListScreen}'s handed-out primitives (all of which end up in
 * {@code Tessellator.INSTANCE}) rather than touching GL, and size themselves against the screen in
 * {@link #layout}, which runs every frame so a resize or a GUI scale change is picked up for free.
 */
@Environment(EnvType.CLIENT)
public abstract class Popup {

	protected static final int TITLE_HEIGHT = 16;
	protected static final int PAD = 6;
	protected static final int BUTTON_HEIGHT = 16;
	/** Gap kept under the box so it never sits flush against the bottom edge. */
	private static final int BOTTOM_MARGIN = 8;

	protected final String title;

	protected int x;
	protected int y;
	protected int width;
	protected int height;

	protected Popup(String title) {
		this.title = title;
	}

	/** Recomputes size and position. Called every frame, and once when the popup is opened. */
	public abstract void layout(ListScreen screen);

	public abstract void render(ListScreen screen, int mouseX, int mouseY);

	public void mouseClicked(ListScreen screen, int mouseX, int mouseY, int button) {}

	/** Also receives plain mouse movement, with {@code button} of -1 - see {@code ListScreen}. That is
	 *  the only drag signal b1.7.3 offers, so anything that follows the cursor keys off it. */
	public void mouseReleased(ListScreen screen, int mouseX, int mouseY, int button) {}

	public void keyPressed(ListScreen screen, char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) screen.closePopup();
	}

	public void scrolled(float notches) {}

	public void tick() {}

	/**
	 * Centred horizontally, sitting at the bottom of the screen.
	 *
	 * <p>Not in the middle, because the middle is where the thing being changed usually is. Dragging
	 * the render scale is the clearest case - the whole point is to watch the world resample while you
	 * do it, and a box parked over the centre of the view hides exactly what you are judging. The
	 * bottom edge is the one part of the screen nothing important occupies while a config screen is
	 * open, and it is also where the list's own footer already sat, so the eye is already there.
	 */
	protected void placeAtBottom(ListScreen screen) {
		x = Math.max(2, (screen.screenWidth() - width) / 2);
		y = Math.max(2, screen.screenHeight() - height - BOTTOM_MARGIN);
	}

	/**
	 * The box, its border and the title strip - everything every popup has in common.
	 *
	 * <p>No full-screen dim behind it. The box is opaque black with a border, which is all it needs to
	 * be legible, and dimming the rest would defeat the reason it moved to the bottom of the screen in
	 * the first place.
	 */
	protected void drawFrame(ListScreen screen) {
		screen.gFill(x, y, x + width, y + height, ListScreen.PANEL_BG);
		screen.outline(x, y, width, height, ListScreen.PANEL_EDGE);
		screen.gText(screen.trim(title, width - PAD * 2), x + PAD, y + 5, ListScreen.TEXT);
		screen.gFill(x + 1, y + TITLE_HEIGHT - 1, x + width - 1, y + TITLE_HEIGHT, ListScreen.SEPARATOR);
	}

	protected boolean inside(int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	/** A button rectangle inside the popup, kept as a field so layout and hit-testing agree. */
	protected static final class Button {
		final String text;
		final Runnable action;
		int x;
		int y;
		int width;
		int height;
		boolean enabled = true;

		Button(String text, Runnable action) {
			this.text = text;
			this.action = action;
		}

		boolean contains(int mouseX, int mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}

		void render(ListScreen screen, int mouseX, int mouseY) {
			screen.drawButton(x, y, width, height, text, enabled, enabled && contains(mouseX, mouseY));
		}
	}

	/** Lays {@code buttons} out evenly across one row, matching the list's own footer. */
	protected void layoutButtonRow(Button[] buttons, int rowY) {
		int span = width - PAD * 2;
		int gap = 4;
		int each = (span - gap * (buttons.length - 1)) / buttons.length;
		for (int i = 0; i < buttons.length; i++) {
			buttons[i].x = x + PAD + i * (each + gap);
			buttons[i].y = rowY;
			buttons[i].width = i == buttons.length - 1 ? x + width - PAD - buttons[i].x : each;
			buttons[i].height = BUTTON_HEIGHT;
		}
	}

	protected boolean clickButtons(Button[] buttons, int mouseX, int mouseY) {
		for (Button button : buttons) {
			if (button.enabled && button.contains(mouseX, mouseY)) {
				button.action.run();
				return true;
			}
		}
		return false;
	}
}
