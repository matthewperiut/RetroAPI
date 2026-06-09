package com.periut.retroapi.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.register.block.RetroTextures;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads and resolves model JSONs (vanilla 1.8-era schema): parent chains, texture
 * variables, elements and faces. The standard parents ship embedded so resource packs
 * need no vanilla assets. Texture values are registered through {@link RetroTextures}
 * (deduplicated by identifier, so model textures and {@code .texture()} calls share slots).
 *
 * <p>Model references are {@code ns:block/name} or {@code ns:item/name}; files resolve at
 * {@code assets/{ns}/retroapi/models/{path}.json}, then {@code assets/{ns}/models/...},
 * then {@code assets/{ns}/stationapi/models/...}.</p>
 */
public final class RetroModelLoader {

	private static final Map<String, RetroModel> CACHE = new HashMap<>();
	private static final Map<String, com.periut.retroapi.register.block.RetroTexture> TEXTURES_BY_ID = new HashMap<>();
	private static final Map<String, String> EMBEDDED_PARENTS = new HashMap<>();

	private static final String[] FACE_NAMES = {"down", "up", "north", "south", "west", "east"};

	static {
		EMBEDDED_PARENTS.put("block/block", "{}");
		EMBEDDED_PARENTS.put("block/cube",
			"{\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
				+ "\"down\":{\"texture\":\"#down\",\"cullface\":\"down\"},"
				+ "\"up\":{\"texture\":\"#up\",\"cullface\":\"up\"},"
				+ "\"north\":{\"texture\":\"#north\",\"cullface\":\"north\"},"
				+ "\"south\":{\"texture\":\"#south\",\"cullface\":\"south\"},"
				+ "\"west\":{\"texture\":\"#west\",\"cullface\":\"west\"},"
				+ "\"east\":{\"texture\":\"#east\",\"cullface\":\"east\"}}}]}");
		EMBEDDED_PARENTS.put("block/cube_all",
			"{\"parent\":\"block/cube\",\"textures\":{\"particle\":\"#all\",\"down\":\"#all\",\"up\":\"#all\","
				+ "\"north\":\"#all\",\"south\":\"#all\",\"west\":\"#all\",\"east\":\"#all\"}}");
		EMBEDDED_PARENTS.put("block/cube_column",
			"{\"parent\":\"block/cube\",\"textures\":{\"particle\":\"#side\",\"down\":\"#end\",\"up\":\"#end\","
				+ "\"north\":\"#side\",\"south\":\"#side\",\"west\":\"#side\",\"east\":\"#side\"}}");
		EMBEDDED_PARENTS.put("block/cube_bottom_top",
			"{\"parent\":\"block/cube\",\"textures\":{\"particle\":\"#side\",\"down\":\"#bottom\",\"up\":\"#top\","
				+ "\"north\":\"#side\",\"south\":\"#side\",\"west\":\"#side\",\"east\":\"#side\"}}");
		EMBEDDED_PARENTS.put("block/cross",
			"{\"ambientocclusion\":false,\"textures\":{\"particle\":\"#cross\"},\"elements\":["
				+ "{\"from\":[0.8,0,8],\"to\":[15.2,16,8],\"shade\":false,\"rotation\":{\"origin\":[8,8,8],\"axis\":\"y\",\"angle\":45,\"rescale\":true},"
				+ "\"faces\":{\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#cross\"},\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#cross\"}}},"
				+ "{\"from\":[8,0,0.8],\"to\":[8,16,15.2],\"shade\":false,\"rotation\":{\"origin\":[8,8,8],\"axis\":\"y\",\"angle\":45,\"rescale\":true},"
				+ "\"faces\":{\"west\":{\"uv\":[0,0,16,16],\"texture\":\"#cross\"},\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#cross\"}}}]}");
		// The horizontal column: ends on the x faces, side grain rotated so the
		// blockstate's x=90 lays the texture along the log. Matches vanilla.
		EMBEDDED_PARENTS.put("block/cube_column_horizontal",
			"{\"textures\":{\"particle\":\"#side\"},\"elements\":[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
				+ "\"down\":{\"texture\":\"#side\",\"cullface\":\"down\",\"rotation\":90},"
				+ "\"up\":{\"texture\":\"#side\",\"cullface\":\"up\",\"rotation\":90},"
				+ "\"north\":{\"texture\":\"#side\",\"cullface\":\"north\",\"rotation\":90},"
				+ "\"south\":{\"texture\":\"#side\",\"cullface\":\"south\",\"rotation\":90},"
				+ "\"west\":{\"texture\":\"#end\",\"cullface\":\"west\"},"
				+ "\"east\":{\"texture\":\"#end\",\"cullface\":\"east\"}}}]}");
		// Furnace-style: a front face, matching sides, top and bottom. orientable's
		// bottom defaults to the top texture, exactly like vanilla's parent pair.
		EMBEDDED_PARENTS.put("block/orientable_with_bottom",
			"{\"parent\":\"block/cube\",\"textures\":{\"particle\":\"#front\",\"down\":\"#bottom\",\"up\":\"#top\","
				+ "\"north\":\"#front\",\"south\":\"#side\",\"west\":\"#side\",\"east\":\"#side\"}}");
		EMBEDDED_PARENTS.put("block/orientable",
			"{\"parent\":\"block/orientable_with_bottom\",\"textures\":{\"bottom\":\"#top\"}}");
		EMBEDDED_PARENTS.put("item/generated", "{}");
		EMBEDDED_PARENTS.put("item/handheld", "{}");
	}

