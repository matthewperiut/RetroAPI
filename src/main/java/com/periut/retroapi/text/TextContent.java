package com.periut.retroapi.text;

import java.util.Optional;

/** What a text node says, separately from how it looks. */
public interface TextContent {
    <T> Optional<T> visit(Text.StyledVisitor<T> visitor, Style style);

    <T> Optional<T> visit(Text.Visitor<T> visitor);
}
