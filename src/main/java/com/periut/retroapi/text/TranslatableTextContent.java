package com.periut.retroapi.text;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * A translation key resolved at render time.
 *
 * <p>Resolution goes through {@link Translations}, which knows where a table can be found on each
 * side and falls back to the key itself when there is none. Arguments are flattened to plain strings
 * before substitution, so a styled argument keeps its text but not its colour; nothing in the mod passes
 * styled arguments, and doing it properly would mean a formatter that splits on every {@code %s}.
 */
public class TranslatableTextContent implements TextContent {
    private final String key;
    private final Object[] args;

    public TranslatableTextContent(final String key, final Object... args) {
        this.key = key;
        this.args = args;
    }

    public String getKey() {
        return key;
    }

    public Object[] getArgs() {
        return args;
    }

    public String resolve() {
        final String pattern = Translations.get(key, key);
        if (args.length == 0) {
            return pattern;
        }
        final Object[] flattened = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            flattened[i] = args[i] instanceof Text ? ((Text) args[i]).getString() : args[i];
        }
        try {
            return String.format(pattern, flattened);
        } catch (final IllegalArgumentException ex) {
            return pattern;
        }
    }

    @Override
    public <T> Optional<T> visit(final Text.StyledVisitor<T> visitor, final Style style) {
        return visitor.accept(style, resolve());
    }

    @Override
    public <T> Optional<T> visit(final Text.Visitor<T> visitor) {
        return visitor.accept(resolve());
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof TranslatableTextContent)) {
            return false;
        }
        final TranslatableTextContent that = (TranslatableTextContent) o;
        return Objects.equals(key, that.key) && Arrays.equals(args, that.args);
    }

    @Override
    public int hashCode() {
        return 31 * key.hashCode() + Arrays.hashCode(args);
    }

    @Override
    public String toString() {
        return "translatable{key=" + key + ", args=" + Arrays.toString(args) + "}";
    }
}
