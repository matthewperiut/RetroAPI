package com.periut.retroapi.text;

import java.util.Objects;

/**
 * An immutable set of formatting decisions, each of which may be unset ({@code null}) and inherited
 * from the parent text through {@link #withParent}.
 *
 * <p>Colour is a full RGB value rather than one of the sixteen vanilla codes: the mod draws chat
 * itself, so it is not limited to what {@code §} can express. {@link Texts#toLegacy} snaps to the
 * nearest vanilla colour on the way out to a vanilla client.
 */
public class Style {
    public static final Style EMPTY = new Style(null, null, null, null, null, null, null, null, null);

    private final Integer color;
    private final Boolean bold;
    private final Boolean italic;
    private final Boolean underlined;
    private final Boolean strikethrough;
    private final Boolean obfuscated;
    private final ClickEvent clickEvent;
    private final HoverEvent hoverEvent;
    private final String insertion;

    private Style(final Integer color, final Boolean bold, final Boolean italic, final Boolean underlined, final Boolean strikethrough, final Boolean obfuscated, final ClickEvent clickEvent, final HoverEvent hoverEvent, final String insertion) {
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.clickEvent = clickEvent;
        this.hoverEvent = hoverEvent;
        this.insertion = insertion;
    }

    public Integer getColor() {
        return color;
    }

    public int getColorOr(final int fallback) {
        return color == null ? fallback : color;
    }

    public boolean isBold() {
        return bold != null && bold;
    }

    public boolean isItalic() {
        return italic != null && italic;
    }

    public boolean isUnderlined() {
        return underlined != null && underlined;
    }

    public boolean isStrikethrough() {
        return strikethrough != null && strikethrough;
    }

    public boolean isObfuscated() {
        return obfuscated != null && obfuscated;
    }

    public ClickEvent getClickEvent() {
        return clickEvent;
    }

    public HoverEvent getHoverEvent() {
        return hoverEvent;
    }

    public String getInsertion() {
        return insertion;
    }

    public boolean isEmpty() {
        return this == EMPTY || (color == null && bold == null && italic == null && underlined == null
            && strikethrough == null && obfuscated == null && clickEvent == null && hoverEvent == null && insertion == null);
    }

    public Style withColor(final Integer color) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withColor(final Formatting formatting) {
        return withColor(formatting == null || !formatting.isColor() ? null : formatting.getColorValue());
    }

    public Style withBold(final Boolean bold) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withItalic(final Boolean italic) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withUnderline(final Boolean underlined) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withStrikethrough(final Boolean strikethrough) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withObfuscated(final Boolean obfuscated) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withClickEvent(final ClickEvent clickEvent) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withHoverEvent(final HoverEvent hoverEvent) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withInsertion(final String insertion) {
        return new Style(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    public Style withFormatting(final Formatting formatting) {
        if (formatting == null) {
            return this;
        }
        switch (formatting) {
            case BOLD:
                return withBold(Boolean.TRUE);
            case ITALIC:
                return withItalic(Boolean.TRUE);
            case UNDERLINE:
                return withUnderline(Boolean.TRUE);
            case STRIKETHROUGH:
                return withStrikethrough(Boolean.TRUE);
            case OBFUSCATED:
                return withObfuscated(Boolean.TRUE);
            case RESET:
                return EMPTY;
            default:
                return withColor(formatting);
        }
    }

    /** Fills every unset field from {@code parent}, which is how a sibling inherits its parent's look. */
    public Style withParent(final Style parent) {
        if (this == EMPTY) {
            return parent;
        }
        if (parent == EMPTY || parent == null) {
            return this;
        }
        return new Style(
            color != null ? color : parent.color,
            bold != null ? bold : parent.bold,
            italic != null ? italic : parent.italic,
            underlined != null ? underlined : parent.underlined,
            strikethrough != null ? strikethrough : parent.strikethrough,
            obfuscated != null ? obfuscated : parent.obfuscated,
            clickEvent != null ? clickEvent : parent.clickEvent,
            hoverEvent != null ? hoverEvent : parent.hoverEvent,
            insertion != null ? insertion : parent.insertion);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Style)) {
            return false;
        }
        final Style that = (Style) o;
        return Objects.equals(color, that.color)
            && Objects.equals(bold, that.bold)
            && Objects.equals(italic, that.italic)
            && Objects.equals(underlined, that.underlined)
            && Objects.equals(strikethrough, that.strikethrough)
            && Objects.equals(obfuscated, that.obfuscated)
            && Objects.equals(clickEvent, that.clickEvent)
            && Objects.equals(hoverEvent, that.hoverEvent)
            && Objects.equals(insertion, that.insertion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, bold, italic, underlined, strikethrough, obfuscated, clickEvent, hoverEvent, insertion);
    }

    @Override
    public String toString() {
        return "Style{color=" + color + ", bold=" + bold + ", italic=" + italic + ", underlined=" + underlined
            + ", strikethrough=" + strikethrough + ", obfuscated=" + obfuscated + ", clickEvent=" + clickEvent
            + ", hoverEvent=" + hoverEvent + ", insertion=" + insertion + "}";
    }
}
