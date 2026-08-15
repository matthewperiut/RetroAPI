package com.periut.retrotweaks.client.gui.multiplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.TranslationStorage;

import org.lwjgl.input.Keyboard;

/**
 * Connect to a server without saving it to the list. From MojangFix.
 *
 * <p>Ported from
 * {@code pl.telvarost.mojangfixstationapi.client.gui.multiplayer.DirectConnectScreen}, including
 * {@link #connect(Minecraft, String)} - the plain {@code split(":")} address parser both
 * {@link MultiplayerScreen#joinServer(ServerData)} and this screen use to reach {@link ConnectScreen}.
 * It does not special-case bracketed IPv6 addresses the way vanilla's own {@code MultiplayerScreen}
 * does; the reference does not either, so this does not.
 */
@Environment(EnvType.CLIENT)
public class DirectConnectScreen extends Screen {

	private static final int DEFAULT_PORT = 25565;

	private static final int ID_CONNECT = 0;
	private static final int ID_CANCEL = 1;

	private final Screen parent;
	private TextFieldWidget addressField;
	private ButtonWidget connectButton;

	public DirectConnectScreen(Screen parent) {
		this.parent = parent;
	}

	private static int parseIntWithDefault(String s, int def) {
		try {
			return Integer.parseInt(s.trim());
		} catch (Exception e) {
			return def;
		}
	}

	public static void connect(Minecraft minecraft, String addressText) {
		String[] split = addressText.split(":");
		minecraft.setScreen(new ConnectScreen(minecraft, split[0], split.length > 1 ? parseIntWithDefault(split[1], DEFAULT_PORT) : DEFAULT_PORT));
	}

	@Override
	public void tick() {
		this.addressField.tick();
	}

	@Override
	public void init() {
		Keyboard.enableRepeatEvents(true);

		TranslationStorage translations = TranslationStorage.getInstance();
		this.buttons.add(this.connectButton = new ButtonWidget(ID_CONNECT, this.width / 2 - 100, this.height / 4 + 96 + 12, translations.get("multiplayer.connect")));
		this.buttons.add(new ButtonWidget(ID_CANCEL, this.width / 2 - 100, this.height / 4 + 120 + 12, translations.get("gui.cancel")));

		String lastServer = this.minecraft.options.lastServer.replaceAll("_", ":");
		this.connectButton.active = lastServer.length() > 0;
		this.addressField = new TextFieldWidget(this, this.textRenderer, this.width / 2 - 100, this.height / 4 - 10 + 50 + 18, 200, 20, lastServer);
		this.addressField.focused = true;
		this.addressField.setMaxLength(128);
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (!button.active) return;
		switch (button.id) {
			case ID_CONNECT:
				String address = this.addressField.getText().trim();
				this.minecraft.options.lastServer = address.replaceAll(":", "_");
				this.minecraft.options.save();
				connect(this.minecraft, address);
				return;
			case ID_CANCEL:
				this.minecraft.setScreen(this.parent);
				return;
			default:
		}
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		this.addressField.keyPressed(character, keyCode);
		if (keyCode == Keyboard.KEY_RETURN) {
			this.buttonClicked(this.connectButton);
		}

		this.connectButton.active = this.addressField.getText().length() > 0;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		this.addressField.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		this.renderBackground();
		this.drawCenteredTextWithShadow(this.textRenderer, "Direct Connect", this.width / 2, this.height / 4 - 60 + 20, 0xFFFFFF);
		this.drawCenteredTextWithShadow(this.textRenderer, TranslationStorage.getInstance().get("multiplayer.ipinfo"), this.width / 2, this.height / 4 - 60 + 60 + 36, 0xA0A0A0);
		this.addressField.render();
		super.render(mouseX, mouseY, delta);
	}
}
