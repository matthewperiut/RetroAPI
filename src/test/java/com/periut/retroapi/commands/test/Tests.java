package com.periut.retroapi.commands.test;

import java.util.ArrayList;
import java.util.List;

/**
 * A three-method test framework.
 *
 * <p>Loom cannot resolve JUnit here (b1.7.3 development is offline more often than not), and these
 * tests exist to be runnable from a plain {@code java} invocation against nothing but the mod's own
 * classes - see {@link TestRunner}. Nothing in here may touch a Minecraft class.
 */
public final class Tests {
    private static final List<String> FAILURES = new ArrayList<>();
    private static int checks;
    private static String group = "";

    private Tests() {
    }

    public static void group(final String name) {
        group = name;
    }

    public static void check(final String name, final boolean condition) {
        checks++;
        if (!condition) {
            FAILURES.add(group + " / " + name);
        }
    }

    public static void eq(final String name, final Object expected, final Object actual) {
        checks++;
        if (!java.util.Objects.equals(expected, actual)) {
            FAILURES.add(group + " / " + name + "\n    expected: " + expected + "\n    actual:   " + actual);
        }
    }

    public static void throwsError(final String name, final Class<? extends Throwable> type, final ThrowingRunnable runnable) {
        checks++;
        try {
            runnable.run();
            FAILURES.add(group + " / " + name + "\n    expected " + type.getSimpleName() + ", nothing was thrown");
        } catch (final Throwable thrown) {
            if (!type.isInstance(thrown)) {
                FAILURES.add(group + " / " + name + "\n    expected " + type.getSimpleName() + ", got " + thrown);
            }
        }
    }

    /** Prints the report and returns the process exit code. */
    public static int report() {
        if (FAILURES.isEmpty()) {
            System.out.println("all " + checks + " checks passed");
            return 0;
        }
        System.out.println(FAILURES.size() + " of " + checks + " checks failed:");
        for (final String failure : FAILURES) {
            System.out.println("  " + failure);
        }
        return 1;
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
