package com.periut.retroapi.commands.test;

/**
 * Entry point for the offline test suite: {@code java -cp <classes> com.periut.retroapi.commands.test.TestRunner}.
 *
 * <p>Every suite registered here must run without a Minecraft classpath.
 */
public final class TestRunner {
    public static void main(final String[] args) {
        BrigadierTest.run();
        TextTest.run();
        CommandTest.run();
        ClipboardTest.run();
        System.exit(Tests.report());
    }
}
