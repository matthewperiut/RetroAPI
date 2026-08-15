package com.periut.retroapi.config;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.util.Json;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * One mod's configuration: its option tree, its file, and everything the screen needs to show it.
 *
 * <p>Options are declared as plain annotated fields on a plain object rather than registered one by
 * one - see {@link Opt} and {@link Cat}. A mod hands over the object; this reflects over it, produces
 * the tree, reads {@code config/<id>.json} into it and writes the file back:
 *
 * <pre>{@code
 * public final class MyConfig {
 *     @Opt(name = "Shiny Blocks", desc = "Makes blocks shiny", scope = Scope.WORLD)
 *     public boolean shinyBlocks = true;
 *
 *     @Cat(name = "Sounds", scope = Scope.CLIENT)
 *     public final Sounds sounds = new Sounds();
 * }
 *
 * RetroConfigs.register("mymod", "My Mod", new MyConfig());
 * }</pre>
 *
 * <p>Read the values off the object's fields directly; nothing needs to go through this class at
 * runtime. The field always holds the EFFECTIVE value - the player's choice, unless another mod has
 * taken the feature over ({@link Opt#source()}) or a server is dictating it ({@link Scope#WORLD}) - so
 * a hot path is a field read with no lookup and no indirection.
 *
 * <p>The file is rewritten after every load, so a config from an older version of the mod gains the
 * options it was missing and keeps the ones it had: what is on disk always matches what the screen
 * offers.
 */
public final class RetroConfig {

	private final String id;
	private final String name;
	private final Object root;
	private final ConfigTree.Category tree;
	private final Path file;
	private final boolean freshInstall;
	private Runnable onSaved = () -> { };

	RetroConfig(final String id, final String name, final Object root) {
		this.id = id;
		this.name = name;
		this.root = root;
		this.tree = ConfigTree.build(id, name, root);
		this.file = FabricLoader.getInstance().getConfigDir().resolve(id + ".json");
		this.freshInstall = !Files.exists(file);
		load();
	}

	/** The mod id this belongs to, which is also the config file's name. */
	public String id() {
		return id;
	}

	/** What the screen calls it. */
	public String name() {
		return name;
	}

	/** The object whose fields the options are. */
	public Object root() {
		return root;
	}

	/** The reflected tree, which is what the screen walks. */
	public ConfigTree.Category tree() {
		return tree;
	}

	/** True when there was no config file at all when this was registered. */
	public boolean isFreshInstall() {
		return freshInstall;
	}

	/**
	 * Runs after every {@link #save()}, for work that has to follow a value changing rather than
	 * waiting for a restart - rebuilding recipes, recomputing a cached layout, handing a value to
	 * another mod. Optional; most configs need none.
	 */
	public RetroConfig onSaved(final Runnable action) {
		this.onSaved = action == null ? () -> { } : action;
		return this;
	}

	/**
	 * The value the PLAYER chose for the boolean at {@code dottedPath} (e.g.
	 * {@code "bugfixes.grassBlockItemFix"}), ignoring any stand-down - unlike the field itself, which
	 * reads false whenever another installed mod provides the feature.
	 *
	 * <p>For the rare path that is NOT the implementation the stand-down was aimed at, and so must keep
	 * running while the field says otherwise. Anything that would double-apply a feature must read the
	 * field instead.
	 */
	public boolean chosenBoolean(final String dottedPath, final boolean fallback) {
		final String[] parts = dottedPath.split("\\.");
		ConfigTree.Category category = tree;
		for (int i = 0; i < parts.length - 1; i++) {
			category = ConfigTree.find(category, parts[i]);
			if (category == null) {
				return fallback;
			}
		}
		final ConfigTree.Option option = ConfigTree.findOption(category, parts[parts.length - 1]);
		return option != null && option.get() instanceof Boolean value ? value : fallback;
	}

	/** The option at a dotted path, or null. */
	public ConfigTree.Option option(final String dottedPath) {
		final String[] parts = dottedPath.split("\\.");
		ConfigTree.Category category = tree;
		for (int i = 0; i < parts.length - 1; i++) {
			category = ConfigTree.find(category, parts[i]);
			if (category == null) {
				return null;
			}
		}
		return ConfigTree.findOption(category, parts[parts.length - 1]);
	}

	private void load() {
		if (!freshInstall) {
			try {
				final Map<String, Object> json = Json.parseObject(Files.readString(file, StandardCharsets.UTF_8));
				ConfigTree.fromJson(tree, json);
			} catch (final IOException | RuntimeException e) {
				// A corrupt config must not stop the game booting. Defaults are already in place; the
				// broken file is kept beside the new one so the user can rescue their settings.
				RetroAPI.LOGGER.error("Could not read {}, falling back to defaults", file, e);
				backupBrokenConfig();
			}
		}
		applySuppression();
		save();
	}

	/**
	 * Forces off every option another installed mod already provides, or that needs an optional API
	 * that is not installed, and remembers why - so the screen can explain the greyed-out row instead
	 * of looking broken.
	 */
	public void applySuppression() {
		ConfigTree.forEachOption(tree, option -> option.setSuppressed(ConfigTree.isStoodDownFor(option)));
	}

	public void save() {
		onSaved.run();
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, Json.write(ConfigTree.toJson(tree)), StandardCharsets.UTF_8);
		} catch (final IOException e) {
			RetroAPI.LOGGER.error("Could not write {}", file, e);
		}
	}

	private void backupBrokenConfig() {
		try {
			final Path backup = file.resolveSibling(id + ".json.broken");
			Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
			RetroAPI.LOGGER.error("Moved the unreadable config to {}", backup);
		} catch (final IOException e) {
			RetroAPI.LOGGER.error("Could not back up the unreadable config", e);
		}
	}
}
