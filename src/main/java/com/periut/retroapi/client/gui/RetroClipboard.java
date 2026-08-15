package com.periut.retroapi.client.gui;

import net.minecraft.client.gui.screen.Screen;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Method;

/**
 * RetroClipboard access.
 *
 * <p>Beta can only read: {@link Screen#getClipboard()} is the one clipboard call it has, so copying
 * has to find its own way out. Which way that should be depends on what is driving the window.
 *
 * <p>Under retrodragon the window is SDL, and retrodragon redirects {@code Screen.getClipboard()} at
 * its own {@code Display.getClipboard()}, so reads already come from the compositor.
 * {@code Display.setClipboard()} is the matching write, and going through it is what keeps copy and
 * paste on one backend - AWT would put the text somewhere the read side never looks.
 *
 * <p>On a plain LWJGL 2 install that method does not exist and AWT is all there is. That is also
 * where the two can disagree: AWT is an XWayland client, so it cannot see a selection owned by a
 * Wayland-native application, though it can always read back whatever it wrote itself - copy
 * appears to work while pasting from a browser or a terminal silently yields nothing.
 *
 * <p>Every call is guarded: a headless JVM, or a desktop that refuses clipboard access, must cost
 * the player a copy rather than the chat screen.
 */
public final class RetroClipboard {
    private RetroClipboard() {
    }

    public static String read() {
        try {
            final String contents = Screen.getClipboard();
            return contents == null ? "" : contents;
        } catch (final RuntimeException | LinkageError ignored) {
            return "";
        }
    }

    public static void write(final String contents) {
        if (contents == null || contents.isEmpty()) {
            return;
        }

        if (Display.write(contents)) {
            return;
        }

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(contents), null);
        } catch (final RuntimeException | LinkageError ignored) {
            // Nothing to be done about it, and nothing worth interrupting the player over.
        }
    }

    /**
     * {@code org.lwjgl.opengl.Display.setClipboard}, by reflection.
     *
     * <p>The class is on the compile classpath, but as LWJGL 2, whose clipboard is read-only - the
     * setter exists only on the replacement LWJGL 3 stack, so it cannot be named directly. Looked up
     * once, on first use.
     */
    private static final class Display {
        private static final Method SET;

        static {
            Method set = null;
            try {
                set = Class.forName("org.lwjgl.opengl.Display").getMethod("setClipboard", String.class);
            } catch (final ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // A plain LWJGL 2 install, where AWT is the only way to write.
            }
            SET = set;
        }

        private Display() {
        }

        /** @return whether the clipboard actually took it */
        static boolean write(final String contents) {
            if (SET == null) {
                return false;
            }
            try {
                SET.invoke(null, contents);
                return true;
            } catch (final ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return false;
            }
        }
    }
}