	private RetroModelLoader() {}

	/** Loads (or returns the cached) resolved model for a reference like {@code mymod:block/lamp}. */
	public static RetroModel load(String reference) {
		RetroModel cached = CACHE.get(reference);
		if (cached != null) {
			return cached;
		}
		RetroModel model = new RetroModel();
		Map<String, String> textureVars = new HashMap<>();
		try {
			resolveChain(reference, model, textureVars, 0);
		} catch (Exception e) {
			RetroAPI.LOGGER.error("Failed to load model {}: {}", reference, e.toString());
		}
		// Resolve texture variables (following #indirection through the merged map) to slots.
		for (Map.Entry<String, String> entry : textureVars.entrySet()) {
			String value = follow(entry.getValue(), textureVars);
			if (value == null || value.startsWith("#")) {
				// A texture variable that never bottomed out in a real texture: a typo, or a
				// parent expecting a #variable this model never supplied. Say so loudly; the
				// faces using it would otherwise just render as a silent missing texture.
				RetroAPI.LOGGER.warn("Model '{}': texture variable '#{}' did not resolve (chains to '{}'); faces using it will be untextured",
					reference, entry.getKey(), entry.getValue());
				continue;
			}
			model.textures.put(entry.getKey(), textureFor(value));
		}
		CACHE.put(reference, model);
		return model;
	}

	/** The raw parent JSON name of an item model, used to classify item model shapes. */
	public static String rawParent(String reference) {
		JsonObject json = readJson(reference);
		return json != null && json.has("parent") ? json.get("parent").getAsString() : null;
	}

	private static String follow(String value, Map<String, String> vars) {
		int hops = 0;
		while (value != null && value.startsWith("#") && hops++ < 16) {
			value = vars.get(value.substring(1));
		}
		return value;
	}

