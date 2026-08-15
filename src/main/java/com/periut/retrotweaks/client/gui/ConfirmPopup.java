package com.periut.retrotweaks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.List;

/**
 * "Are you sure?" for the one action on these screens that destroys work: restoring defaults.
 *
 * <p>Every other control here is its own undo - click the checkbox again, pick the old value back out
 * of the dropdown. Defaults is not: it can wipe a whole page, and at the top level every page at once,
 * with a single click on a button that sits right next to Done. So it asks first, and the question
 * names exactly how much is about to go.
 */
@Environment(EnvType.CLIENT)
public final class ConfirmPopup extends Popup {

	private static final int LINE_HEIGHT = 10;
	private static final int MAX_LINES = 4;

	private final String message;
	/** A second paragraph in {@link ListScreen#TEXT_WARN}, or null. */
	private final String warning;
	private final Button[] buttons;
	private List<String> lines = List.of();
	private List<String> warningLines = List.of();

	public ConfirmPopup(String title, String message, String confirmText, Runnable onConfirm) {
		this(title, message, null, confirmText, onConfirm);
	}

	/**
	 * The same question with a consequence that reaches past this client spelled out under it, in the
	 * red the asterisks use. Kept separate from {@code message} rather than appended to it so it cannot
	 * be wrapped into the middle of a sentence and read as part of the ordinary warning.
	 */
	public ConfirmPopup(String title, String message, String warning, String confirmText, Runnable onConfirm) {
		super(title);
		this.message = message;
		this.warning = warning == null || warning.isEmpty() ? null : warning;
		this.buttons = new Button[]{
			new Button("Cancel", () -> {}),
			new Button(confirmText, onConfirm),
		};
	}

	@Override
	public void layout(ListScreen screen) {
		width = Math.min(Math.max(230, screen.gWidth(title) + PAD * 2), Math.max(180, screen.screenWidth() - 16));
		lines = screen.wrap(message, width - PAD * 2, MAX_LINES);
		warningLines = warning == null ? List.of() : screen.wrap(warning, width - PAD * 2, MAX_LINES);
		int textLines = Math.max(1, lines.size()) + (warningLines.isEmpty() ? 0 : warningLines.size() + 1);
		height = TITLE_HEIGHT + 5 + textLines * LINE_HEIGHT + 6 + BUTTON_HEIGHT + PAD;
		placeAtBottom(screen);
		layoutButtonRow(buttons, y + height - PAD - BUTTON_HEIGHT + 2);
	}

	@Override
	public void render(ListScreen screen, int mouseX, int mouseY) {
		drawFrame(screen);
		for (int i = 0; i < lines.size(); i++) {
			screen.gText(screen.trim(lines.get(i), width - PAD * 2), x + PAD,
				y + TITLE_HEIGHT + 6 + i * LINE_HEIGHT, ListScreen.TEXT_DIM);
		}
		// One blank line between the two, so the red reads as its own statement rather than as the
		// tail of the grey one.
		int warningTop = y + TITLE_HEIGHT + 6 + (lines.size() + 1) * LINE_HEIGHT;
		for (int i = 0; i < warningLines.size(); i++) {
			screen.gText(screen.trim(warningLines.get(i), width - PAD * 2), x + PAD,
				warningTop + i * LINE_HEIGHT, ListScreen.TEXT_WARN);
		}
		for (Button button : buttons) button.render(screen, mouseX, mouseY);
	}

	@Override
	public void mouseClicked(ListScreen screen, int mouseX, int mouseY, int button) {
		if (button != 0) return;
		if (!inside(mouseX, mouseY)) {
			screen.closePopup();
			return;
		}
		// Closed before the action runs, so the action is free to rebuild the screen underneath it.
		if (buttons[1].contains(mouseX, mouseY)) {
			screen.playClick();
			screen.closePopup();
			buttons[1].action.run();
			return;
		}
		if (buttons[0].contains(mouseX, mouseY)) {
			screen.playClick();
			screen.closePopup();
		}
	}
}
