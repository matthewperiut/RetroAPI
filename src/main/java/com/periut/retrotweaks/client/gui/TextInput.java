package com.periut.retrotweaks.client.gui;

import com.periut.retrotweaks.RetroTweaks;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;

import org.lwjgl.input.Keyboard;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

/**
 * A single-line text field with a real caret and a real selection.
 *
 * <p>b1.7.3's own {@code TextFieldWidget} has neither: the insertion point is always the end of the
 * string, drawn as a blinking underscore appended to it, and the only edit is backspace. Fixing a typo
 * near the front of a resource URL therefore means deleting everything after it. This keeps an index
 * into the string instead, draws it as a one-pixel vertical bar between the two characters it sits
 * between, and moves it with the arrow keys (Home/End, and Ctrl for whole words).
 *
 * <p>Selection is an {@link #anchor} index alongside the caret; the two are equal when nothing is
 * selected. Shift with any movement key extends it, click-and-drag sweeps it, and Ctrl/Cmd+A takes
 * everything. Copy, cut and paste all work, on both the Ctrl and the Cmd chord - see
 * {@link #isCommandDown()}, which is why a Mac's Cmd+V does not have to be retrained as Ctrl+V.
 *
 * <p>It also scrolls horizontally by CARET rather than by end-of-string: {@link #scrollIndex} is the
 * first visible character and only moves when the caret would otherwise fall off an edge, so typing in
 * the middle of a long value leaves the text where it is instead of yanking the whole line about on
 * every keystroke.
 *
 * <p>Drawing goes through {@link ListScreen}'s handed-out primitives (i.e. {@code Tessellator.INSTANCE})
 * like everything else in this package; the font is asked for measurements only at draw time, which is
 * also the only moment it is available.
 */
@Environment(EnvType.CLIENT)
public final class TextInput {

	/** Ticks the caret spends solid, then hidden. 20 ticks is a second, so this is the usual ~1.6Hz. */
	private static final int BLINK_PERIOD = 6;
	private static final int TEXT_INSET = 4;

	private final StringBuilder text = new StringBuilder();
	private int maxLength = 4096;
	private int caret;
	/** The fixed end of the selection; equal to {@link #caret} when nothing is selected. */
	private int anchor;
	/** Index of the leftmost character actually drawn - see the class doc. */
	private int scrollIndex;
	private int blink;
	private boolean dragging;

	private int x;
	private int y;
	private int width;
	private int height;

	public TextInput(String initial) {
		setText(initial);
	}

	public void setBounds(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}

	public String getText() {
		return text.toString();
	}

	/** Replaces the whole value and puts the caret at the end, as if it had just been typed. */
	public void setText(String value) {
		text.setLength(0);
		text.append(value == null ? "" : value);
		if (text.length() > maxLength) text.setLength(maxLength);
		caret = anchor = text.length();
		scrollIndex = 0;
		blink = 0;
	}

	public void tick() {
		blink++;
	}

	private int selectionStart() {
		return Math.min(anchor, caret);
	}

	private int selectionEnd() {
		return Math.max(anchor, caret);
	}

	private boolean hasSelection() {
		return anchor != caret;
	}

	// ---------------------------------------------------------------- rendering

	public void render(ListScreen screen) {
		ensureCaretVisible(screen);

		screen.gFill(x, y, x + width, y + height, ListScreen.BOX_BG);
		screen.outline(x, y, width, height, ListScreen.BOX_BORDER);

		int textY = y + (height - 8) / 2;
		String visible = visibleText(screen);
		// Highlight first, text over it - fill() is opaque, so the other order would erase the glyphs.
		drawSelection(screen, visible, textY);
		screen.gText(visible, x + TEXT_INSET, textY, ListScreen.TEXT);

		if ((blink / BLINK_PERIOD) % 2 == 0) {
			int caretX = x + TEXT_INSET + screen.gWidth(text.substring(scrollIndex, caret));
			// A bar BETWEEN characters, one pixel wide and a little taller than the glyphs, rather than
			// vanilla's underscore appended after them - the whole point is that it can sit mid-string.
			screen.gFill(caretX, textY - 1, caretX + 1, textY + 9, ListScreen.TEXT);
		}
	}

