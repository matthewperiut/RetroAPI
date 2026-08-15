package com.periut.retrotweaks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.input.Keyboard;

/**
 * Drags a numeric option. Everything is expressed as a 0..1 fraction, the same convention vanilla's
 * own {@code SliderWidget} uses, so the same popup drives a {@code ConfigTree} option, one of
 * RetroTweaks' added video options and RetroDragon's render scale without knowing what any of them are.
 *
 * <p>A slider in a twelve-pixel row would be a two-pixel-tall track that nobody could hit, which is why
 * numbers open this instead of being dragged in place. That buys a bigger track, a readable value, and
 * arrow keys for the cases where an exact number matters and a drag will not reliably land on it.
 */
@Environment(EnvType.CLIENT)
public final class SliderPopup extends Popup {

	/** What the slider is actually attached to. Implementations read and write live, with no cache. */
	public interface Model {
		float fraction();

		void setFraction(float fraction);

		/** The value as the user should read it, e.g. "12 chunks". */
		String display();

		/** How far one arrow-key press or one -/+ tap moves the fraction. */
		float step();

		/**
		 * Called once when the mouse button comes back up after any change. Most models need nothing
		 * here; the video options do, because {@code GameOptionsMixin} defers snapping to a whole step
		 * (and, for GUI scale, actually applying it) until the button is released, and nothing else
		 * would tell it that moment had arrived.
		 */
		default void released() {}
	}

	private static final int TRACK_HEIGHT = 14;
	private static final int HANDLE_WIDTH = 6;
	private static final int STEP_BUTTON_WIDTH = 14;

	private final Model model;
	private final Runnable onDefault;

	private final Button[] buttons;
	private int trackX;
	private int trackY;
	private int trackWidth;
	private boolean dragging;
	/** Set by any change, cleared by {@link Model#released()}, so a -/+ tap gets the same release the
	 *  end of a drag does - see that method for why the distinction matters. */
	private boolean changedSincePress;

	/** {@code onDefault} may be null, in which case no "Default" button is offered. */
	public SliderPopup(String title, Model model, Runnable onDefault) {
		super(title);
		this.model = model;
		this.onDefault = onDefault;
		this.buttons = onDefault == null
			? new Button[]{new Button("Done", () -> {})}
			: new Button[]{new Button("Default", onDefault), new Button("Done", () -> {})};
	}

	@Override
	public void layout(ListScreen screen) {
		width = Math.min(Math.max(220, screen.gWidth(title) + PAD * 2), Math.max(160, screen.screenWidth() - 16));
		height = TITLE_HEIGHT + 4 + 10 + 4 + TRACK_HEIGHT + 6 + BUTTON_HEIGHT + PAD;
		placeAtBottom(screen);

		trackY = y + TITLE_HEIGHT + 18;
		trackX = x + PAD + STEP_BUTTON_WIDTH + 4;
		trackWidth = width - PAD * 2 - (STEP_BUTTON_WIDTH + 4) * 2;
		layoutButtonRow(buttons, y + height - PAD - BUTTON_HEIGHT + 2);
	}

	@Override
	public void render(ListScreen screen, int mouseX, int mouseY) {
		if (dragging) setFromMouse(mouseX);
		drawFrame(screen);

		screen.gTextCentered(model.display(), x + width / 2, y + TITLE_HEIGHT + 4, ListScreen.TEXT);

		int minusX = x + PAD;
		int plusX = x + width - PAD - STEP_BUTTON_WIDTH;
		screen.drawButton(minusX, trackY, STEP_BUTTON_WIDTH, TRACK_HEIGHT, "-", true,
			hits(mouseX, mouseY, minusX, trackY, STEP_BUTTON_WIDTH, TRACK_HEIGHT));
		screen.drawButton(plusX, trackY, STEP_BUTTON_WIDTH, TRACK_HEIGHT, "+", true,
			hits(mouseX, mouseY, plusX, trackY, STEP_BUTTON_WIDTH, TRACK_HEIGHT));

		float fraction = clamp(model.fraction());
		screen.gFill(trackX, trackY, trackX + trackWidth, trackY + TRACK_HEIGHT, ListScreen.BOX_BG);
		int handleX = trackX + Math.round(fraction * (trackWidth - HANDLE_WIDTH));
		// Everything left of the handle is filled in, so the value is readable at a glance from the bar
		// alone rather than only from the number above it.
		screen.gFill(trackX + 1, trackY + 1, handleX, trackY + TRACK_HEIGHT - 1, 0xFF303030);
		boolean hot = dragging || hits(mouseX, mouseY, trackX, trackY, trackWidth, TRACK_HEIGHT);
		screen.gFill(handleX, trackY, handleX + HANDLE_WIDTH, trackY + TRACK_HEIGHT,
			hot ? ListScreen.TEXT_HOVER : ListScreen.SCROLL_THUMB_HOVER);
		screen.outline(trackX, trackY, trackWidth, TRACK_HEIGHT, hot ? ListScreen.TEXT_HOVER : ListScreen.BOX_BORDER);

		for (Button button : buttons) button.render(screen, mouseX, mouseY);
	}

	private static boolean hits(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static float clamp(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private void setFromMouse(int mouseX) {
		model.setFraction(clamp((mouseX - trackX - HANDLE_WIDTH / 2.0F) / (trackWidth - HANDLE_WIDTH)));
		changedSincePress = true;
	}

	private void nudge(int direction) {
		model.setFraction(clamp(model.fraction() + direction * model.step()));
		// An arrow key never produces a mouse release, so the model is told right away. A -/+ tap does
		// produce one, and is flagged as well so it gets the release too - the immediate call there
		// lands while the button is still down, which is exactly the case the release is needed for.
		changedSincePress = true;
		model.released();
	}

	@Override
	public void mouseClicked(ListScreen screen, int mouseX, int mouseY, int button) {
		if (button != 0) return;
		if (!inside(mouseX, mouseY)) {
			screen.closePopup();
			return;
		}
		if (hits(mouseX, mouseY, x + PAD, trackY, STEP_BUTTON_WIDTH, TRACK_HEIGHT)) {
			screen.playClick();
			nudge(-1);
			return;
		}
		if (hits(mouseX, mouseY, x + width - PAD - STEP_BUTTON_WIDTH, trackY, STEP_BUTTON_WIDTH, TRACK_HEIGHT)) {
			screen.playClick();
			nudge(1);
			return;
		}
		if (hits(mouseX, mouseY, trackX, trackY, trackWidth, TRACK_HEIGHT)) {
			// Once, on the grab - exactly where vanilla's own slider makes the sound, and not again for
			// the rest of the drag, which is a stream of mouse events rather than a stream of presses.
			screen.playClick();
			dragging = true;
			setFromMouse(mouseX);
			return;
		}
		if (buttons[buttons.length - 1].contains(mouseX, mouseY)) {
			screen.playClick();
			screen.closePopup();
			return;
		}
		if (onDefault != null && buttons[0].contains(mouseX, mouseY)) {
			screen.playClick();
			onDefault.run();
		}
	}

	@Override
	public void mouseReleased(ListScreen screen, int mouseX, int mouseY, int button) {
		if (button != 0) return;
		dragging = false;
		if (changedSincePress) {
			changedSincePress = false;
			model.released();
		}
	}

	@Override
	public void keyPressed(ListScreen screen, char character, int keyCode) {
		switch (keyCode) {
			case Keyboard.KEY_LEFT -> nudge(-1);
			case Keyboard.KEY_RIGHT -> nudge(1);
			case Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_ESCAPE -> screen.closePopup();
			default -> { }
		}
	}
}
