package com.periut.retroapi.commands.test;

import java.lang.reflect.Field;

/**
 * Guards the {@code Display.setClipboard} lookup in {@code Clipboard$Display}.
 *
 * <p>The lookup is by name, and when it misses it misses silently: writes fall back to AWT, which
 * puts the text where the read side - retrodragon's SDL-backed {@code Screen.getClipboard()} -
 * never looks, and copying out of chat quietly does nothing. So the failure this catches is the one
 * with no symptom.
 *
 * <p>{@code Clipboard$Display} is its own class file and names no Minecraft type, so loading it
 * here does not drag {@code Screen} in with it - the suite still runs without a Minecraft classpath.
 */
public final class ClipboardTest {
    private static final String DISPLAY = "org.lwjgl.opengl.Display";
    private static final String HOLDER = "com.periut.retroapi.commands.client.gui.Clipboard$Display";

    private ClipboardTest() {
    }

    public static void run() {
        Tests.group("clipboard");

        if (!hasSetter()) {
            // A plain LWJGL 2 classpath: the setter genuinely is not there and AWT is correct.
            Tests.check("no LWJGL 3 Display, nothing to verify", true);
            return;
        }

        try {
            Tests.check("Display.setClipboard resolved", field(Class.forName(HOLDER), "SET") != null);
        } catch (final ReflectiveOperationException thrown) {
            Tests.check("Clipboard$Display is loadable without Minecraft: " + thrown, false);
        }
    }

    private static Object field(final Class<?> owner, final String name) throws ReflectiveOperationException {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    /** Whether this classpath has a writable clipboard at all, checked independently of the mod. */
    private static boolean hasSetter() {
        try {
            Class.forName(DISPLAY).getMethod("setClipboard", String.class);
            return true;
        } catch (final ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
