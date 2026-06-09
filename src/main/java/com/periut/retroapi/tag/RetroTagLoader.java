package com.periut.retroapi.tag;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.periut.retroapi.RetroAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Scans every fabric mod for block tag data files and parses them into raw entry lists,
 * which {@link RetroTags} resolves to Block sets on first query.
 *
 * <p>Scanned layouts inside each mod, for every namespace directory under {@code data/}:</p>
 * <ul>
 *   <li>{@code data/{ns}/tags/block/**.json} (modern, 1.21+)</li>
 *   <li>{@code data/{ns}/tags/blocks/**.json} (modern, pre-1.21 plural)</li>
 *   <li>{@code data/{ns}/stationapi/tags/blocks/**.json} (StationAPI layout)</li>
 * </ul>
 *
 * <p>File schema is the modern one exactly: {@code values} (block ids, {@code #tag} refs,
 * or {@code {"id": ..., "required": false}} objects) and {@code replace}. The same tag path
 * across namespaces and mods unions, unless a file sets {@code replace: true}, in which case
 * it clears what loaded before it (mod iteration order).</p>
 */
final class RetroTagLoader {

	/** One raw value entry: a block name, or a #reference, possibly optional. */
	static final class Entry {
		final String value;
		final boolean required;

		Entry(String value, boolean required) {
			this.value = value;
			this.required = required;
		}

		boolean isReference() {
			return value.startsWith("#");
		}
	}

	private RetroTagLoader() {}

	/** path (e.g. "mineable/pickaxe") to raw entries, in load order. */
	static Map<String, List<Entry>> loadAll() {
		Map<String, List<Entry>> tags = new LinkedHashMap<>();
		int files = 0;
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			for (Path root : mod.getRootPaths()) {
				Path data = root.resolve("data");
				if (!Files.isDirectory(data)) {
					continue;
				}
				try (Stream<Path> namespaces = Files.list(data)) {
					for (Path ns : (Iterable<Path>) namespaces::iterator) {
						if (!Files.isDirectory(ns)) {
							continue;
						}
						files += scanTagRoot(ns.resolve("tags").resolve("block"), tags);
						files += scanTagRoot(ns.resolve("tags").resolve("blocks"), tags);
						files += scanTagRoot(ns.resolve("stationapi").resolve("tags").resolve("blocks"), tags);
					}
				} catch (IOException e) {
					RetroAPI.LOGGER.error("Failed to scan data/ of mod {}", mod.getMetadata().getId(), e);
				}
			}
		}
		if (files > 0) {
			RetroAPI.LOGGER.info("Loaded {} block tag file(s) into {} tag(s)", files, tags.size());
		}
		return tags;
	}

	private static int scanTagRoot(Path tagRoot, Map<String, List<Entry>> tags) {
		if (!Files.isDirectory(tagRoot)) {
			return 0;
		}
		int count = 0;
		try (Stream<Path> walk = Files.walk(tagRoot)) {
			for (Path file : (Iterable<Path>) walk::iterator) {
				if (!Files.isRegularFile(file) || !file.toString().endsWith(".json")) {
					continue;
				}
				String rel = tagRoot.relativize(file).toString().replace('\\', '/');
				String tagPath = rel.substring(0, rel.length() - ".json".length());
				try {
					parseFile(file, tagPath, tags);
					count++;
				} catch (Exception e) {
					RetroAPI.LOGGER.error("Bad tag file {} ({})", file, e.toString());
				}
			}
		} catch (IOException e) {
			RetroAPI.LOGGER.error("Failed to walk tag root {}", tagRoot, e);
		}
		return count;
	}

	private static void parseFile(Path file, String tagPath, Map<String, List<Entry>> tags) throws IOException {
		JsonObject json;
		try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
			json = JsonParser.parseReader(reader).getAsJsonObject();
		}

		boolean replace = json.has("replace") && json.get("replace").getAsBoolean();
		List<Entry> entries = tags.computeIfAbsent(tagPath, k -> new ArrayList<>());
		if (replace) {
			entries.clear();
		}

		JsonArray values = json.getAsJsonArray("values");
		if (values == null) {
			return;
		}
		for (JsonElement element : values) {
			if (element.isJsonObject()) {
				JsonObject obj = element.getAsJsonObject();
				String id = obj.get("id").getAsString();
				boolean required = !obj.has("required") || obj.get("required").getAsBoolean();
				entries.add(new Entry(id, required));
			} else {
				entries.add(new Entry(element.getAsString(), true));
			}
		}
	}
}
