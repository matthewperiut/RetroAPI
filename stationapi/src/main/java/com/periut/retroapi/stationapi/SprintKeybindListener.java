package com.periut.retroapi.stationapi;

import net.mine_diver.unsafeevents.listener.EventListener;
import net.minecraft.client.option.KeyBinding;
import net.modificationstation.stationapi.api.client.event.option.KeyBindingRegisterEvent;
import org.lwjgl.input.Keyboard;

/**
 * Registers RetroAPI's sprint key as a real, rebindable binding.
 *
 * <p>Beta's controls screen only lists the keys beta itself has, so without StationAPI the sprint key
 * is a fixed default nobody can change. StationAPI does have a registry for this, and the key it ends
 * up bound to is read back through {@code StationBridge.sprintKeyCode()}.
 */
public final class SprintKeybindListener {
    private static KeyBinding sprintKeybind;

    @EventListener
    public void registerKeybinds(final KeyBindingRegisterEvent event) {
        sprintKeybind = new KeyBinding("key.retroapi.sprint", Keyboard.KEY_LCONTROL);
        event.keyBindings.add(sprintKeybind);
    }

    /** @return the bound key, or -1 before the binding exists */
    static int keyCode() {
        return sprintKeybind == null ? -1 : sprintKeybind.code;
    }
}
