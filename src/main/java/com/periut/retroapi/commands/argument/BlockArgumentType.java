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
import com.periut.retroapi.text.Text;
import net.minecraft.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code <block>} - modern's {@code BlockStateArgument} as far as beta can carry it: a name
 * ({@code stone}), a namespaced id ({@code minecraft:wool}, {@code somemod:thing}) or a raw id
 * ({@code 35}), each optionally followed by {@code :meta}.
 *
 * <p>Modern's block <em>states</em> ({@code minecraft:furnace[facing=north]}) have no beta
 * equivalent - the metadata nibble is the whole of it - so the subtype is that nibble, spelled the
 * way beta spells everything else.
 *
 * <p>Only ids that name a real block are accepted, which is what stops {@code /setblock ~ ~ ~ apple}
 * from putting an item id in the world.
 */
public class BlockArgumentType implements ArgumentType<BlockArgumentType.BlockInput> {
    private static final Collection<String> EXAMPLES = Arrays.asList("stone", "minecraft:wool:14", "35:14");

    public static final DynamicCommandExceptionType UNKNOWN_BLOCK = new DynamicCommandExceptionType(
        block -> Text.literal("Unknown block '" + block + "'"));

    /** A block and the metadata to place it with. */
    public record BlockInput(int blockId, int meta) {
    }

    private BlockArgumentType() {
    }

    public static BlockArgumentType block() {
        return new BlockArgumentType();
    }

    public static BlockInput getBlock(final CommandContext<RetroCommandSource> context, final String name) {
        return context.getArgument(name, BlockInput.class);
    }

    @Override
    public BlockInput parse(final StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        final StringBuilder token = new StringBuilder();
        while (reader.canRead() && (StringReader.isAllowedInUnquotedString(reader.peek()) || reader.peek() == ':')) {
            token.append(reader.read());
        }
        final String text = token.toString();

        VanillaIds.VanillaItem resolved = ItemIds.resolve(text);
        if (resolved == null) {
            // A trailing ":<meta>" is only a subtype when what precedes it names a real block.
            final int lastSeparator = text.lastIndexOf(':');
            if (lastSeparator > 0) {
                final VanillaIds.VanillaItem head = ItemIds.resolve(text.substring(0, lastSeparator));
                if (head != null) {
                    try {
                        resolved = new VanillaIds.VanillaItem(head.id(), Integer.parseInt(text.substring(lastSeparator + 1)));
                    } catch (final NumberFormatException ignored) {
                        // Leave it unresolved so the error names the whole token.
                    }
                }
            }
        }

        if (resolved == null || !isBlock(resolved.id())) {
            reader.setCursor(start);
            throw UNKNOWN_BLOCK.createWithContext(reader, text);
        }
        return new BlockInput(resolved.id(), resolved.meta());
    }

    private static boolean isBlock(final int id) {
        return id > 0 && id < Block.BLOCKS.length && Block.BLOCKS[id] != null;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        final List<String> blocks = new ArrayList<>();
        for (final String identifier : ItemIds.allIdentifiers()) {
            final VanillaIds.VanillaItem resolved = ItemIds.resolve(identifier);
            if (resolved != null && isBlock(resolved.id())) {
                blocks.add(identifier);
            }
        }
        return SuggestionHelper.suggestIdentifiers(blocks, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
