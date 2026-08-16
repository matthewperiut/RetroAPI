package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.NbtCompound;

import java.util.Arrays;
import java.util.Collection;

/**
 * {@code <nbt>} - a modern {@code {Key:value}} compound, as {@code /summon}'s third argument.
 *
 * <p>The parsing is {@link Snbt}; suggestions are not here, because which keys are worth offering
 * depends on the entity named earlier in the command. {@code SummonCommand} supplies them.
 */
public class NbtCompoundArgumentType implements ArgumentType<NbtCompound> {
    private static final Collection<String> EXAMPLES = Arrays.asList("{Color:14}", "{powered:1b}", "{Fuse:10b}");

    private NbtCompoundArgumentType() {
    }

    public static NbtCompoundArgumentType nbtCompound() {
        return new NbtCompoundArgumentType();
    }

    public static NbtCompound getNbtCompound(final CommandContext<?> context, final String name) {
        return context.getArgument(name, NbtCompound.class);
    }

    @Override
    public NbtCompound parse(final StringReader reader) throws CommandSyntaxException {
        return Snbt.parseCompound(reader);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
