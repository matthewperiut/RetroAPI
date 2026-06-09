package com.periut.retroapi.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.client.render.RetroRenderLayer;
import com.periut.retroapi.client.render.RetroRenderLayers;
import com.periut.retroapi.state.RetroBlockState;
import com.periut.retroapi.state.RetroBoolProperty;
import com.periut.retroapi.state.RetroIntProperty;
import com.periut.retroapi.state.RetroProperty;
import com.periut.retroapi.state.RetroStates;
import net.minecraft.block.Block;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads blockstate JSONs (vanilla {@code variants}/{@code multipart} format plus the retro
 * extensions {@code properties} and {@code render_layer}) and builds, per flattened state
 * index, the list of model choices the renderer draws. Matching semantics follow vanilla:
 * variant keys are comma-joined partial matches (most specific wins, "" is the catch-all),
 * weighted arrays pick deterministically from a position-seeded hash, multipart conditions
 * support {@code value|value} alternation and {@code OR}.
 */
public final class BlockstateLoader {

	/** One model choice: reference plus variant rotation, uvlock and weight. */
	public static final class ModelVariant {
		public final String model;
		public final int x;
		public final int y;
		public final boolean uvlock;
		public final int weight;
		private RetroBakedModel baked;

		ModelVariant(String model, int x, int y, boolean uvlock, int weight) {
			this.model = model;
			this.x = x;
			this.y = y;
			this.uvlock = uvlock;
			this.weight = weight;
		}

		public RetroBakedModel baked() {
			if (baked == null) {
				baked = RetroBakedModel.bake(RetroModelLoader.load(model), x, y, uvlock);
			}
			return baked;
		}
	}

	/** One render unit: pick one option by weight. Multipart contributes several units. */
	public static final class WeightedList {
		public final List<ModelVariant> options = new ArrayList<>();
		public int totalWeight = 0;

		void add(ModelVariant variant) {
			options.add(variant);
			totalWeight += variant.weight;
		}

		public ModelVariant pick(long seed) {
			if (options.size() == 1) {
				return options.get(0);
			}
			int target = (int) (Math.abs(seed % totalWeight));
			for (ModelVariant option : options) {
				target -= option.weight;
				if (target < 0) {
					return option;
				}
			}
			return options.get(0);
		}
	}

	/** The per-state render units for one block. */
	public static final class BlockModelTable {
		public final List<WeightedList>[] byState;

		@SuppressWarnings("unchecked")
		BlockModelTable(int stateCount) {
			this.byState = new List[stateCount];
		}

		public List<WeightedList> forState(int index) {
			if (index < 0 || index >= byState.length) {
				index = 0;
			}
			List<WeightedList> units = byState[index];
			return units != null ? units : java.util.Collections.emptyList();
		}

		/** The default state's first model, for the particle sprite. */
		public RetroModel firstModel() {
			for (List<WeightedList> units : byState) {
				if (units != null && !units.isEmpty() && !units.get(0).options.isEmpty()) {
					return RetroModelLoader.load(units.get(0).options.get(0).model);
				}
			}
			return null;
		}
	}

	private static final Map<Block, BlockModelTable> TABLES = new HashMap<>();

	private BlockstateLoader() {}

	public static BlockModelTable tableFor(Block block) {
		return TABLES.get(block);
	}

	/**
	 * Loads the blockstate JSON for a block being registered, if one exists. Declares
	 * data-driven properties, applies the render layer, and builds the state-to-models
	 * table. Returns the table, or null when the block has no blockstate JSON.
	 */
	public static BlockModelTable tryLoad(Block block, NamespacedIdentifier id) {
		JsonObject json = readJson(id.namespace(), id.identifier());
		if (json == null) {
			return null;
		}

		// Inline state definition; code declarations (states(...)) win on conflict.
		if (json.has("properties")) {
			List<RetroProperty<?>> properties = new ArrayList<>();
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("properties").entrySet()) {
				properties.add(parseProperty(entry.getKey(), entry.getValue()));
			}
			RetroStates.defineFromData(block, properties);
		}

		if (json.has("render_layer")) {
			String layer = json.get("render_layer").getAsString().toUpperCase(java.util.Locale.ROOT);
			try {
				RetroRenderLayers.set(block, RetroRenderLayer.valueOf(layer));
			} catch (IllegalArgumentException e) {
				RetroAPI.LOGGER.warn("Unknown render_layer '{}' in blockstate for {}", layer, id);
			}
		}

		int stateCount = stateCountOf(block);
		BlockModelTable table = new BlockModelTable(stateCount);