	private static com.periut.retroapi.register.block.RetroTexture textureFor(String id) {
		com.periut.retroapi.register.block.RetroTexture existing = TEXTURES_BY_ID.get(id);
		if (existing != null) {
			return existing;
		}
		int colon = id.indexOf(':');
		String ns = colon >= 0 ? id.substring(0, colon) : "minecraft";
		String path = colon >= 0 ? id.substring(colon + 1) : id;

		// Vanilla references ("minecraft:block/cobblestone") resolve straight to the
		// vanilla atlas sprite by name, like StationAPI and modern versions; models
		// copied from modern packs just work, no PNG re-shipping. Unknown names fall
		// through to the file path below (so a mod CAN override by shipping the file).
		if ("minecraft".equals(ns)) {
			Integer sprite = VanillaTextureNames.blockSprite(path);
			if (sprite != null) {
				com.periut.retroapi.register.block.RetroTexture vanilla =
					RetroTextures.vanillaTexture(NamespacedIdentifiers.from(ns, path), sprite);
				TEXTURES_BY_ID.put(id, vanilla);
				return vanilla;
			}
			// A minecraft: block-texture name beta has no sprite for, usually a newer-pack
			// name with no beta equivalent. It is not fatal (the model still bakes, that
			// face just shows the missing texture), but it is almost never intended, so warn
			// rather than dropping it silently. (item/ names are resolved on the item path.)
			if (!path.startsWith("item/")) {
				RetroAPI.LOGGER.warn("Texture '{}' is a minecraft: name with no beta sprite; that face will render as a missing texture", id);
			}
		}

		// Texture ids in models look like "mymod:block/lamp"; addBlockTexture prepends
		// "block/", so strip a leading block/ but keep deeper folders intact.
		String registered = path.startsWith("block/") ? path.substring("block/".length())
			: path.startsWith("blocks/") ? path.substring("blocks/".length()) : path; // pre-flattening plural
		com.periut.retroapi.register.block.RetroTexture tex =
			RetroTextures.addBlockTexture(NamespacedIdentifiers.from(ns, registered));
		TEXTURES_BY_ID.put(id, tex);
		return tex;
	}

	private static void resolveChain(String reference, RetroModel model, Map<String, String> textureVars, int depth) {
		if (depth > 16) {
			RetroAPI.LOGGER.warn("Model parent chain too deep at {}", reference);
			return;
		}
		JsonObject json = readJson(reference);
		if (json == null) {
			RetroAPI.LOGGER.warn("Model not found: {}", reference);
			return;
		}

		// Parents first: children override textures and replace elements.
		if (json.has("parent")) {
			resolveChain(json.get("parent").getAsString(), model, textureVars, depth + 1);
		}

		if (json.has("ambientocclusion")) {
			model.ambientOcclusion = json.get("ambientocclusion").getAsBoolean();
		}

		if (json.has("textures")) {
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
				textureVars.put(entry.getKey(), entry.getValue().getAsString());
			}
		}

		if (json.has("elements")) {
			// Like vanilla: a child's elements REPLACE the parent's.
			model.elements.clear();
			for (JsonElement el : json.getAsJsonArray("elements")) {
				model.elements.add(parseElement(el.getAsJsonObject()));
			}
		}

