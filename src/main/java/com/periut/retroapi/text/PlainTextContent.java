package com.periut.retroapi.text;

import java.util.Objects;
import java.util.Optional;

public class PlainTextContent implements TextContent {
    public static final PlainTextContent EMPTY = new PlainTextContent("");

    private final String string;

    public PlainTextContent(final String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }

    @Override
    public <T> Optional<T> visit(final Text.StyledVisitor<T> visitor, final Style style) {
        return string.isEmpty() ? Optional.empty() : visitor.accept(style, string);
    }

    @Override
    public <T> Optional<T> visit(final Text.Visitor<T> visitor) {
        return string.isEmpty() ? Optional.empty() : visitor.accept(string);
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof PlainTextContent && Objects.equals(string, ((PlainTextContent) o).string);
    }

    @Override
    public int hashCode() {
        return string.hashCode();
    }

    @Override
    public String toString() {
        return "literal{" + string + "}";
    }
}
