package com.periut.retroapi.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A ~200 line JSON reader/writer.
 *
 * <p>RetroTweaks hard-depends on nothing but the Fabric loader, and Minecraft b1.7.3 ships no JSON
 * library, so the config format the mod was asked for has to be parsed by hand. This handles the
 * subset a config file needs: objects, arrays, strings, numbers, booleans and null, plus {@code //}
 * and {@code /* *}{@code /} comments so hand-edited configs survive a round trip through a human.
 *
 * <p>Parsed objects are {@link LinkedHashMap}s, so key order is preserved and a re-saved config
 * keeps the shape the user (or a previous version) left it in.
 */
public final class Json {

	private Json() {}

	// ---------------------------------------------------------------- parsing

	public static Object parse(String text) {
		Parser p = new Parser(text);
		p.skipWhitespace();
		Object value = p.readValue();
		p.skipWhitespace();
		if (!p.atEnd()) throw new IllegalArgumentException("Trailing content at index " + p.index);
		return value;
	}

	/** Parses and casts to an object, returning an empty map for anything else (including null). */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseObject(String text) {
		Object value = parse(text);
		return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
	}

	private static final class Parser {
		private final String src;
		private int index;

		Parser(String src) { this.src = src; }

		boolean atEnd() { return index >= src.length(); }

		void skipWhitespace() {
			while (index < src.length()) {
				char c = src.charAt(index);
				if (c == '/' && index + 1 < src.length()) {
					char next = src.charAt(index + 1);
					if (next == '/') {
						while (index < src.length() && src.charAt(index) != '\n') index++;
						continue;
					}
					if (next == '*') {
						index += 2;
						while (index + 1 < src.length() && !(src.charAt(index) == '*' && src.charAt(index + 1) == '/')) index++;
						index = Math.min(index + 2, src.length());
						continue;
					}
					return;
				}
				if (c != ' ' && c != '\t' && c != '\n' && c != '\r') return;
				index++;
			}
		}

		Object readValue() {
			if (atEnd()) throw new IllegalArgumentException("Unexpected end of input");
			char c = src.charAt(index);
			switch (c) {
				case '{': return readObject();
				case '[': return readArray();
				case '"': return readString();
				default:
					if (startsWith("true")) { index += 4; return Boolean.TRUE; }
					if (startsWith("false")) { index += 5; return Boolean.FALSE; }
					if (startsWith("null")) { index += 4; return null; }
					return readNumber();
			}
		}

		private boolean startsWith(String literal) {
			return src.regionMatches(index, literal, 0, literal.length());
		}

		Map<String, Object> readObject() {
			Map<String, Object> map = new LinkedHashMap<>();
			index++; // '{'
			skipWhitespace();
			if (!atEnd() && src.charAt(index) == '}') { index++; return map; }
			while (true) {
				skipWhitespace();
				String key = readString();
				skipWhitespace();
				expect(':');
				skipWhitespace();
				map.put(key, readValue());
				skipWhitespace();
				if (atEnd()) throw new IllegalArgumentException("Unterminated object");
				char c = src.charAt(index++);
				if (c == '}') return map;
				if (c != ',') throw new IllegalArgumentException("Expected ',' or '}' at index " + (index - 1));
				skipWhitespace();
				// tolerate a trailing comma before '}'
				if (!atEnd() && src.charAt(index) == '}') { index++; return map; }
			}
		}

		List<Object> readArray() {
			List<Object> list = new ArrayList<>();
			index++; // '['
			skipWhitespace();
			if (!atEnd() && src.charAt(index) == ']') { index++; return list; }
			while (true) {
				skipWhitespace();
				list.add(readValue());
				skipWhitespace();
				if (atEnd()) throw new IllegalArgumentException("Unterminated array");
				char c = src.charAt(index++);
				if (c == ']') return list;
				if (c != ',') throw new IllegalArgumentException("Expected ',' or ']' at index " + (index - 1));
				skipWhitespace();
				if (!atEnd() && src.charAt(index) == ']') { index++; return list; }
			}
		}

		String readString() {
			expect('"');
			StringBuilder out = new StringBuilder();
			while (true) {
				if (atEnd()) throw new IllegalArgumentException("Unterminated string");
				char c = src.charAt(index++);
				if (c == '"') return out.toString();
				if (c != '\\') { out.append(c); continue; }
				char esc = src.charAt(index++);
				switch (esc) {
					case '"': out.append('"'); break;
					case '\\': out.append('\\'); break;
					case '/': out.append('/'); break;
					case 'b': out.append('\b'); break;
					case 'f': out.append('\f'); break;
					case 'n': out.append('\n'); break;
					case 'r': out.append('\r'); break;
					case 't': out.append('\t'); break;
					case 'u':
						out.append((char) Integer.parseInt(src.substring(index, index + 4), 16));
						index += 4;
						break;
					default: throw new IllegalArgumentException("Bad escape \\" + esc);
				}
			}
		}

		Number readNumber() {
			int start = index;
			if (!atEnd() && (src.charAt(index) == '-' || src.charAt(index) == '+')) index++;
			boolean floating = false;
			while (!atEnd()) {
				char c = src.charAt(index);
				if (c >= '0' && c <= '9') { index++; continue; }
				if (c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') { floating |= (c == '.' || c == 'e' || c == 'E'); index++; continue; }
				break;
			}
			String text = src.substring(start, index);
			if (text.isEmpty()) throw new IllegalArgumentException("Expected a value at index " + start);
			if (floating) return Double.parseDouble(text);
			try {
				return Long.parseLong(text);
			} catch (NumberFormatException e) {
				return Double.parseDouble(text);
			}
		}

		void expect(char c) {
			if (atEnd() || src.charAt(index) != c) {
				throw new IllegalArgumentException("Expected '" + c + "' at index " + index);
			}
			index++;
		}
	}

	// ---------------------------------------------------------------- writing

	/** Writes {@code value} as pretty-printed JSON with tab indentation. */
	public static String write(Object value) {
		StringBuilder out = new StringBuilder();
		writeValue(out, value, 0);
		out.append('\n');
		return out.toString();
	}

	private static void writeValue(StringBuilder out, Object value, int depth) {
		if (value == null) { out.append("null"); return; }
		if (value instanceof Map<?, ?> map) {
			if (map.isEmpty()) { out.append("{}"); return; }
			out.append("{\n");
			int i = 0;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				indent(out, depth + 1);
				writeString(out, String.valueOf(entry.getKey()));
				out.append(": ");
				writeValue(out, entry.getValue(), depth + 1);
				if (++i < map.size()) out.append(',');
				out.append('\n');
			}
			indent(out, depth);
			out.append('}');
			return;
		}
		if (value instanceof List<?> list) {
			if (list.isEmpty()) { out.append("[]"); return; }
			out.append('[');
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) out.append(", ");
				writeValue(out, list.get(i), depth + 1);
			}
			out.append(']');
			return;
		}
		if (value instanceof String s) { writeString(out, s); return; }
		if (value instanceof Boolean b) { out.append(b.booleanValue()); return; }
		if (value instanceof Float || value instanceof Double) {
			double d = ((Number) value).doubleValue();
			// Keep whole floats readable ("0.0" not "0"), but never emit scientific notation.
			out.append(d == Math.rint(d) && Math.abs(d) < 1e15 ? String.format(java.util.Locale.ROOT, "%.1f", d) : Double.toString(d));
			return;
		}
		if (value instanceof Number n) { out.append(n.longValue()); return; }
		writeString(out, String.valueOf(value));
	}

	private static void indent(StringBuilder out, int depth) {
		out.append("\t".repeat(depth));
	}

	private static void writeString(StringBuilder out, String s) {
		out.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '"': out.append("\\\""); break;
				case '\\': out.append("\\\\"); break;
				case '\n': out.append("\\n"); break;
				case '\r': out.append("\\r"); break;
				case '\t': out.append("\\t"); break;
				default:
					if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
					else out.append(c);
			}
		}
		out.append('"');
	}
}
