package com.periut.retrotweaks.client.gui;

import com.periut.retroapi.config.ConfigSync;
import com.periut.retroapi.config.ConfigTree;
import com.periut.retroapi.config.RetroConfig;
import com.periut.retroapi.config.RetroConfigs;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * Every mod that has registered a config, one row each.
 *
 * <p>Reached from the "Mods" button on the top config page. Rows come from
 * {@link RetroConfigs#all()} in registration order, so a mod appears here purely by having called
 * {@code RetroConfigs.register} - there is nothing else to add and nothing to list it in.
 *
 * <p>Each row opens that mod's own {@link ConfigScreen}, which is the same screen this one was opened
 * from: the same widgets, the same descriptions, the same Defaults, and the same red asterisks on
 * anything a server owns. That equality is the point of the API - a mod's settings page is not a
 * lesser version of RetroTweaks', it IS RetroTweaks'.
 */
@Environment(EnvType.CLIENT)
public class ModsScreen extends ListScreen {

	/**
	 * The screen outside the config system. This page has nothing above it - it was opened sideways
	 * from the top config page, not down into - so leaving it leaves entirely rather than putting that
	 * page back for one more press of Escape.
	 */
	private final Screen exit;

	public ModsScreen(Screen exit) {
		this.exit = exit;
	}

	@Override
	protected String title() {
		return "Configs > Mods";
	}

	@Override
	protected void buildRows(List<Row> out) {
		for (final RetroConfig config : RetroConfigs.all()) {
			out.add(new ModRow(config));
		}
	}

	@Override
	protected void buildFooter(List<FooterButton> out) {
		out.add(new FooterButton("Done", this::done));
	}

	/** Says once, here, what the asterisks on the pages below will mean. */
	@Override
	protected String footnote() {
		if (!ConfigSync.canEditServerConfig()) {
			return null;
		}
		for (final Row row : rows) {
			if (row.serverEdit()) {
				return ListScreen.SERVER_EDIT_MARK + " Edits affect server config";
			}
		}
		return null;
	}

	private void done() {
		RetroConfigs.saveAll();
		this.minecraft.setScreen(exit);
	}

	/** Escape steps back one level, as it does on every other page here. */
	@Override
	protected void keyPressed(char character, int keyCode) {
		if (!hasPopup() && keyCode == Keyboard.KEY_ESCAPE) {
			done();
			return;
		}
		super.keyPressed(character, keyCode);
	}

	private final class ModRow extends Row {
		private final RetroConfig config;

		ModRow(final RetroConfig config) {
			this.config = config;
		}

		@Override
		public String label() {
			return config.name();
		}

		@Override
		public String value() {
			return ">";
		}

		/** The mod id, which is also its config file's name - the thing to look for on disk. */
		@Override
		public String subLabel() {
			return config.id() + ".json";
		}

		/** True when anything inside belongs to the server and this player may change it. */
		@Override
		public boolean serverEdit() {
			if (!ConfigSync.canEditServerConfig()) {
				return false;
			}
			final boolean[] found = { false };
			ConfigTree.forEachOption(config.tree(), option -> found[0] |= ConfigSync.isServerScoped(option));
			return found[0];
		}

		@Override
		public boolean enabled() {
			return config.tree().hasContent();
		}

		@Override
		public String hint() {
			final String description = FabricLoader.getInstance().getModContainer(config.id())
				.map(container -> container.getMetadata().getDescription())
				.orElse("");
			return description.isEmpty() ? config.name() + " settings" : description;
		}

		@Override
		public void click(final ListScreen screen, final int relativeX, final int button) {
			if (button == 0) {
				minecraft.setScreen(new ConfigScreen(ModsScreen.this, exit, config));
			}
		}
	}
}
