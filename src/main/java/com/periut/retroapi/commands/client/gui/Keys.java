package com.periut.retroapi.commands.client.gui;

/**
 * LWJGL 2 key codes, as compile-time constants.
 *
 * <p>{@code org.lwjgl.input.Keyboard}'s own fields are not constant expressions in the Legacy Fabric
 * build beta runs against, so they cannot appear in a {@code switch}. These are the same values -
 * PS/2 scan codes, which is what beta's screens receive.
 */
public final class Keys {
    public static final int ESCAPE = 1;
    public static final int BACKSPACE = 14;
    public static final int TAB = 15;
    public static final int RETURN = 28;
    public static final int LCONTROL = 29;
    public static final int A = 30;
    public static final int LSHIFT = 42;
    public static final int X = 45;
    public static final int C = 46;
    public static final int V = 47;
    public static final int RSHIFT = 54;
    public static final int NUMPAD_ENTER = 156;
    public static final int RCONTROL = 157;
    public static final int HOME = 199;
    public static final int UP = 200;
    public static final int PAGE_UP = 201;
    public static final int LEFT = 203;
    public static final int RIGHT = 205;
    public static final int END = 207;
    public static final int DOWN = 208;
    public static final int PAGE_DOWN = 209;
    public static final int DELETE = 211;
    public static final int LMETA = 219;
    public static final int RMETA = 220;

    private Keys() {
    }

    public static boolean isShiftDown() {
        return org.lwjgl.input.Keyboard.isKeyDown(LSHIFT) || org.lwjgl.input.Keyboard.isKeyDown(RSHIFT);
    }

    /** Mac keyboards drive shortcuts from Command, not Control. */
    public static boolean isControlDown() {
        return org.lwjgl.input.Keyboard.isKeyDown(LCONTROL) || org.lwjgl.input.Keyboard.isKeyDown(RCONTROL)
            || org.lwjgl.input.Keyboard.isKeyDown(LMETA) || org.lwjgl.input.Keyboard.isKeyDown(RMETA);
    }
}