	private void drawSelection(ListScreen screen, String visible, int textY) {
		if (!hasSelection()) return;
		int from = Math.max(selectionStart(), scrollIndex);
		int to = Math.min(selectionEnd(), scrollIndex + visible.length());
		if (to <= from) return;
		int left = x + TEXT_INSET + screen.gWidth(text.substring(scrollIndex, from));
		int right = x + TEXT_INSET + screen.gWidth(text.substring(scrollIndex, to));
		screen.gFill(left, textY - 1, right, textY + 9, ListScreen.SELECTION);
	}

	private int innerWidth() {
		return Math.max(1, width - TEXT_INSET * 2 - 1);
	}

	/**
	 * Moves {@link #scrollIndex} the least amount that puts the caret back inside the box: left if the
	 * caret has run off the front, right until it fits from behind, and then back left again if doing so
	 * left a gap at the end that earlier text could fill.
	 */
	private void ensureCaretVisible(ListScreen screen) {
		if (caret > text.length()) caret = text.length();
		if (anchor > text.length()) anchor = text.length();
		if (scrollIndex > caret) scrollIndex = caret;
		int room = innerWidth();
		while (scrollIndex < caret && screen.gWidth(text.substring(scrollIndex, caret)) > room) scrollIndex++;
		while (scrollIndex > 0 && screen.gWidth(text.substring(scrollIndex - 1)) <= room) scrollIndex--;
	}

	private String visibleText(ListScreen screen) {
		int room = innerWidth();
		int end = scrollIndex;
		while (end < text.length() && screen.gWidth(text.substring(scrollIndex, end + 1)) <= room) end++;
		return text.substring(scrollIndex, end);
	}

	// ---------------------------------------------------------------- mouse

