package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** A duration in ticks, written as {@code 100}, {@code 100t}, {@code 5s} or {@code 2d}. */
public class TimeArgumentType implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");

    public static final DynamicCommandExceptionType INVALID_UNIT = new DynamicCommandExceptionType(
        unit -> Text.literal("Invalid unit '" + unit + "'"));
    public static final DynamicCommandExceptionType TICK_COUNT_TOO_LOW = new DynamicCommandExceptionType(
        min -> Text.literal("Tick count must not be less than " + min));

    private final int minimum;

    private TimeArgumentType(final int minimum) {
        this.minimum = minimum;
    }

    public static TimeArgumentType time() {
        return new TimeArgumentType(0);
    }

    public static TimeArgumentType time(final int minimum) {
        return new TimeArgumentType(minimum);
    }

    public static int getTime(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Integer.class);
    }

    @Override
    public Integer parse(final StringReader reader) throws CommandSyntaxException {
        final float value = reader.readFloat();
        final String unit = reader.readUnquotedString();

        final int multiplier = switch (unit) {
            case "", "t" -> 1;
            case "s" -> 20;
            case "d" -> 24000;
            default -> throw INVALID_UNIT.createWithContext(reader, unit);
        };

        final int ticks = Math.round(value * multiplier);
        if (ticks < minimum) {
            throw TICK_COUNT_TOO_LOW.createWithContext(reader, minimum);
        }
        return ticks;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        final String remaining = builder.getRemaining();
        // Suffixes are only worth offering once there is a number for them to qualify.
        if (!remaining.isEmpty() && Character.isDigit(remaining.charAt(remaining.length() - 1))) {
            builder.suggest(remaining + "t");
            builder.suggest(remaining + "s");
            builder.suggest(remaining + "d");
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
