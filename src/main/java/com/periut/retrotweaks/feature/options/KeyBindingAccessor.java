package com.periut.retrotweaks.feature.options;

/**
 * Implemented by {@code KeyBinding} through a mixin, so the key code it was constructed with is
 * still reachable after the player rebinds it. From MojangFix, backs the "Reset to default" button
 * on each row of {@link com.periut.retrotweaks.client.gui.ControlsScreen}.
 */
public interface KeyBindingAccessor {

	int retrotweaks$getDefaultKeyCode();
}