	/** Puts the caret where the click landed and starts a drag-select. Clicks outside are ignored. */
	public void mouseClicked(ListScreen screen, int mouseX, int mouseY) {
		if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) return;
		caret = anchor = indexAt(screen, mouseX);
		dragging = true;
		blink = 0;
	}

	/**
	 * Extends the selection to the cursor while the button is held. b1.7.3 has no drag event of its own;
	 * plain mouse MOVEMENT arrives as a release with button -1, which is what {@code TextPopup} forwards
	 * here - see {@code ListScreen.mouseReleased}.
	 */
	public void mouseDragged(ListScreen screen, int mouseX) {
		if (!dragging) return;
		caret = indexAt(screen, mouseX);
		blink = 0;
	}

	public void mouseReleased() {
		dragging = false;
	}

	/** The character boundary nearest {@code mouseX}, clamped to what is currently on screen. */
	private int indexAt(ListScreen screen, int mouseX) {
		int target = mouseX - (x + TEXT_INSET);
		if (target <= 0) return scrollIndex;
		int index = scrollIndex;
		while (index < text.length() && screen.gWidth(text.substring(scrollIndex, index + 1)) <= target) index++;
		return index;
	}

	// ---------------------------------------------------------------- keyboard

	/** @return true when the key was consumed; false leaves it for the caller (Enter, Escape, ...). */
	public boolean keyPressed(char character, int keyCode) {
		blink = 0;
		boolean command = isCommandDown();
		boolean extend = isShiftDown();

		if (command) {
			switch (keyCode) {
				case Keyboard.KEY_A:
					anchor = 0;
					caret = text.length();
					return true;
				case Keyboard.KEY_C:
					copySelection();
					return true;
				case Keyboard.KEY_X:
					copySelection();
					deleteSelection();
					return true;
				case Keyboard.KEY_V:
					insert(Screen.getClipboard());
					return true;
				default:
					break;
			}
		}

		switch (keyCode) {
			case Keyboard.KEY_LEFT:
				// With a selection and no Shift, an arrow collapses to that end rather than stepping one
				// character from the moving end - what every other text field does.
				if (command) moveTo(wordBoundaryLeft(), extend);
				else if (hasSelection() && !extend) moveTo(selectionStart(), false);
				else moveTo(Math.max(0, caret - 1), extend);
				return true;
			case Keyboard.KEY_RIGHT:
				if (command) moveTo(wordBoundaryRight(), extend);
				else if (hasSelection() && !extend) moveTo(selectionEnd(), false);
				else moveTo(Math.min(text.length(), caret + 1), extend);
				return true;
			case Keyboard.KEY_HOME:
				moveTo(0, extend);
				return true;
			case Keyboard.KEY_END:
				moveTo(text.length(), extend);
				return true;
			case Keyboard.KEY_BACK: {
				if (deleteSelection()) return true;
				if (caret == 0) return true;
				int from = command ? wordBoundaryLeft() : caret - 1;
				text.delete(from, caret);
				caret = anchor = from;
				return true;
			}
			case Keyboard.KEY_DELETE: {
				if (deleteSelection()) return true;
				if (caret >= text.length()) return true;
				text.delete(caret, command ? wordBoundaryRight() : caret + 1);
				anchor = caret;
				return true;
			}
			default:
				break;
		}

		// Everything printable. The command check matters on macOS, where Cmd+C still reports 'c' as the
		// event character - without it the copy chord would type a letter as well as copying.
		if (!command && character >= 32 && character != 127) {
			insert(String.valueOf(character));
			return true;
		}
		return false;
	}

	/** Moves the caret, dragging the anchor with it unless the selection is being extended. */
	private void moveTo(int index, boolean extendSelection) {
		caret = Math.max(0, Math.min(text.length(), index));
		if (!extendSelection) anchor = caret;
	}

	/** @return true when there was a selection to remove. */
	private boolean deleteSelection() {
		if (!hasSelection()) return false;
		int start = selectionStart();
		text.delete(start, selectionEnd());
		caret = anchor = start;
		return true;
	}

	private void copySelection() {
		if (!hasSelection()) return;
		String selected = text.substring(selectionStart(), selectionEnd());
		try {
			// Vanilla only ever reads the clipboard (Screen.getClipboard), so writing it means going to
			// AWT directly - the same toolkit that call sits on top of.
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selected), null);
		} catch (Exception e) {
			// A clipboard owned by another process, or none at all: losing a copy is not worth a crash.
			RetroTweaks.LOGGER.warn("Could not write to the system clipboard", e);
		}
	}

	private void insert(String addition) {
		if (addition == null || addition.isEmpty()) {
			deleteSelection();
			return;
		}
		StringBuilder clean = new StringBuilder(addition.length());
		for (int i = 0; i < addition.length(); i++) {
			char c = addition.charAt(i);
			if (c >= 32 && c != 127) clean.append(c);
		}
		deleteSelection();
		int room = maxLength - text.length();
		if (room <= 0) return;
		if (clean.length() > room) clean.setLength(room);
		text.insert(caret, clean);
		caret = anchor = caret + clean.length();
	}

	/** Ctrl on Windows and Linux, Cmd on macOS - both accepted everywhere rather than probing the OS. */
	private static boolean isCommandDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
			|| Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA);
	}

	private static boolean isShiftDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}

	/** Start of the run of non-spaces the caret is in or just past - Ctrl+Left's destination. */
	private int wordBoundaryLeft() {
		int index = caret;
		while (index > 0 && text.charAt(index - 1) == ' ') index--;
		while (index > 0 && text.charAt(index - 1) != ' ') index--;
		return index;
	}

	/** End of the run of non-spaces the caret is in or just before - Ctrl+Right's destination. */
	private int wordBoundaryRight() {
		int index = caret;
		int length = text.length();
		while (index < length && text.charAt(index) == ' ') index++;
		while (index < length && text.charAt(index) != ' ') index++;
		return index;
	}
}
