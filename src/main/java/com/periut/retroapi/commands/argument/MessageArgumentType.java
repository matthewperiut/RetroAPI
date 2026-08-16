package com.periut.retroapi.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.selector.EntitySelector;
import com.periut.retroapi.commands.selector.EntitySelectorReader;
import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * The rest of the line, with any {@code @a}-style selectors inside it resolved to names when the
 * message is finally built - so {@code /say hello @a} greets everyone by name, as in modern.
 */
public class MessageArgumentType implements ArgumentType<MessageArgumentType.MessageFormat> {
    private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");

    private MessageArgumentType() {
    }

    public static MessageArgumentType message() {
        return new MessageArgumentType();
    }

    public static String getMessage(final CommandContext<RetroCommandSource> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, MessageFormat.class).format(context.getSource());
    }

    @Override
    public MessageFormat parse(final StringReader reader) throws CommandSyntaxException {
        return MessageFormat.parse(reader);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    /** The raw text plus the location of every selector found in it. */
    public record MessageFormat(String contents, List<Part> parts) {
        public String format(final RetroCommandSource source) throws CommandSyntaxException {
            if (parts.isEmpty()) {
                return contents;
            }

            final StringBuilder result = new StringBuilder();
            int cursor = 0;
            for (final Part part : parts) {
                result.append(contents, cursor, part.start());
                result.append(part.resolve(source));
                cursor = part.end();
            }
            result.append(contents.substring(cursor));
            return result.toString();
        }

        static MessageFormat parse(final StringReader reader) throws CommandSyntaxException {
            final int base = reader.getCursor();
            final String contents = reader.getRemaining();
            reader.setCursor(reader.getTotalLength());

            final List<Part> parts = new ArrayList<>();
            for (int i = 0; i < contents.length(); i++) {
                if (contents.charAt(i) != EntitySelectorReader.SELECTOR_PREFIX) {
                    continue;
                }

                final StringReader selectorReader = new StringReader(contents);
                selectorReader.setCursor(i);
                try {
                    final EntitySelector selector = new EntitySelectorReader(selectorReader, true).read();
                    parts.add(new Part(i, selectorReader.getCursor(), selector));
                    i = selectorReader.getCursor() - 1;
                } catch (final CommandSyntaxException ignored) {
                    // A bare '@' in prose is not an error - leave it as text.
                }
            }

            reader.setCursor(base + contents.length());
            return new MessageFormat(contents, parts);
        }
    }

    public record Part(int start, int end, EntitySelector selector) {
        String resolve(final RetroCommandSource source) throws CommandSyntaxException {
            final List<? extends Entity> entities = selector.getEntities(source);
            final StringBuilder names = new StringBuilder();
            for (final Entity entity : entities) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(EntityNames.displayName(entity));
            }
            return names.toString();
        }
    }
}
