package com.periut.retroapi.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

public class MutableText implements Text {
    private final TextContent content;
    private final List<Text> siblings;
    private Style style;

    public MutableText(final TextContent content, final Style style) {
        this(content, new ArrayList<>(), style);
    }

    public MutableText(final TextContent content, final List<Text> siblings, final Style style) {
        this.content = content;
        this.siblings = siblings;
        this.style = style;
    }

    @Override
    public TextContent getContent() {
        return content;
    }

    @Override
    public List<Text> getSiblings() {
        return siblings;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    public MutableText setStyle(final Style style) {
        this.style = style;
        return this;
    }

    public MutableText styled(final UnaryOperator<Style> operator) {
        this.style = operator.apply(this.style);
        return this;
    }

    public MutableText formatted(final Formatting... formattings) {
        for (final Formatting formatting : formattings) {
            style = style.withFormatting(formatting);
        }
        return this;
    }

    public MutableText withColor(final int rgb) {
        style = style.withColor(rgb);
        return this;
    }

    public MutableText append(final Text text) {
        siblings.add(text);
        return this;
    }

    public MutableText append(final String string) {
        return append(Text.literal(string));
    }

    @Override
    public MutableText copy() {
        return new MutableText(content, new ArrayList<>(siblings), style);
    }

    @Override
    public <T> Optional<T> visit(final StyledVisitor<T> visitor, final Style parentStyle) {
        final Style merged = style.withParent(parentStyle);
        final Optional<T> result = content.visit(visitor, merged);
        if (result.isPresent()) {
            return result;
        }
        for (final Text sibling : siblings) {
            final Optional<T> siblingResult = sibling.visit(visitor, merged);
            if (siblingResult.isPresent()) {
                return siblingResult;
            }
        }
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(final Visitor<T> visitor) {
        final Optional<T> result = content.visit(visitor);
        if (result.isPresent()) {
            return result;
        }
        for (final Text sibling : siblings) {
            final Optional<T> siblingResult = sibling.visit(visitor);
            if (siblingResult.isPresent()) {
                return siblingResult;
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MutableText)) {
            return false;
        }
        final MutableText that = (MutableText) o;
        return Objects.equals(content, that.content) && Objects.equals(style, that.style) && Objects.equals(siblings, that.siblings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, style, siblings);
    }

    @Override
    public String toString() {
        return "Text{" + content + ", style=" + style + ", siblings=" + siblings + "}";
    }
}