		if (json.has("display")) {
			for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("display").entrySet()) {
				JsonObject t = entry.getValue().getAsJsonObject();
				model.display.put(entry.getKey(), new RetroModel.Transform(
					floats(t, "rotation", 3), floats(t, "translation", 3), floats(t, "scale", 3, 1.0f)));
			}
		}
	}

	private static RetroModel.Element parseElement(JsonObject json) {
		float[] from = floatArray(json.getAsJsonArray("from"));
		float[] to = floatArray(json.getAsJsonArray("to"));
		boolean shade = !json.has("shade") || json.get("shade").getAsBoolean();

		RetroModel.Rotation rotation = null;
		if (json.has("rotation")) {
			JsonObject r = json.getAsJsonObject("rotation");
			rotation = new RetroModel.Rotation(
				floatArray(r.getAsJsonArray("origin")),
				r.get("axis").getAsString().charAt(0),
				r.get("angle").getAsFloat(),
				r.has("rescale") && r.get("rescale").getAsBoolean());
		}

		RetroModel.Element element = new RetroModel.Element(from, to, shade, rotation);
		JsonObject faces = json.getAsJsonObject("faces");
		if (faces != null) {
			for (int face = 0; face < 6; face++) {
				JsonObject f = faces.getAsJsonObject(FACE_NAMES[face]);
				if (f == null) {
					continue;
				}
				float[] uv = f.has("uv") ? floatArray(f.getAsJsonArray("uv")) : deriveUv(face, from, to);
				String texture = f.get("texture").getAsString();
				if (texture.startsWith("#")) {
					texture = texture.substring(1);
				}
				int cullface = f.has("cullface") ? faceIndex(f.get("cullface").getAsString()) : -1;
				int uvRotation = f.has("rotation") ? f.get("rotation").getAsInt() : 0;
				int tint = f.has("tintindex") ? f.get("tintindex").getAsInt() : -1;
				element.faces[face] = new RetroModel.Face(uv, texture, cullface, uvRotation, tint);
			}
		}
		return element;
	}

	/**
	 * Vanilla's default UVs: the element bounds projected onto the face plane. The EAST
	 * face needs its u flipped relative to west: its corner winding (FACE_CORNERS) runs z
	 * the opposite way, so the plain {from[2]..to[2]} that is world-aligned on west comes
	 * out MIRRORED on east (u = max - worldZ instead of worldZ). Flipping u here makes a
	 * non-rotated east face read world-aligned, the same as the rotated faces relockUvs
	 * produces, so the two agree. (This showed up as the wall's +x side looking offset.)
	 */
	private static float[] deriveUv(int face, float[] from, float[] to) {
		switch (face) {
			case 0: case 1: return new float[]{from[0], from[2], to[0], to[2]};
			case 2: case 3: return new float[]{from[0], 16 - to[1], to[0], 16 - from[1]};
			default: return new float[]{from[2], 16 - to[1], to[2], 16 - from[1]};
		}
	}

	public static int faceIndex(String name) {
		switch (name) {
			case "down": case "bottom": return 0;
			case "up": case "top": return 1;
			case "north": return 2;
			case "south": return 3;
			case "west": return 4;
			case "east": return 5;
			default: return -1;
		}
	}

	private static float[] floatArray(JsonArray array) {
		float[] result = new float[array.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = array.get(i).getAsFloat();
		}
		return result;
	}

	private static float[] floats(JsonObject json, String key, int size) {
		return floats(json, key, size, 0.0f);
	}

	private static float[] floats(JsonObject json, String key, int size, float fallback) {
		float[] result = new float[size];
		java.util.Arrays.fill(result, fallback);
		if (json.has(key)) {
			JsonArray array = json.getAsJsonArray(key);
			for (int i = 0; i < Math.min(size, array.size()); i++) {
				result[i] = array.get(i).getAsFloat();
			}
		}
		return result;
	}

	private static JsonObject readJson(String reference) {
		int colon = reference.indexOf(':');
		String ns = colon >= 0 ? reference.substring(0, colon) : "minecraft";
		String path = colon >= 0 ? reference.substring(colon + 1) : reference;

		// Embedded standard parents (looked up namespace-less or as minecraft:).
		String embedded = EMBEDDED_PARENTS.get(path);
		if (embedded != null && (colon < 0 || "minecraft".equals(ns))) {
			return JsonParser.parseString(embedded).getAsJsonObject();
		}

		String[] candidates = {
			"/assets/" + ns + "/retroapi/models/" + path + ".json",
			"/assets/" + ns + "/models/" + path + ".json",
			"/assets/" + ns + "/stationapi/models/" + path + ".json",
		};
		for (String candidate : candidates) {
			try (InputStream is = RetroModelLoader.class.getResourceAsStream(candidate)) {
				if (is != null) {
					return JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
				}
			} catch (Exception e) {
				RetroAPI.LOGGER.error("Failed to read model {}: {}", candidate, e.toString());
				return null;
			}
		}
		return null;
	}

	/** All face indices in down/up/north/south/west/east order, shared with the renderer. */
	public static final List<String> FACES = java.util.Collections.unmodifiableList(
		new ArrayList<>(java.util.Arrays.asList(FACE_NAMES)));
}
