package com.periut.retroapi.text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal JSON reader/writer.
 *
 * <p>Gson is on the Minecraft classpath but not on a b1.7.3 dedicated server's, and this mod ships
 * no dependencies of its own, so the few hundred bytes of component JSON that cross the wire are
 * handled here. Values map to {@code Map}, {@code List}, {@code String}, {@code Double},
 * {@code Boolean} and {@code null}.
 */
public final class Json {
    private Json() {
    }

    public static Object parse(final String input) {
        final Parser parser = new Parser(input);
        parser.skipWhitespace();
        final Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos < input.length()) {
            throw new IllegalArgumentException("Trailing data at " + parser.pos);
        }
        return value;
    }

    public static String write(final Object value) {
        final StringBuilder builder = new StringBuilder();
        writeValue(builder, value);
        return builder.toString();
    }

    private static void writeValue(final StringBuilder builder, final Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof String) {
            writeString(builder, (String) value);
        } else if (value instanceof Boolean) {
            builder.append(value);
        } else if (value instanceof Number) {
            final double number = ((Number) value).doubleValue();
            if (number == Math.floor(number) && !Double.isInfinite(number)) {
                builder.append((long) number);
            } else {
                builder.append(number);
            }
        } else if (value instanceof Map) {
            builder.append('{');
            boolean first = true;
            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeString(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                writeValue(builder, entry.getValue());
            }
            builder.append('}');
        } else if (value instanceof Iterable) {
            builder.append('[');
            boolean first = true;
            for (final Object element : (Iterable<?>) value) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                writeValue(builder, element);
            }
            builder.append(']');
        } else {
            writeString(builder, String.valueOf(value));
        }
    }

    private static void writeString(final StringBuilder builder, final String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        builder.append('"');
    }

    private static final class Parser {
        private final String input;
        private int pos;

        private Parser(final String input) {
            this.input = input;
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private Object readValue() {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of input");
            }
            final char c = input.charAt(pos);
            return switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readKeyword("true", Boolean.TRUE);
                case 'f' -> readKeyword("false", Boolean.FALSE);
                case 'n' -> readKeyword("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            final Map<String, Object> result = new LinkedHashMap<>();
            pos++;
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == '}') {
                pos++;
                return result;
            }
            while (true) {
                skipWhitespace();
                final String key = readString();
                skipWhitespace();
                expect(':');
                result.put(key, readValue());
                skipWhitespace();
                final char next = next();
                if (next == '}') {
                    return result;
                }
                if (next != ',') {
                    throw new IllegalArgumentException("Expected ',' or '}' at " + pos);
                }
            }
        }

        private List<Object> readArray() {
            final List<Object> result = new ArrayList<>();
            pos++;
            skipWhitespace();
            if (pos < input.length() && input.charAt(pos) == ']') {
                pos++;
                return result;
            }
            while (true) {
                result.add(readValue());
                skipWhitespace();
                final char next = next();
                if (next == ']') {
                    return result;
                }
                if (next != ',') {
                    throw new IllegalArgumentException("Expected ',' or ']' at " + pos);
                }
            }
        }

        private String readString() {
            expect('"');
            final StringBuilder builder = new StringBuilder();
            while (true) {
                final char c = next();
                if (c == '"') {
                    return builder.toString();
                }
                if (c != '\\') {
                    builder.append(c);
                    continue;
                }
                final char escape = next();
                switch (escape) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        builder.append((char) Integer.parseInt(input.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw new IllegalArgumentException("Invalid escape '" + escape + "' at " + pos);
                }
            }
        }

        private Object readNumber() {
            final int start = pos;
            while (pos < input.length() && "+-.eE0123456789".indexOf(input.charAt(pos)) >= 0) {
                pos++;
            }
            if (start == pos) {
                throw new IllegalArgumentException("Unexpected character '" + input.charAt(pos) + "' at " + pos);
            }
            return Double.parseDouble(input.substring(start, pos));
        }

        private Object readKeyword(final String keyword, final Object value) {
            if (!input.startsWith(keyword, pos)) {
                throw new IllegalArgumentException("Unexpected character at " + pos);
            }
            pos += keyword.length();
            return value;
        }

        private char next() {
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of input");
            }
            return input.charAt(pos++);
        }

        private void expect(final char expected) {
            final char actual = next();
            if (actual != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' but found '" + actual + "' at " + pos);
            }
        }
    }
}
