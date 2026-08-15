package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A game mode, by name, by modern's shorthand ({@code c}, {@code sp}) or by the numeric id modern
 * still accepts. All four exist here because RetroAPI implements all four.
 */
public class GameModeArgumentType implements ArgumentType<RetroGameMode> {
    private static final Collection<String> EXAMPLES = Arrays.asList("survival", "creative", "spectator", "1");

    public static final DynamicCommandExceptionType UNKNOWN_GAME_MODE = new DynamicCommandExceptionType(
        mode -> Text.literal("Unknown game mode '" + mode + "'"));

    private GameModeArgumentType() {
    }

    public static GameModeArgumentType gameMode() {
        return new GameModeArgumentType();
    }

    public static RetroGameMode getGameMode(final CommandContext<?> context, final String name) {
        return context.getArgument(name, RetroGameMode.class);
    }

    @Override
    public RetroGameMode parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String value = reader.readUnquotedString();

        final RetroGameMode mode = RetroGameMode.byName(value);
        if (mode == null) {
            reader.setCursor(start);
            throw UNKNOWN_GAME_MODE.createWithContext(reader, value);
        }
        return mode;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        final List<String> names = new ArrayList<>();
        for (final RetroGameMode mode : RetroGameMode.values()) {
            names.add(mode.getName());
        }
        return SuggestionHelper.suggestMatching(names, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
