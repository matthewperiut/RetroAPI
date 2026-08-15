package com.periut.retroapi.text;

import com.mojang.brigadier.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Conversions between {@link Text} and the flat, {@code §}-coded strings beta deals in. */
public final class Texts {
    private Texts() {
    }

    /** One entry per run of characters that share a style, in render order. */
    public record Segment(String text, Style style) {
    }

    public static List<Segment> flatten(final Text text) {
        final List<Segment> segments = new ArrayList<>();
        text.visit((style, string) -> {
            if (!string.isEmpty()) {
                segments.add(new Segment(string, style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return segments;
    }

    /** Brigadier hands back {@link Message}s; ours are already {@link Text}, others become literals. */
    public static Text of(final Message message) {
        if (message instanceof Text) {
            return (Text) message;
        }
        return Text.literal(message == null ? "" : message.getString());
    }

    /**
     * Flattens to a {@code §}-coded string for anything that cannot render a component: the server
     * console, log files, and vanilla clients on a multiplayer server.
     *
     * <p>Only colours survive. Beta's font renderer has no code for bold or italics and prints an
     * unrecognised {@code §x} pair as-is, so emitting those would leave visible junk in vanilla chat.
     */
    public static String toLegacy(final Text text) {
        final StringBuilder builder = new StringBuilder();
        Formatting current = null;

        for (final Segment segment : flatten(text)) {
            final Integer color = segment.style().getColor();
            final Formatting formatting = color == null ? Formatting.WHITE : Formatting.nearest(color);
            if (formatting != current) {
                builder.append(Formatting.CODE_PREFIX).append(formatting.getCode());
                current = formatting;
            }
            builder.append(segment.text());
        }

        return builder.toString();
    }

    /** The inverse: turns an incoming vanilla chat line into a styled text. */
    public static MutableText fromLegacy(final String legacy) {
        final MutableText result = Text.empty();
        if (legacy == null || legacy.isEmpty()) {
            return result;
        }

        Style style = Style.EMPTY;
        final StringBuilder pending = new StringBuilder();

        for (int i = 0; i < legacy.length(); i++) {
            final char c = legacy.charAt(i);
            final Formatting formatting = c == Formatting.CODE_PREFIX && i + 1 < legacy.length()
                ? Formatting.byCode(legacy.charAt(i + 1))
                : null;

            if (formatting == null) {
                pending.append(c);
                continue;
            }

            if (pending.length() > 0) {
                result.append(Text.literal(pending.toString()).setStyle(style));
                pending.setLength(0);
            }
            // A colour code resets the formats, exactly as vanilla treats it.
            style = formatting.isColor() ? Style.EMPTY.withColor(formatting) : style.withFormatting(formatting);
            i++;
        }

        if (pending.length() > 0) {
            result.append(Text.literal(pending.toString()).setStyle(style));
        }

        return result;
    }
}
