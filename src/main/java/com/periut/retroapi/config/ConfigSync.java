package com.periut.retroapi.config;

import com.periut.retroapi.RetroAPI;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decides which options are in force while the player is on a server, and lets an operator change
 * them from inside the game.
 *
 * <p>Three situations, and the rule for each:
 *
 * <ul>
 *   <li><b>Singleplayer.</b> The local config is the truth. Nothing here runs.</li>
 *   <li><b>A server running this config.</b> The server sends its {@link Scope#WORLD} values and they
 *       take over for as long as the player is connected. {@link Scope#CLIENT} options are never sent
 *       and never overridden - how the player's own HUD or mouse behaves is their business.</li>
 *   <li><b>A vanilla server, or any server that does not answer.</b> Every {@code WORLD} option falls
 *       back to its default. This is the important case: a client that keeps its own block placement
 *       or drop rules against a server that has never heard of them will predict one thing, be
 *       corrected by the server, and look broken.</li>
 * </ul>
 *
 * <p>The user's own values are never written over - they are held in the option and simply not used
 * while a server is in charge - so leaving the server restores everything exactly, and the config
 * file on disk is never touched by what server someone visited.
 *
 * <h2>Editing a server's config</h2>
 *
 * <p>A {@code WORLD} option shown to a player on a server is the SERVER's setting, so an operator
 * editing it is editing the server. That is exactly what happens: the server tells each player on
 * join whether they may ({@link #canEditServerConfig()}), the screen marks every such row with a red
 * asterisk so nobody changes the whole server's rules thinking they changed their own, and an edit is
 * sent back on the same channel. The server re-checks that the sender is an operator before applying
 * anything - the flag is for the screen, never for the decision - then saves and broadcasts the new
 * value to everyone connected.
 */
public final class ConfigSync {

	private ConfigSync() {
	}

	private static boolean serverControlActive;
	private static boolean serverEditable;

	/** True while a server's rules are in force rather than the player's own. */
	public static boolean isServerControlled() {
		return serverControlActive;
	}

	/**
	 * True while this player may edit the connected server's world options - i.e. the server said they
	 * are an operator. Always false in singleplayer, where there is no server to edit and the local
	 * config is simply the config.
	 */
	public static boolean canEditServerConfig() {
		return serverControlActive && serverEditable;
	}

	/**
	 * Whether editing this option changes the connected server rather than this client. True for a
	 * {@link Scope#WORLD} option while a server is in charge - which is what the screen's red asterisk
	 * means.
	 */
	public static boolean isServerScoped(final ConfigTree.Option option) {
		return serverControlActive && option.scope == Scope.WORLD;
	}

	/**
	 * Applies a server's values. Anything the server did not mention falls back to its default, so a
	 * partial or older payload cannot leave a stray tweak running.
	 */
	public static void applyServerConfig(final Map<String, Object> values, final boolean editable) {
		serverControlActive = true;
		serverEditable = editable;
		int applied = 0;
		for (final Map.Entry<String, ConfigTree.Option> entry : worldOptions().entrySet()) {
			final Object value = values.get(entry.getKey());
			final ConfigTree.Option option = entry.getValue();
			if (value == null) {
				option.takeOverByServer(vanillaValue(option));
			} else {
				option.takeOverByServer(value);
				applied++;
			}
		}
		RetroAPI.LOGGER.info("Server sent {} world config option(s){}", applied,
			editable ? "; this player may edit them" : "");
	}

	/** Forces every world-affecting option to its default, for a server that does not run these configs. */
	public static void applyVanillaServer() {
		serverControlActive = true;
		serverEditable = false;
		for (final ConfigTree.Option option : worldOptions().values()) {
			option.takeOverByServer(vanillaValue(option));
		}
		RetroAPI.LOGGER.info("Server is not running RetroAPI configs; world options are off for this session. "
			+ "Interface, HUD, inventory and sound options are unaffected.");
	}

	/** Hands every option back to the player's own value. Called on disconnect. */
	public static void restoreLocal() {
		if (!serverControlActive) {
			return;
		}
		serverControlActive = false;
		serverEditable = false;
		for (final RetroConfig config : RetroConfigs.all()) {
			ConfigTree.forEachOption(config.tree(), ConfigTree.Option::releaseToLocal);
		}
		RetroAPI.LOGGER.info("Left the server; local settings restored.");
	}

	/** Every world option in every registered config, keyed as {@code <modId>:<dotted.path>}. */
	public static Map<String, ConfigTree.Option> worldOptions() {
		final Map<String, ConfigTree.Option> options = new LinkedHashMap<>();
		for (final RetroConfig config : RetroConfigs.all()) {
			collect(config.id(), config.tree(), "", options);
		}
		return options;
	}

	/** This side's world options, as the values a server would send. */
	public static Map<String, Object> collectWorldConfig() {
		final Map<String, Object> values = new LinkedHashMap<>();
		worldOptions().forEach((key, option) -> values.put(key, option.get()));
		return values;
	}

	/**
	 * Writes one value straight into the option a key names, for a payload arriving over the wire.
	 * Server-side this IS the setting; client-side it is the server's value taking over.
	 *
	 * @return the option that was written, or null when nothing answers to that key - a mod the other
	 *         side has and this one does not, which is a normal thing to be sent and a normal thing to
	 *         ignore
	 */
	public static ConfigTree.Option writeWorldValue(final String key, final String encoded, final boolean asServer) {
		final ConfigTree.Option option = worldOptions().get(key);
		if (option == null) {
			return null;
		}
		final Object value;
		try {
			value = ConfigTree.decodeString(option, encoded);
		} catch (final RuntimeException e) {
			RetroAPI.LOGGER.warn("Ignoring unreadable value for config option '{}': {}", key, e.getMessage());
			return null;
		}
		if (asServer) {
			option.set(value);
		} else {
			option.takeOverByServer(value);
		}
		return option;
	}

	/** The config a key belongs to, or null. Keys are {@code <modId>:<dotted.path>}. */
	public static RetroConfig configOf(final String key) {
		final int colon = key.indexOf(':');
		return colon < 0 ? null : RetroConfigs.get(key.substring(0, colon));
	}

	/** The wire key for an option inside {@code config}. */
	public static String keyOf(final RetroConfig config, final ConfigTree.Option option) {
		return config.id() + ":" + option.path;
	}

	/**
	 * What the option has to read as for the client to behave like the server does with no config at
	 * all. For a toggle that is "off"; for the numbers and choices that are only parameters of a
	 * toggle, the value does not matter and the default is the least surprising thing to show.
	 */
	private static Object vanillaValue(final ConfigTree.Option option) {
		return option.kind == ConfigTree.Kind.BOOLEAN ? Boolean.FALSE : option.defaultValue;
	}

	private static void collect(final String modId, final ConfigTree node, final String prefix,
			final Map<String, ConfigTree.Option> out) {
		if (node instanceof ConfigTree.Option option) {
			if (option.scope == Scope.WORLD) {
				out.put(modId + ":" + prefix, option);
			}
			return;
		}
		for (final ConfigTree child : ((ConfigTree.Category) node).children) {
			collect(modId, child, prefix.isEmpty() ? child.key : prefix + "." + child.key, out);
		}
	}
}