		if (json.has("variants")) {
			JsonObject variants = json.getAsJsonObject("variants");
			for (int index = 0; index < stateCount; index++) {
				RetroBlockState state = RetroStates.fromIndex(block, index);
				String bestKey = null;
				int bestPairs = -1;
				for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
					int pairs = matchPairs(entry.getKey(), state, block);
					if (pairs > bestPairs) {
						bestPairs = pairs;
						bestKey = entry.getKey();
					}
				}
				if (bestKey != null) {
					WeightedList unit = parseUnit(variants.get(bestKey));
					List<WeightedList> units = new ArrayList<>();
					units.add(unit);
					table.byState[index] = units;
				}
			}
		}

		if (json.has("multipart")) {
			JsonArray parts = json.getAsJsonArray("multipart");
			for (int index = 0; index < stateCount; index++) {
				RetroBlockState state = RetroStates.fromIndex(block, index);
				List<WeightedList> units = table.byState[index];
				for (JsonElement partEl : parts) {
					JsonObject part = partEl.getAsJsonObject();
					if (!part.has("when") || matchesCondition(part.getAsJsonObject("when"), state, block)) {
						if (units == null) {
							units = new ArrayList<>();
							table.byState[index] = units;
						}
						units.add(parseUnit(part.get("apply")));
					}
				}
			}
		}

		// Load EVERY referenced model now, not lazily: model loading is what registers the
		// model's textures, and texture slots must exist before the atlas composites at the
		// first terrain.png load. A model first loaded at render time would sit on an empty
		// (fully transparent) atlas slot and the block would draw invisible.
		for (List<WeightedList> units : table.byState) {
			if (units == null) {
				continue;
			}
			for (WeightedList unit : units) {
				for (ModelVariant variant : unit.options) {
					RetroModelLoader.load(variant.model);
				}
			}
		}

		TABLES.put(block, table);
		return table;
	}

	private static int stateCountOf(Block block) {
		return RetroStates.stateCount(block);
	}

	private static RetroProperty<?> parseProperty(String name, JsonElement spec) {
		if (spec.isJsonPrimitive() && "boolean".equals(spec.getAsString())) {
			return RetroBoolProperty.of(name);
		}
		if (spec.isJsonObject()) {
			JsonObject range = spec.getAsJsonObject();
			return RetroIntProperty.of(name, range.get("min").getAsInt(), range.get("max").getAsInt());
		}
		// Array of value names: a string-enum property expressed as an int-indexed list is
		// awkward, so model it as a NAMED property backed by the value list.
		JsonArray array = spec.getAsJsonArray();
		List<String> values = new ArrayList<>();
		for (JsonElement value : array) {
			values.add(value.getAsString());
		}
		return new DataEnumProperty(name, values);
	}

	/** A data-declared enum property: string values straight from the JSON list. */
	public static final class DataEnumProperty extends RetroProperty<String> {
		private final List<String> values;

		DataEnumProperty(String name, List<String> values) {
			super(name);
			this.values = java.util.Collections.unmodifiableList(values);
		}

		@Override
		public List<String> values() {
			return values;
		}

		@Override
		public String valueName(String value) {
			return value;
		}

		@Override
		public String parse(String name) {
			return values.contains(name) ? name : null;
		}
	}

	/** Counts matching prop=value pairs in a variant key, or -1 when any pair mismatches. */
	private static int matchPairs(String key, RetroBlockState state, Block block) {
		// "" is the modern catch-all; "normal" is the 1.8/StationAPI-era spelling.
		if (key.isEmpty() || key.equals("normal")) {
			return 0;
		}
		int pairs = 0;
		for (String pair : key.split(",")) {
			int eq = pair.indexOf('=');
			if (eq < 0) {
				return -1;
			}
			if (!valueMatches(state, block, pair.substring(0, eq).trim(), pair.substring(eq + 1).trim())) {
				return -1;
			}
			pairs++;
		}
		return pairs;
	}

	private static boolean matchesCondition(JsonObject when, RetroBlockState state, Block block) {
		if (when.has("OR")) {
			for (JsonElement option : when.getAsJsonArray("OR")) {
				if (matchesCondition(option.getAsJsonObject(), state, block)) {
					return true;
				}
			}
			return false;
		}
		for (Map.Entry<String, JsonElement> entry : when.entrySet()) {
			String expected = entry.getValue().getAsString();
			boolean any = false;
			for (String alternative : expected.split("\\|")) {
				if (valueMatches(state, block, entry.getKey(), alternative.trim())) {
					any = true;
					break;
				}
			}
			if (!any) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static boolean valueMatches(RetroBlockState state, Block block, String propertyName, String valueName) {
		RetroProperty<?> property = RetroStates.property(block, propertyName);
		if (property == null) {
			return false;
		}
		Object parsed = property.parse(valueName);
		if (parsed == null) {
			return false;
		}
		return state.get((RetroProperty<Object>) property).equals(parsed);
	}

	private static WeightedList parseUnit(JsonElement spec) {
		WeightedList unit = new WeightedList();
		if (spec.isJsonArray()) {
			for (JsonElement option : spec.getAsJsonArray()) {
				unit.add(parseVariant(option.getAsJsonObject()));
			}
		} else {
			unit.add(parseVariant(spec.getAsJsonObject()));
		}
		return unit;
	}

	private static ModelVariant parseVariant(JsonObject json) {
		return new ModelVariant(
			json.get("model").getAsString(),
			json.has("x") ? json.get("x").getAsInt() : 0,
			json.has("y") ? json.get("y").getAsInt() : 0,
			json.has("uvlock") && json.get("uvlock").getAsBoolean(),
			json.has("weight") ? json.get("weight").getAsInt() : 1);
	}

	private static JsonObject readJson(String ns, String name) {
		String[] candidates = {
			"/assets/" + ns + "/retroapi/blockstates/" + name + ".json",
			"/assets/" + ns + "/blockstates/" + name + ".json",
			"/assets/" + ns + "/stationapi/blockstates/" + name + ".json",
		};
		for (String candidate : candidates) {
			try (InputStream is = BlockstateLoader.class.getResourceAsStream(candidate)) {
				if (is != null) {
					return JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
				}
			} catch (Exception e) {
				RetroAPI.LOGGER.error("Failed to read blockstate {}: {}", candidate, e.toString());
				return null;
			}
		}
		return null;
	}
}
