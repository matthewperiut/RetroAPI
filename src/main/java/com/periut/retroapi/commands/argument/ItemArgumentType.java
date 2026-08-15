package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.SuggestionHelper;
import com.periut.retroapi.register.item.ObtainableItems;
import com.periut.retroapi.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * {@code <item>} - a name ({@code stone}), a namespaced id ({@code minecraft:stone},
 * {@code somemod:thing}) or a raw beta id ({@code 1}), each optionally followed by {@code :meta}
 * for a subtype such as a wool colour.
 */
public class ItemArgumentType implements ArgumentType<ItemStackArgument> {
    /** Distinct from "unknown": the name is real, it just is not something anyone can hold. */
    public static final DynamicCommandExceptionType NOT_OBTAINABLE = new DynamicCommandExceptionType(
        item -> Text.literal("'" + item + "' is a block state rather than an item"));

    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:wool:14", "35:14", "1");

    private ItemArgumentType() {
    }

    public static ItemArgumentType item() {
        return new ItemArgumentType();
    }

    public static ItemStackArgument getItem(final CommandContext<RetroCommandSource> context, final String name) {
        return context.getArgument(name, ItemStackArgument.class);
    }

    @Override
    public ItemStackArgument parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final String token = readToken(reader);

        VanillaIds.VanillaItem resolved = ItemIds.resolve(token);

        // A trailing ":<meta>" is only a subtype when what precedes it names a real item; that keeps
        // "minecraft:wool" from being read as the item "minecraft" with the subtype "wool".
        if (resolved == null) {
            final int lastSeparator = token.lastIndexOf(':');
            if (lastSeparator > 0) {
                final VanillaIds.VanillaItem head = ItemIds.resolve(token.substring(0, lastSeparator));
                if (head != null) {
                    try {
                        resolved = new VanillaIds.VanillaItem(head.id(), Integer.parseInt(token.substring(lastSeparator + 1)));
                    } catch (final NumberFormatException ignored) {
                        // Leave it unresolved so the error below names the whole token.
                    }
                }
            }
        }

        if (resolved == null) {
            reader.setCursor(start);
            throw ItemStackArgument.UNKNOWN_ITEM.createWithContext(reader, token);
        }

        // Leaving these out of the suggestions was only half the job: a name still resolved, so
        // /give minecraft:redstone_torch_off handed over a block that has no item form at all. An
        // argument that will not offer something should not accept it typed out either.
        if (!ObtainableItems.isObtainable(resolved.id())) {
            reader.setCursor(start);
            throw NOT_OBTAINABLE.createWithContext(reader, token);
        }

        return new ItemStackArgument(resolved.id(), resolved.meta());
    }

    private static String readToken(final StringReader reader) {
        final int start = reader.getCursor();
        while (reader.canRead() && (StringReader.isAllowedInUnquotedString(reader.peek()) || reader.peek() == ':')) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        // Only what can actually be given: a block that exists solely as a world state has no item
        // form, and offering it here offers something the command cannot hand over.
        return SuggestionHelper.suggestIdentifiers(ItemIds.obtainableIdentifiers(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
