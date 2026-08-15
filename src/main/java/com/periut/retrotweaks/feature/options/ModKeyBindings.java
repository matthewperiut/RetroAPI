package com.periut.retrotweaks.feature.options;

import com.periut.retrotweaks.compat.Mods;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;

import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * RetroTweaks' own client keybindings. From GameplayEssentials.
 *
 * <p>Held here rather than inline in a mixin so both {@code KeyBindingRegistryMixin} (which registers it into
 * {@code GameOptions.allKeys}) and the client interaction mixin that reads it see the same instance.
 */
@Environment(EnvType.CLIENT)
public final class ModKeyBindings {

	private ModKeyBindings() {}

	/**
	 * Dedicated key for "Disable Block Interactions With Keybind", independent of the sneak key.
	 * Defaults to Left Shift.
	 */
	public static final KeyBinding DISABLE_BLOCK_INTERACTIONS = new KeyBinding("No Interactions", Keyboard.KEY_LSHIFT);

	/** Hold to zoom the camera in. From UniTweaks' FOV slider feature. Defaults to C. */
	public static final KeyBinding ZOOM = new KeyBinding("Zoom", Keyboard.KEY_C);

	/**
	 * Toggles the debug profiler graph on its own when "Debug Graph Needs Shift+F3" is off.
	 * From MojangFix. Defaults to Left Control.
	 */
	public static final KeyBinding DEBUG_GRAPH = new KeyBinding("Debug Graph", Keyboard.KEY_LCONTROL);

	/**
	 * The keys this install should actually put in {@code GameOptions.allKeys}, in order.
	 *
	 * <p>{@link #ZOOM} exists only to drive the FOV-slider feature, which is
	 * {@code @Opt(source = Mods.UNITWEAKS)} and therefore already stands down wholesale when UniTweaks
	 * is installed - {@code ModOptions.enabled(fovOption)} answers false, and
	 * {@code GameRendererFovMixin} never applies a zoom no matter what the key does. Registering it
	 * anyway put a second, permanently inert "Zoom" in the controls list beside UniTweaks' working one.
	 * So it is left out entirely there.
	 *
	 * <p>Keyed on whether UniTweaks is INSTALLED rather than on the config flag it forces off: mod
	 * presence cannot change mid-session, whereas the flag can, and a key list is built once when
	 * {@code GameOptions} is constructed. Gating on the flag would mean turning FOV back on could not
	 * give the key back without a restart, for an option that is not marked as needing one.
	 *
	 * <p>The other two are RetroTweaks' own - "No Interactions" comes from GameplayEssentials and
	 * "Debug Graph" from MojangFix, neither of which UniTweaks provides an equivalent for - so they
	 * stay registered either way.
	 */
	public static List<KeyBinding> registered() {
		List<KeyBinding> keys = new ArrayList<>(3);
		keys.add(DISABLE_BLOCK_INTERACTIONS);
		if (!Mods.HAS_UNITWEAKS) keys.add(ZOOM);
		keys.add(DEBUG_GRAPH);
		return keys;
	}
}
