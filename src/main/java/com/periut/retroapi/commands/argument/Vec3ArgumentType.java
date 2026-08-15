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

/** {@code <x> <y> <z>}, accepting {@code ~} offsets and {@code ^} local coordinates. */
public class Vec3ArgumentType implements ArgumentType<PosArgument> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "0.1 -0.5 .9", "~0.5 ~1 ~-5");

    private final boolean centerIntegers;

    private Vec3ArgumentType(final boolean centerIntegers) {
        this.centerIntegers = centerIntegers;
    }

    public static Vec3ArgumentType vec3() {
        return new Vec3ArgumentType(true);
    }

    public static Vec3ArgumentType vec3(final boolean centerIntegers) {
        return new Vec3ArgumentType(centerIntegers);
    }

    public boolean centersIntegers() {
        return centerIntegers;
    }

    public static Position getVec3(final CommandContext<RetroCommandSource> context, final String name) {
        return context.getArgument(name, PosArgument.class).toAbsolutePos(context.getSource());
    }

    public static PosArgument getPosArgument(final CommandContext<RetroCommandSource> context, final String name) {
        return context.getArgument(name, PosArgument.class);
    }

    @Override
    public PosArgument parse(final StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '^') {
            return LookingPosArgument.parse(reader);
        }
        return DefaultPosArgument.parse(reader, centerIntegers);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return PositionSuggestions.suggest(builder, false, PositionSuggestions.lookedAtBlock(context));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
