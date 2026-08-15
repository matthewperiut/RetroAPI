package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/** {@code <x> <y> <z>} snapped to a block, the argument {@code /setblock}-style commands take. */
public class BlockPosArgumentType implements ArgumentType<PosArgument> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");

    private BlockPosArgumentType() {
    }

    public static BlockPosArgumentType blockPos() {
        return new BlockPosArgumentType();
    }

    public static Position getBlockPos(final CommandContext<RetroCommandSource> context, final String name) {
        return context.getArgument(name, PosArgument.class).toAbsoluteBlockPos(context.getSource());
    }

    @Override
    public PosArgument parse(final StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') {
            return LookingPosArgument.parse(reader);
        }
        return DefaultPosArgument.parse(reader, false);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return PositionSuggestions.suggest(builder, true, PositionSuggestions.lookedAtBlock(context));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
