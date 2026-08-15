package com.periut.retrotweaks.client.gui;

import com.periut.retrotweaks.feature.options.KeyBindingAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.TranslationStorage;

import java.util.List;

/**
 * A controls screen that names the thing being rebound and does not run off the bottom of the window.
 *
 * <p>Vanilla lays every keybind out in a fixed two-column grid with no scrolling, so the moment a mod
 * adds keys - or the window is short - the last few are drawn off screen and cannot be rebound at all.
 * This scrolls, on the same list as the config screen next door, so the two feel like one mod.
 *
 * <p>It also says what each key DOES. Vanilla's grid puts the binding's name in a label beside its
 * button; the paged port that came before this kept only the buttons, which left a column of key names
 * ("W", "S", "E") with nothing to say which was which. Here the name is the row's label and the key is
 * its value, so the two can never come apart.
 *
 * <p>Rebinding is unchanged: click a row, press a key. Restoring one key to the default it was
 * constructed with (from MojangFix) is a right-click rather than a second button per row - a 40px
 * "Reset" beside every row cost more width than the key names did, and re-checking whether it should be
 * greyed out every frame only to have it inert most of the time was noise.
 */
@Environment(EnvType.CLIENT)
public class ControlsScreen extends ListScreen {

	private final Screen parent;
	private final GameOptions options;

	/** Index into {@link GameOptions#allKeys} that is waiting for a key press, or -1. */
	private int listeningFor = -1;

	public ControlsScreen(Screen parent, GameOptions options) {
		this.parent = parent;
		this.options = options;
	}

	@Override
	protected String title() {
		return TranslationStorage.getInstance().get("controls.title");
	}

	@Override
	protected void buildRows(List<Row> out) {
		for (int i = 0; i < options.allKeys.length; i++) out.add(new KeyRow(i));
	}

	@Override
	protected void buildFooter(List<FooterButton> out) {
		out.add(new FooterButton("Defaults", this::confirmResetAll));
		out.add(new FooterButton(TranslationStorage.getInstance().get("gui.done"),
			() -> this.minecraft.setScreen(parent)));
	}

	/** Asks first: this throws away every rebind at once, from a button next to Done. */
	private void confirmResetAll() {
		openPopup(new ConfirmPopup("Restore Defaults",
			"Restore all " + options.allKeys.length + " keys to their default bindings? This cannot be undone.",
			"Restore", this::resetAll));
	}

	private void resetAll() {
		for (KeyBinding keyBinding : options.allKeys) {
			keyBinding.code = ((KeyBindingAccessor) keyBinding).retrotweaks$getDefaultKeyCode();
		}
		options.save();
		listeningFor = -1;
	}

	private final class KeyRow extends Row {
		private final int index;

		KeyRow(int index) {
			this.index = index;
		}

		@Override
		public String label() {
			return options.getKeybindName(index);
		}

		/** The bound key, under the binding's name rather than beside it - see {@link Row#subLabel()}.
		 *  Both can be long, and a name cut in half is exactly what this screen exists to fix. */
		@Override
		public String subLabel() {
			String key = options.getKeybindKey(index);
			return listeningFor == index ? "> " + key + " <" : key;
		}

		@Override
		public String hint() {
			if (listeningFor == index) return "Press any key to bind it";
			KeyBinding keyBinding = options.allKeys[index];
			boolean changed = ((KeyBindingAccessor) keyBinding).retrotweaks$getDefaultKeyCode() != keyBinding.code;
			return changed
				? "Click to rebind, right-click to restore the default"
				: "Click to rebind";
		}

		@Override
		public void click(ListScreen screen, int relativeX, int button) {
			if (button == 1) {
				KeyBinding keyBinding = options.allKeys[index];
				keyBinding.code = ((KeyBindingAccessor) keyBinding).retrotweaks$getDefaultKeyCode();
				options.save();
				listeningFor = -1;
				return;
			}
			if (button == 0) listeningFor = index;
		}
	}

	/**
	 * While a row is listening every key goes to it, Escape included - vanilla's controls screen binds
	 * Escape too, and swallowing it here would make that the one key nobody could pick. Escape closes
	 * the screen only when nothing is listening, which is what {@link ListScreen#keyPressed} does.
	 */
	@Override
	protected void keyPressed(char character, int keyCode) {
		if (hasPopup() || listeningFor < 0) {
			super.keyPressed(character, keyCode);
			return;
		}
		options.setKeybindKey(listeningFor, keyCode);
		listeningFor = -1;
	}
}
