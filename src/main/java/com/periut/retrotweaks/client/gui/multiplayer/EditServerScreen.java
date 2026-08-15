package com.periut.retrotweaks.client.gui.multiplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.TranslationStorage;

import org.lwjgl.input.Keyboard;

/**
 * Add-a-server / edit-a-server screen. From MojangFix.
 *
 * <p>Ported from
 * {@code pl.telvarost.mojangfixstationapi.client.gui.multiplayer.EditServerScreen}. The two text
 * fields are plain vanilla {@link TextFieldWidget}s, the same as {@code TextEditScreen} uses - this
 * mod does not touch text input itself (see {@code TEXT_FEATURES.txt}), and neither did the
 * reference screen.
 *
 * <p>Fixed relative to the reference: it compared the typed {@code character} against
 * {@code Keyboard.KEY_RETURN} (a keycode, 28) rather than testing {@code keyCode}, so the comparison
 * never matched and Enter did nothing in MojangFix's own Add/Edit Server screen. Its sibling
 * {@code DirectConnectScreen} gets this right two files over, which is what gives away that it was a
 * slip rather than a decision. Ported behaviour is what is wanted here, not ported typos.
 */
@Environment(EnvType.CLIENT)
public class EditServerScreen extends Screen {

	private static final int ID_SAVE = 0;
	private static final int ID_CANCEL = 1;

	/**
	 * Name stored when the server-name field is left blank, and the text of the ghost hint drawn in
	 * that field while it is blank. One constant for both so the hint can never drift out of sync
	 * with what actually gets saved.
	 */
	private static final String DEFAULT_SERVER_NAME = "Minecraft Server";

	private final ServerData server;
	private final MultiplayerScreen parent;
	private ButtonWidget button;
	private TextFieldWidget nameTextField;
	private TextFieldWidget ipTextField;

	public EditServerScreen(MultiplayerScreen parent, ServerData server) {
		this.parent = parent;
		this.server = server;
	}

	@Override
	public void tick() {
		this.nameTextField.tick();
		this.ipTextField.tick();
	}

	@Override
	public void init() {
		Keyboard.enableRepeatEvents(true);
		this.buttons.add(this.button = new ButtonWidget(ID_SAVE, this.width / 2 - 100, this.height / 4 + 96 + 12,
			this.server == null ? "Add Server" : "Edit"));
		this.buttons.add(new ButtonWidget(ID_CANCEL, this.width / 2 - 100, this.height / 4 + 120 + 12,
			TranslationStorage.getInstance().get("gui.cancel")));
		this.nameTextField = new TextFieldWidget(this, this.textRenderer, this.width / 2 - 100, 60, 200, 20,
			this.server == null ? "" : this.server.getName());
		this.nameTextField.setMaxLength(32);
		// Focused on open, matching DirectConnectScreen's single field: typing works immediately
		// without a click, and it gives Tab a defined starting point (name -> address -> name ...).
		this.nameTextField.focused = true;
		this.ipTextField = new TextFieldWidget(this, this.textRenderer, this.width / 2 - 100, 106, 200, 20,
			this.server == null ? "" : this.server.getIp());
		this.ipTextField.setMaxLength(32);
		this.updateButton();
	}

	/**
	 * Only the address is required. A blank name is filled in with {@link #DEFAULT_SERVER_NAME} on
	 * save (see {@link #resolvedServerName()}), so it never blocks the Add/Edit button.
	 */
	private void updateButton() {
		this.button.active = this.ipTextField.getText().trim().length() > 0;
	}

	/**
	 * The name to save: the field's own text, or {@link #DEFAULT_SERVER_NAME} if that text is blank
	 * (or whitespace-only, per the same {@code trim()} convention {@link #updateButton()} uses) so an
	 * entry never ends up nameless in the server list.
	 */
	private String resolvedServerName() {
		String name = this.nameTextField.getText();
		return name.trim().length() > 0 ? name : DEFAULT_SERVER_NAME;
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (!button.active) return;
		switch (button.id) {
			case ID_SAVE:
				if (this.server != null) {
					this.server.setName(this.resolvedServerName());
					this.server.setIp(this.ipTextField.getText());
				} else {
					this.parent.getServersList().add(new ServerData(this.resolvedServerName(), this.ipTextField.getText()));
				}
				this.parent.saveServers();
				this.minecraft.setScreen(this.parent);
				return;
			case ID_CANCEL:
				this.minecraft.setScreen(this.parent);
				return;
			default:
		}
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (keyCode == Keyboard.KEY_TAB) {
			// Handled here instead of being forwarded like every other key: TextFieldWidget#keyPressed
			// already calls Screen#handleTab() by itself when the field it is called on is focused and
			// sees a tab character. Forwarding Tab to both fields, as done below for everything else,
			// would fire that callback twice in one press - once from the field that had focus, and
			// again from the field #handleTab() just handed focus to - toggling focus right back and
			// making Tab a no-op. Going through #handleTab() directly, once, avoids that.
			this.handleTab();
			return;
		}
		this.nameTextField.keyPressed(character, keyCode);
		this.ipTextField.keyPressed(character, keyCode);
		this.updateButton();
		if (keyCode == Keyboard.KEY_RETURN) {
			this.buttonClicked(this.button);
		}
	}

	/** Moves focus between the two fields; see the Tab handling in {@link #keyPressed(char, int)}. */
	@Override
	public void handleTab() {
		if (this.nameTextField.focused) {
			this.nameTextField.setFocused(false);
			this.ipTextField.setFocused(true);
		} else {
			this.ipTextField.setFocused(false);
			this.nameTextField.setFocused(true);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		this.nameTextField.mouseClicked(mouseX, mouseY, button);
		this.ipTextField.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		this.renderBackground();
		this.drawCenteredTextWithShadow(this.textRenderer,
			this.server == null ? "Add Server" : "Edit Server Info", this.width / 2, 20, 0xFFFFFF);
		this.drawTextWithShadow(this.textRenderer, "Server Name:", this.width / 2 - 100, 47, 0xA0A0A0);
		this.drawTextWithShadow(this.textRenderer, "Server Address:", this.width / 2 - 100, 94, 0xA0A0A0);
		this.nameTextField.render();
		if (this.nameTextField.getText().trim().length() == 0) {
			// TextFieldWidget has no placeholder support and paints its own background inside
			// render(), so the hint is drawn here, after that call, at the same
			// (x + 4, y + (height - 8) / 2) spot vanilla uses for the field's real text, and in
			// vanilla's own "disabled field" grey so it unmistakably reads as a hint rather than as
			// typed content.
			//
			// While the field is focused vanilla blinks a "_" caret at exactly that spot, so the hint
			// is pushed right by one caret width to sit beside it instead of underneath it. The shift
			// is unconditional rather than tied to the blink, so the hint stays put and only the caret
			// flashes - a hint that jittered twice a second would be worse than either problem.
			boolean focused = this.nameTextField.focused;
			int caret = focused ? this.textRenderer.getWidth("_") : 0;
			this.drawTextWithShadow(this.textRenderer, DEFAULT_SERVER_NAME,
				this.width / 2 - 100 + 4 + caret, 60 + (20 - 8) / 2, 0x707070);
		}
		this.ipTextField.render();
		super.render(mouseX, mouseY, delta);
	}
}
