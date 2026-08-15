package com.periut.retrotweaks.client.gui.multiplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The real, persistent multiplayer server list. From MojangFix's "Enable Multiplayer Server
 * Changes". Replaces vanilla's {@link net.minecraft.client.gui.screen.MultiplayerScreen}, which is
 * just a single IP text field with no memory of servers you have visited before.
 *
 * <p>Ported from
 * {@code pl.telvarost.mojangfixstationapi.client.gui.multiplayer.MultiplayerScreen}. The reference
 * dispatches its own buttons through a lambda-callback widget plus a global {@code Screen} mixin
 * (its {@code CallbackButtonWidget}/{@code ScreenMixin}) - RetroTweaks already has an equivalent for
 * screens it does not own ({@code ExtraButton}), but this screen is entirely our own, so it uses
 * plain ids and a {@code buttonClicked} override instead, the same way {@code ConfigScreen} and
 * {@code TextEditScreen} do. Behaviour is unchanged; only the button-dispatch plumbing is native.
 */
@Environment(EnvType.CLIENT)
public class MultiplayerScreen extends Screen {

	private static final int ID_CONNECT = 100;
	private static final int ID_DIRECT_CONNECT = 101;
	private static final int ID_ADD_SERVER = 102;
	private static final int ID_EDIT = 103;
	private static final int ID_DELETE = 104;
	private static final int ID_CANCEL = 105;

	/** Passed to {@link #confirmed(boolean, int)} for the "delete this server?" dialog. */
	private static final int CONFIRM_DELETE = 0;

	private final Screen parent;
	private String title;
	private boolean joining;

	private ServerData selectedServer;
	private List<ServerData> serversList;

	private MultiplayerServerListWidget serverListWidget;

	private ButtonWidget buttonEdit;
	private ButtonWidget buttonConnect;
	private ButtonWidget buttonDelete;

	public MultiplayerScreen() {
		this(new TitleScreen());
	}

	public MultiplayerScreen(Screen parent) {
		this.parent = parent;
	}

	@Override
	public void init() {
		this.title = TranslationStorage.getInstance().get("multiplayer.title");
		this.loadServers();
		this.serverListWidget = new MultiplayerServerListWidget(this);
		this.initButtons();
	}

	private void loadServers() {
		this.selectedServer = null;

		try {
			File serversFile = new File(Minecraft.getRunDirectory(), "servers.dat");
			if (serversFile.exists()) {
				NbtCompound nbt = NbtIo.read(new DataInputStream(new FileInputStream(serversFile)));
				this.serversList = ServerData.load(nbt.getList("servers"));
			} else {
				this.serversList = new ArrayList<>();
				this.saveServers();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initButtons() {
		TranslationStorage translations = TranslationStorage.getInstance();
		this.buttons.add(this.buttonConnect = new ButtonWidget(ID_CONNECT, this.width / 2 - 150 - 4, this.height - 52, 100, 20, translations.get("multiplayer.connect")));
		this.buttons.add(new ButtonWidget(ID_DIRECT_CONNECT, this.width / 2 - 50, this.height - 52, 100, 20, "Direct Connect"));
		this.buttons.add(new ButtonWidget(ID_ADD_SERVER, this.width / 2 + 50 + 4, this.height - 52, 100, 20, "Add Server"));
		this.buttons.add(this.buttonEdit = new ButtonWidget(ID_EDIT, this.width / 2 - 154, this.height - 28, 70, 20, "Edit"));
		this.buttons.add(this.buttonDelete = new ButtonWidget(ID_DELETE, this.width / 2 - 74, this.height - 28, 70, 20, translations.get("selectWorld.delete")));
		this.buttons.add(new ButtonWidget(ID_CANCEL, this.width / 2 + 4, this.height - 28, 150, 20, translations.get("gui.cancel")));
		this.buttonConnect.active = false;
		this.buttonEdit.active = false;
		this.buttonDelete.active = false;
	}

	@Override
	protected void buttonClicked(ButtonWidget button) {
		if (!button.active) return;
		switch (button.id) {
			case ID_CONNECT:
				this.joinServer(this.selectedServer);
				return;
			case ID_DIRECT_CONNECT:
				this.minecraft.setScreen(new DirectConnectScreen(this));
				return;
			case ID_ADD_SERVER:
				this.minecraft.setScreen(new EditServerScreen(this, null));
				return;
			case ID_EDIT:
				this.minecraft.setScreen(new EditServerScreen(this, this.selectedServer));
				return;
			case ID_DELETE:
				TranslationStorage translations = TranslationStorage.getInstance();
				this.minecraft.setScreen(new ConfirmScreen(this,
					"Are you sure you want to remove this server?",
					"'" + this.selectedServer.getName() + "' " + translations.get("selectWorld.deleteWarning"),
					translations.get("selectWorld.deleteButton"),
					translations.get("gui.cancel"),
					CONFIRM_DELETE));
				return;
			case ID_CANCEL:
				this.minecraft.setScreen(this.parent);
				return;
			default:
		}

		this.serverListWidget.buttonClicked(button);
	}

	@Override
	public void confirmed(boolean confirmed, int id) {
		if (id == CONFIRM_DELETE && confirmed) {
			this.deleteServer(this.selectedServer);
		}
		this.minecraft.setScreen(this);
	}

	public void selectServer(int slot, boolean join) {
		this.selectedServer = this.serversList.get(slot);
		boolean selected = this.selectedServer != null;

		this.buttonEdit.active = selected;
		this.buttonDelete.active = selected;
		this.buttonConnect.active = selected;

		if (selected && join) {
			this.joinServer(this.selectedServer);
		}
	}

	public void joinServer(ServerData server) {
		this.minecraft.setScreen(null);
		if (!this.joining) {
			this.joining = true;
			DirectConnectScreen.connect(this.minecraft, server.getIp());
		}
	}

	public void deleteServer(ServerData server) {
		this.serversList.remove(server);
		this.saveServers();
	}

	public void saveServers() {
		try {
			NbtCompound compound = new NbtCompound();
			compound.put("servers", ServerData.save(this.serversList));
			NbtIo.write(compound, new DataOutputStream(new FileOutputStream(new File(Minecraft.getRunDirectory(), "servers.dat"))));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		this.serverListWidget.render(mouseX, mouseY, delta);
		this.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
		super.render(mouseX, mouseY, delta);
	}

	public ServerData getSelectedServer() {
		return selectedServer;
	}

	public List<ServerData> getServersList() {
		return serversList;
	}

	public Minecraft getMinecraft() {
		return this.minecraft;
	}

	public TextRenderer getFontRenderer() {
		return this.textRenderer;
	}
}
