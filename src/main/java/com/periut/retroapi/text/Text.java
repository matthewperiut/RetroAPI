package com.periut.retroapi.text;

import com.mojang.brigadier.Message;

import java.util.List;
import java.util.Optional;

/**
 * A chat component: content, a style, and siblings that inherit that style.
 *
 * <p>{@code Text} is also Brigadier's {@link Message}, exactly as in modern Minecraft, so a
 * command exception or a suggestion tooltip can carry formatting.
 */
public interface Text extends Message {
    Style getStyle();

    TextContent getContent();

    List<Text> getSiblings();

    MutableText copy();

    /** Walks this text and its siblings, stopping at the first visitor result. */
    <T> Optional<T> visit(StyledVisitor<T> visitor, Style style);

    <T> Optional<T> visit(Visitor<T> visitor);

    @Override
    default String getString() {
        final StringBuilder builder = new StringBuilder();
        visit(text -> {
            builder.append(text);
            return Optional.empty();
        });
        return builder.toString();
    }

    static MutableText empty() {
        return new MutableText(PlainTextContent.EMPTY, Style.EMPTY);
    }

    static MutableText literal(final String string) {
        return new MutableText(new PlainTextContent(string), Style.EMPTY);
    }

    static MutableText translatable(final String key, final Object... args) {
        return new MutableText(new TranslatableTextContent(key, args), Style.EMPTY);
    }

    /** Parses vanilla {@code §} codes into a styled text - how plain beta chat lines arrive. */
    static MutableText legacy(final String string) {
        return Texts.fromLegacy(string);
    }

    @FunctionalInterface
    interface Visitor<T> {
        Optional<T> accept(String text);
    }

    @FunctionalInterface
    interface StyledVisitor<T> {
        Optional<T> accept(Style style, String text);
    }
}
