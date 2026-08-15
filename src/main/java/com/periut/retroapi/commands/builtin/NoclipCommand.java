package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;

/**
 * {@code /noclip [speed]} - free flight through blocks, with the scroll wheel as a throttle.
 *
 * <p>Also the state the movement, scroll and server-correction mixins read, keyed by player name so
 * that reconnecting keeps a player's setting.
 */
public final class NoclipCommand {
    public static final double MIN_SPEED = 0.1;
    public static final double MAX_SPEED = 10.0;
    public static final double SPEED_STEP = 1.25;
    public static final double BASE_SPEED = 0.4;

    private static final Set<String> ACTIVE = new HashSet<>();
    private static final Map<String, Double> SPEEDS = new HashMap<>();

    private NoclipCommand() {
    }

    public static boolean isActive(final String playerName) {
        return playerName != null && ACTIVE.contains(playerName);
    }

    public static double speed(final String playerName) {
        final Double value = SPEEDS.get(playerName);
        return value == null ? 1.0 : value;
    }

    /** Back to 1x. Called when a player leaves the modes that have a throttle at all. */
    public static void resetSpeed(final String playerName) {
        SPEEDS.remove(playerName);
    }

    /** @return the new speed, or -1 when it was already at the end of its range */
    public static double changeSpeed(final String playerName, final double factor) {
        final double current = speed(playerName);
        final double next = Math.max(MIN_SPEED, Math.min(MAX_SPEED, current * factor));
        if (Math.abs(next - current) < 1.0e-9) {
            return -1.0;
        }
        SPEEDS.put(playerName, next);
        return next;
    }

    /** Two decimals at the slow end, none at the fast end, so the readout never reads "2.50x". */
    public static String format(final double speed) {
        if (speed >= 10.0 || Math.abs(speed - Math.rint(speed)) < 0.005) {
            return String.valueOf((int) Math.rint(speed));
        }
        return String.valueOf(Math.rint(speed * 100.0) / 100.0);
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("noclip")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(context -> toggle(context.getSource()))
            .then(argument("speed", doubleArg(MIN_SPEED, MAX_SPEED))
                .executes(context -> setSpeed(context.getSource(), getDouble(context, "speed")))));
    }

    private static int toggle(final RetroCommandSource source) throws CommandSyntaxException {
        final PlayerEntity player = source.getPlayerOrThrow();
        final boolean on = !ACTIVE.contains(player.name);

        if (on) {
            ACTIVE.add(player.name);
        } else {
            ACTIVE.remove(player.name);
            // Hand the player back to normal physics rather than leaving them intangible and hanging.
            player.noClip = false;
            player.velocityX = 0.0;
            player.velocityY = 0.0;
            player.velocityZ = 0.0;
        }

        source.sendFeedback(Text.literal("Noclip " + (on ? "on" : "off")
            + (on ? " - scroll to change speed (" + format(speed(player.name)) + "x)" : "")));
        return Command.SINGLE_SUCCESS;
    }

    /** Setting a speed deliberately does not toggle, so you can slow down without landing. */
    private static int setSpeed(final RetroCommandSource source, final double speed) throws CommandSyntaxException {
        final PlayerEntity player = source.getPlayerOrThrow();
        SPEEDS.put(player.name, speed);
        source.sendFeedback(Text.literal("Noclip speed " + format(speed) + "x"));
        return Command.SINGLE_SUCCESS;
    }
}
