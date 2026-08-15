package com.periut.retroapi.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads and writes texts in modern Minecraft's component JSON, which is what crosses the wire to
 * clients that have the mod. A payload written by modern Minecraft deserialises here, minus the
 * features beta has nothing to say about (item and entity hovers, fonts, scores).
 */
public final class TextCodec {
    private TextCodec() {
    }

    public static String toJson(final Text text) {
        return Json.write(toMap(text));
    }

    public static Text fromJson(final String json) {
        try {
            return fromValue(Json.parse(json));
        } catch (final RuntimeException ex) {
            // Never let a malformed payload take down chat; show it as-is instead.
            return Text.literal(json);
        }
    }

    private static Map<String, Object> toMap(final Text text) {
        final Map<String, Object> result = new LinkedHashMap<>();
        final TextContent content = text.getContent();

        if (content instanceof TranslatableTextContent translatable) {
            result.put("translate", translatable.getKey());
            if (translatable.getArgs().length > 0) {
                final List<Object> args = new ArrayList<>();
                for (final Object arg : translatable.getArgs()) {
                    args.add(arg instanceof Text ? toMap((Text) arg) : String.valueOf(arg));
                }
                result.put("with", args);
            }
        } else if (content instanceof PlainTextContent plain) {
            result.put("text", plain.getString());
        } else {
            result.put("text", "");
        }

        writeStyle(result, text.getStyle());

        if (!text.getSiblings().isEmpty()) {
            final List<Object> extra = new ArrayList<>();
            for (final Text sibling : text.getSiblings()) {
                extra.add(toMap(sibling));
            }
            result.put("extra", extra);
        }

        return result;
    }

    private static void writeStyle(final Map<String, Object> result, final Style style) {
        if (style.getColor() != null) {
            final int rgb = style.getColor();
            final Formatting named = Formatting.nearest(rgb);
            result.put("color", named.getColorValue() == rgb ? named.getName() : String.format("#%06X", rgb));
        }
        if (style.isBold()) {
            result.put("bold", Boolean.TRUE);
        }
        if (style.isItalic()) {
            result.put("italic", Boolean.TRUE);
        }
        if (style.isUnderlined()) {
            result.put("underlined", Boolean.TRUE);
        }
        if (style.isStrikethrough()) {
            result.put("strikethrough", Boolean.TRUE);
        }
        if (style.isObfuscated()) {
            result.put("obfuscated", Boolean.TRUE);
        }
        if (style.getInsertion() != null) {
            result.put("insertion", style.getInsertion());
        }
        if (style.getClickEvent() != null) {
            final Map<String, Object> click = new LinkedHashMap<>();
            click.put("action", style.getClickEvent().getAction().getName());
            click.put("value", style.getClickEvent().getValue());
            result.put("clickEvent", click);
        }
        if (style.getHoverEvent() != null && style.getHoverEvent().getValue() != null) {
            final Map<String, Object> hover = new LinkedHashMap<>();
            hover.put("action", style.getHoverEvent().getAction().getName());
            hover.put("contents", toMap(style.getHoverEvent().getValue()));
            result.put("hoverEvent", hover);
        }
    }

    @SuppressWarnings("unchecked")
    private static Text fromValue(final Object value) {
        if (value instanceof String string) {
            return Text.literal(string);
        }
        if (value instanceof List<?> list) {
            // A bare array is its first element with the rest appended, as in vanilla.
            final MutableText result = list.isEmpty() ? Text.empty() : (MutableText) fromValue(list.get(0)).copy();
            for (int i = 1; i < list.size(); i++) {
                result.append(fromValue(list.get(i)));
            }
            return result;
        }
        if (!(value instanceof Map)) {
            return Text.literal(String.valueOf(value));
        }

        final Map<String, Object> map = (Map<String, Object>) value;
        final MutableText result;

        if (map.get("translate") instanceof String key) {
            final List<Object> args = new ArrayList<>();
            if (map.get("with") instanceof List<?> with) {
                for (final Object arg : with) {
                    args.add(arg instanceof String ? arg : fromValue(arg));
                }
            }
            result = Text.translatable(key, args.toArray());
        } else {
            result = Text.literal(map.get("text") instanceof String text ? text : "");
        }

        result.setStyle(readStyle(map));

        if (map.get("extra") instanceof List<?> extra) {
            for (final Object sibling : extra) {
                result.append(fromValue(sibling));
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static Style readStyle(final Map<String, Object> map) {
        Style style = Style.EMPTY;

        if (map.get("color") instanceof String color) {
            if (color.startsWith("#")) {
                try {
                    style = style.withColor(Integer.parseInt(color.substring(1), 16));
                } catch (final NumberFormatException ignored) {
                }
            } else {
                final Formatting formatting = Formatting.byName(color.toLowerCase(Locale.ROOT));
                if (formatting != null && formatting.isColor()) {
                    style = style.withColor(formatting);
                }
            }
        }

        style = withFlag(style, map, "bold");
        style = withFlag(style, map, "italic");
        style = withFlag(style, map, "underlined");
        style = withFlag(style, map, "strikethrough");
        style = withFlag(style, map, "obfuscated");

        if (map.get("insertion") instanceof String insertion) {
            style = style.withInsertion(insertion);
        }

        if (map.get("clickEvent") instanceof Map<?, ?> click) {
            final ClickEvent.Action action = ClickEvent.Action.byName((String) click.get("action"));
            final Object clickValue = click.get("value");
            if (action != null && clickValue instanceof String) {
                style = style.withClickEvent(new ClickEvent(action, (String) clickValue));
            }
        }

        if (map.get("hoverEvent") instanceof Map<?, ?> hover) {
            final HoverEvent.Action action = HoverEvent.Action.byName((String) hover.get("action"));
            // "contents" is the modern key; "value" is what older payloads use.
            final Object contents = hover.containsKey("contents") ? hover.get("contents") : hover.get("value");
            if (action == HoverEvent.Action.SHOW_TEXT && contents != null) {
                style = style.withHoverEvent(new HoverEvent(action, fromValue(contents)));
            }
        }

        return style;
    }

    private static Style withFlag(final Style style, final Map<String, Object> map, final String key) {
        if (!(map.get(key) instanceof Boolean flag)) {
            return style;
        }
        return switch (key) {
            case "bold" -> style.withBold(flag);
            case "italic" -> style.withItalic(flag);
            case "underlined" -> style.withUnderline(flag);
            case "strikethrough" -> style.withStrikethrough(flag);
            case "obfuscated" -> style.withObfuscated(flag);
            default -> style;
        };
    }
}
