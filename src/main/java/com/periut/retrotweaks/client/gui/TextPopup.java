package com.periut.retrotweaks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;

/**
 * Types a string option's value: the custom version text, the resource URLs.
 *
 * <p>The field itself is {@link TextInput} rather than vanilla's {@code TextFieldWidget} - see that
 * class for why. This is modal because only one field can hold the keyboard at a time, and unlike the
 * rest of the popups an outside click does NOT dismiss it: half-typed text is worth losing only on an
 * explicit Cancel or Escape.
 */
@Environment(EnvType.CLIENT)
public final class TextPopup extends Popup {

	private static final int FIELD_HEIGHT = 20;

	private final String defaultValue;
	private final Consumer<String> onSave;
	private final TextInput field;

	private final Button[] buttons;

	public TextPopup(String title, String initial, String defaultValue, Consumer<String> onSave) {
		super(title);
		this.defaultValue = defaultValue;
		this.onSave = onSave;
		this.field = new TextInput(initial);
		this.field.setMaxLength(4096);
		this.buttons = new Button[]{
			new Button("Default", () -> field.setText(defaultValue)),
			new Button("Cancel", () -> {}),
			new Button("Save", () -> {}),
		};
	}

	@Override
	public void layout(ListScreen screen) {
		width = Math.min(Math.max(240, screen.gWidth(title) + PAD * 2), Math.max(160, screen.screenWidth() - 16));
		height = TITLE_HEIGHT + 5 + FIELD_HEIGHT + 6 + BUTTON_HEIGHT + PAD;
		placeAtBottom(screen);
		field.setBounds(x + PAD, y + TITLE_HEIGHT + 6, width - PAD * 2, FIELD_HEIGHT);
		layoutButtonRow(buttons, y + height - PAD - BUTTON_HEIGHT + 2);
	}

	@Override
	public void render(ListScreen screen, int mouseX, int mouseY) {
		drawFrame(screen);
		field.render(screen);
		for (Button button : buttons) button.render(screen, mouseX, mouseY);
	}

	@Override
	public void mouseClicked(ListScreen screen, int mouseX, int mouseY, int button) {
		if (button != 0) return;
		if (buttons[0].contains(mouseX, mouseY)) {
			screen.playClick();
			field.setText(defaultValue);
			return;
		}
		if (buttons[1].contains(mouseX, mouseY)) {
			screen.playClick();
			screen.closePopup();
			return;
		}
		if (buttons[2].contains(mouseX, mouseY)) {
			screen.playClick();
			commit(screen);
			return;
		}
		field.mouseClicked(screen, mouseX, mouseY);
	}

	@Override
	public void mouseReleased(ListScreen screen, int mouseX, int mouseY, int button) {
		if (button == 0) field.mouseReleased();
		else field.mouseDragged(screen, mouseX);
	}

	@Override
	public void keyPressed(ListScreen screen, char character, int keyCode) {
		if (keyCode == Keyboard.KEY_ESCAPE) {
			screen.closePopup();
			return;
		}
		if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
			commit(screen);
			return;
		}
		field.keyPressed(character, keyCode);
	}

	private void commit(ListScreen screen) {
		String text = field.getText();
		screen.closePopup();
		onSave.accept(text);
	}

	@Override
	public void tick() {
		field.tick();
	}
}
