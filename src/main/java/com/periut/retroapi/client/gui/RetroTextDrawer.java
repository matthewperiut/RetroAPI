package com.periut.retroapi.client.gui;

import com.periut.retroapi.text.Style;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.text.Texts;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.CharacterUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Draws {@link Text} with beta's font renderer.
 *
 * <p>Beta understands sixteen colour codes and nothing else - {@code §l} and friends were added
 * years later - so bold, underline, strikethrough and obfuscation are drawn here instead: bold by
 * stamping the text twice a pixel apart, the two lines by filling a one-pixel rectangle, and
 * obfuscation by substituting characters of the same width, which is exactly how modern Minecraft
 * produces all four effects.
 *
 * <p>Extends {@link DrawContext} only to reach its protected {@code fill}.
 */
public class RetroTextDrawer extends DrawContext {
    public static final RetroTextDrawer INSTANCE = new RetroTextDrawer();

    private static final Random OBFUSCATION_RANDOM = new Random();
    private static final int DEFAULT_COLOR = 0xE0E0E0;

    private RetroTextDrawer() {
    }

    /** A run of same-styled text, already measured. */
    public record Line(List<Texts.Segment> segments) {
        public static final Line EMPTY = new Line(List.of());
    }

    public int draw(final TextRenderer textRenderer, final Text text, final int x, final int y, final int alpha) {
        return draw(textRenderer, flatten(text), x, y, DEFAULT_COLOR, alpha);
    }

    public int draw(final TextRenderer textRenderer, final Line line, final int x, final int y, final int alpha) {
        return draw(textRenderer, line, x, y, DEFAULT_COLOR, alpha);
    }

    /**
     * @param alpha 0-255; beta's font renderer honours the alpha byte of the colour, which is what
     *              lets chat fade out rather than blink away
     * @return the x coordinate just past the text
     */
    public int draw(final TextRenderer textRenderer, final Line line, final int x, final int y, final int defaultColor, final int alpha) {
        int cursor = x;

        for (final Texts.Segment segment : line.segments()) {
            final Style style = segment.style();
            final String content = style.isObfuscated() ? obfuscate(textRenderer, segment.text()) : segment.text();
            final int rgb = style.getColorOr(defaultColor);
            final int color = (Math.max(4, alpha) << 24) | (rgb & 0xFFFFFF);

            textRenderer.drawWithShadow(content, cursor, y, color);
            if (style.isBold()) {
                textRenderer.drawWithShadow(content, cursor + 1, y, color);
            }

            final int width = textRenderer.getWidth(content) + (style.isBold() ? 1 : 0);

            if (style.isStrikethrough()) {
                fill(cursor, y + 3, cursor + width, y + 4, color);
            }
            if (style.isUnderlined()) {
                fill(cursor, y + 9, cursor + width, y + 10, color);
            }

            cursor += width;
        }

        return cursor;
    }

    public int width(final TextRenderer textRenderer, final Text text) {
        return width(textRenderer, flatten(text));
    }

    public int width(final TextRenderer textRenderer, final Line line) {
        int width = 0;
        for (final Texts.Segment segment : line.segments()) {
            width += textRenderer.getWidth(segment.text()) + (segment.style().isBold() ? 1 : 0);
        }
        return width;
    }

    /** The style of the character {@code offsetX} pixels into the line, or null past its end. */
    public Style styleAt(final TextRenderer textRenderer, final Line line, final int offsetX) {
        if (offsetX < 0) {
            return null;
        }

        int cursor = 0;
        for (final Texts.Segment segment : line.segments()) {
            final int width = textRenderer.getWidth(segment.text()) + (segment.style().isBold() ? 1 : 0);
            if (offsetX < cursor + width) {
                return segment.style();
            }
            cursor += width;
        }

        return null;
    }

    public static Line flatten(final Text text) {
        return new Line(Texts.flatten(text));
    }

    /**
     * Breaks a line to fit, preferring to break at a space and falling back to mid-word when a
     * single word is longer than the whole chat window.
     */
    public List<Line> wrap(final TextRenderer textRenderer, final Text text, final int maxWidth) {
        final List<Line> lines = new ArrayList<>();
        List<Texts.Segment> current = new ArrayList<>();
        int currentWidth = 0;

        for (final Texts.Segment segment : Texts.flatten(text)) {
            String remaining = segment.text();

            while (!remaining.isEmpty()) {
                final int available = maxWidth - currentWidth;
                final int fitting = fittingLength(textRenderer, remaining, available);

                if (fitting == remaining.length()) {
                    current.add(new Texts.Segment(remaining, segment.style()));
                    currentWidth += textRenderer.getWidth(remaining);
                    break;
                }

                // Nothing fits on a line that already has content: start a new one instead of
                // emitting an empty line and looping forever.
                if (fitting == 0 && currentWidth == 0) {
                    current.add(new Texts.Segment(remaining.substring(0, 1), segment.style()));
                    remaining = remaining.substring(1);
                    lines.add(new Line(current));
                    current = new ArrayList<>();
                    currentWidth = 0;
                    continue;
                }

                if (fitting == 0) {
                    lines.add(new Line(current));
                    current = new ArrayList<>();
                    currentWidth = 0;
                    continue;
                }

                final int breakAt = breakPoint(remaining, fitting);
                current.add(new Texts.Segment(remaining.substring(0, breakAt), segment.style()));
                lines.add(new Line(current));
                current = new ArrayList<>();
                currentWidth = 0;

                // A break at a space swallows it, as vanilla word wrapping does.
                remaining = remaining.substring(breakAt < remaining.length() && remaining.charAt(breakAt) == ' ' ? breakAt + 1 : breakAt);
            }
        }

        if (!current.isEmpty() || lines.isEmpty()) {
            lines.add(new Line(current));
        }

        return lines;
    }

    private static int fittingLength(final TextRenderer textRenderer, final String text, final int available) {
        if (available <= 0) {
            return 0;
        }

        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += textRenderer.getWidth(String.valueOf(text.charAt(i)));
            if (width > available) {
                return i;
            }
        }
        return text.length();
    }

    private static int breakPoint(final String text, final int limit) {
        for (int i = limit; i > 0; i--) {
            if (text.charAt(i - 1) == ' ') {
                return i - 1;
            }
        }
        return limit;
    }

    /** Swaps each character for a random one the same number of pixels wide. */
    private static String obfuscate(final TextRenderer textRenderer, final String text) {
        final StringBuilder result = new StringBuilder(text.length());

        for (int i = 0; i < text.length(); i++) {
            final char original = text.charAt(i);
            final int index = CharacterUtils.VALID_CHARACTERS.indexOf(original);
            if (index < 0) {
                result.append(original);
                continue;
            }

            final int width = textRenderer.characterWidths[index + 32];
            char replacement = original;
            for (int attempt = 0; attempt < 16; attempt++) {
                final int candidate = OBFUSCATION_RANDOM.nextInt(CharacterUtils.VALID_CHARACTERS.length());
                if (textRenderer.characterWidths[candidate + 32] == width) {
                    replacement = CharacterUtils.VALID_CHARACTERS.charAt(candidate);
                    break;
                }
            }
            result.append(replacement);
        }

        return result.toString();
    }

    /** Exposed so the chat window and the suggestion window can share one background style. */
    public void box(final int x1, final int y1, final int x2, final int y2, final int color) {
        fill(x1, y1, x2, y2, color);
    }
}
