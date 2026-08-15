package com.periut.retroapi.commands.test;

import com.periut.retroapi.text.ClickEvent;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.HoverEvent;
import com.periut.retroapi.text.Json;
import com.periut.retroapi.text.MutableText;
import com.periut.retroapi.text.Style;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.text.TextCodec;
import com.periut.retroapi.text.Texts;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TextTest {
    private TextTest() {
    }

    public static void run() {
        styles();
        legacy();
        json();
        rawJson();
    }

    private static void styles() {
        Tests.group("text styles");

        final MutableText text = Text.literal("hello ")
            .formatted(Formatting.RED)
            .append(Text.literal("world").formatted(Formatting.BOLD));

        Tests.eq("plain string ignores styling", "hello world", text.getString());

        final List<Texts.Segment> segments = Texts.flatten(text);
        Tests.eq("one segment per styled run", 2, segments.size());
        Tests.eq("colour is applied", Formatting.RED.getColorValue(), segments.get(0).style().getColor());
        Tests.check("siblings inherit the parent colour", segments.get(1).style().getColor() == Formatting.RED.getColorValue());
        Tests.check("siblings keep their own formats", segments.get(1).style().isBold());
        Tests.check("parents do not inherit from siblings", !segments.get(0).style().isBold());

        // Style is immutable: a with* call must not disturb the receiver.
        final Style base = Style.EMPTY.withColor(0x123456);
        final Style bolder = base.withBold(Boolean.TRUE);
        Tests.check("with* returns a new style", !base.isBold() && bolder.isBold());
        Tests.eq("with* keeps the other fields", 0x123456, bolder.getColor());
    }

    private static void legacy() {
        Tests.group("legacy codes");

        final MutableText parsed = Texts.fromLegacy("§cred §ftext");
        Tests.eq("legacy text keeps its content", "red text", parsed.getString());
        final List<Integer> colors = Texts.flatten(parsed).stream().map(s -> s.style().getColor()).collect(Collectors.toList());
        Tests.eq("legacy colours are parsed", List.of(Formatting.RED.getColorValue(), Formatting.WHITE.getColorValue()), colors);

        Tests.eq("stripping removes codes", "red text", Formatting.strip("§cred §ftext"));
        Tests.eq("round trip through legacy", "red text", Texts.fromLegacy(Texts.toLegacy(parsed)).getString());

        // An arbitrary RGB has to survive as the closest vanilla colour a beta client can draw.
        Tests.eq("rgb snaps to the nearest code", "§cx", Texts.toLegacy(Text.literal("x").withColor(0xFE5656)));
        Tests.eq("pure red is nearer dark red than red", Formatting.DARK_RED, Formatting.nearest(0xFF0000));
        Tests.eq("exact colours map to themselves", Formatting.GREEN, Formatting.nearest(Formatting.GREEN.getColorValue()));

        // A trailing lone section sign must not be swallowed or crash the parser.
        Tests.eq("dangling section sign is literal", "a§", Texts.fromLegacy("a§").getString());
    }

    private static void json() {
        Tests.group("text json");

        final Text original = Text.literal("click me")
            .formatted(Formatting.GOLD, Formatting.BOLD)
            .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/give @s stone"))
                .withHoverEvent(HoverEvent.showText(Text.literal("a tooltip").formatted(Formatting.GRAY))))
            .append(Text.literal(" and more").formatted(Formatting.AQUA));

        final Text decoded = TextCodec.fromJson(TextCodec.toJson(original));

        Tests.eq("round trip keeps the string", original.getString(), decoded.getString());
        Tests.eq("round trip keeps the style", original.getStyle(), decoded.getStyle());
        Tests.eq("round trip keeps siblings", 1, decoded.getSiblings().size());
        Tests.eq("round trip keeps the click event", "/give @s stone", decoded.getStyle().getClickEvent().getValue());
        Tests.eq("round trip keeps the hover text", "a tooltip", decoded.getStyle().getHoverEvent().getValue().getString());

        // Anything unparseable has to render as itself rather than throw into the chat loop.
        Tests.eq("malformed json degrades to literal", "{oops", TextCodec.fromJson("{oops").getString());

        // Payloads modern Minecraft would send.
        Tests.eq("plain string payload", "hi", TextCodec.fromJson("\"hi\"").getString());
        Tests.eq("array payload concatenates", "ab", TextCodec.fromJson("[\"a\",\"b\"]").getString());
        Tests.eq("hex colour payload", 0xAABBCC, TextCodec.fromJson("{\"text\":\"x\",\"color\":\"#AABBCC\"}").getStyle().getColor());
        Tests.eq("named colour payload", Formatting.RED.getColorValue(), TextCodec.fromJson("{\"text\":\"x\",\"color\":\"red\"}").getStyle().getColor());
        Tests.eq("legacy hover value key", "old", TextCodec.fromJson("{\"text\":\"x\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"old\"}}").getStyle().getHoverEvent().getValue().getString());
    }

    @SuppressWarnings("unchecked")
    private static void rawJson() {
        Tests.group("json");

        Tests.eq("parses nested structures", "b", ((Map<String, Object>) Json.parse("{\"a\":{\"x\":\"b\"}}")).get("a") instanceof Map<?, ?> inner ? inner.get("x") : null);
        Tests.eq("parses numbers", 12.5, Json.parse("12.5"));
        Tests.eq("parses escapes", "a\"b\nc", Json.parse("\"a\\\"b\\nc\""));
        Tests.eq("writes escapes", "\"a\\\"b\"", Json.write("a\"b"));
        Tests.eq("writes whole numbers without a decimal point", "3", Json.write(3.0));
        Tests.eq("skips null map values", "{\"a\":1}", Json.write(new java.util.LinkedHashMap<>(Map.of("a", 1)) {{
            put("b", null);
        }}));
        Tests.throwsError("rejects trailing data", IllegalArgumentException.class, () -> Json.parse("{} junk"));
        Tests.throwsError("rejects an unterminated string", IllegalArgumentException.class, () -> Json.parse("\"abc"));
    }
}
