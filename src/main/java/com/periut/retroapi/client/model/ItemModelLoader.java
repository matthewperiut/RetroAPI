package com.periut.retroapi.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.register.block.RetroTexture;
import com.periut.retroapi.register.block.RetroTextures;
import net.minecraft.item.Item;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item model support, checked automatically at {@code RetroItemAccess.register(id)}:
 *
 * <ul>
 *   <li>{@code parent: item/generated} / {@code item/handheld} layer models: layer0 becomes
 *       the item's sprite; additional layers composite onto the same atlas slot at atlas
 *       build time (sprite-level flattening, so per-layer tint is unsupported and warned).
 *       The item then renders through the completely unchanged vanilla sprite path.</li>
 *   <li>Block-parent models: covered for BLOCK items automatically (the block's baked model
 *       renders in inventory/hand through the model render type). A PLAIN item pointing at
 *       a block model logs a warning in v1.</li>
 * </ul>
 *
 * <p>When a model JSON exists it overrides a code-side {@code .texture(...)} call.</p>
 */
public final class ItemModelLoader {

	/** Overlay layers (layer1+) by base texture, composited by AtlasExpander. */
	private static final Map<RetroTexture, List<NamespacedIdentifier>> OVERLAYS = new HashMap<>();

	/**
	 * For a derived item slot whose BASE layer is a vanilla texture, the vanilla atlas
	 * slot to copy in as the base. We cannot point such an item straight at the shared
	 * vanilla slot, overlaying onto it would corrupt the real vanilla item, so the item
	 * gets its own slot, the vanilla base is copied here, and overlays go on top.
	 */
	private static final Map<RetroTexture, Integer> BASE_FROM_VANILLA = new HashMap<>();

	private ItemModelLoader() {}

	public static List<NamespacedIdentifier> overlaysFor(RetroTexture texture) {
		return OVERLAYS.get(texture);
	}

	/**
	 * Code path for a layered item with NO model JSON: {@code base} is layer 0, each of {@code overlays}
	 * stacks on top, composited onto one atlas slot at build time (exactly like a {@code item/generated}
	 * model with {@code layer0}/{@code layer1}...). Base and overlays may each be a modded texture
	 * ({@code ns:name} → {@code textures/item/name.png}) or a vanilla one ({@code minecraft:apple}). This
	 * is what {@code RetroItemAccess.layers(...)} calls, so a mod can stack sprites without shipping JSON.
	 */
	public static RetroTexture applyLayers(Item item, NamespacedIdentifier base, List<NamespacedIdentifier> overlays) {
		List<String> layerValues = new ArrayList<>();
		layerValues.add(base.namespace() + ":" + base.identifier());
		for (NamespacedIdentifier o : overlays) {
			layerValues.add(o.namespace() + ":" + o.identifier());
		}
		return buildLayers(item, base, layerValues);
	}

	/** Adds one overlay on top of an existing base item texture (backs {@code RetroItemAccess.overlay(...)}). */
	public static void addOverlay(RetroTexture base, NamespacedIdentifier overlay) {
		OVERLAYS.computeIfAbsent(base, k -> new ArrayList<>()).add(overlay);
	}

	/** The vanilla atlas slot a derived item should copy as its base, or null. */
	public static Integer vanillaBaseSlot(RetroTexture texture) {
		return BASE_FROM_VANILLA.get(texture);
	}

	/** Public bridge to the vanilla item sprite table (for the atlas compositor). */
	public static Integer vanillaSprite(String name) {
		return VanillaItemTextureNames.itemSprite(name);
	}

	/** The vanilla atlas slot a texture VALUE ("minecraft:item/apple") resolves to, or null. */
	private static Integer vanillaSpriteOf(String value) {
		int colon = value.indexOf(':');
		String ns = colon >= 0 ? value.substring(0, colon) : "minecraft";
		String path = colon >= 0 ? value.substring(colon + 1) : value;
		return "minecraft".equals(ns) ? VanillaItemTextureNames.itemSprite(path) : null;
	}

	/**
	 * Turns an ordered list of layer texture VALUES into the item's sprite: layer 0 is the
	 * base, the rest are overlays composited on top at atlas-build time. When the base is a
	 * vanilla texture AND there are overlays, the item gets its own slot copied from the
	 * vanilla base (so the real vanilla item is never touched).
	 */
	private static boolean finalizeLayers(Item item, NamespacedIdentifier id, List<String> layerValues) {
		return buildLayers(item, id, layerValues) != null;
	}

	/** As {@link #finalizeLayers}, but returns the base RetroTexture (or null if there were no layers). */
	private static RetroTexture buildLayers(Item item, NamespacedIdentifier id, List<String> layerValues) {
		if (layerValues.isEmpty()) {
			return null;
		}
		String baseValue = layerValues.get(0);
		List<NamespacedIdentifier> overlays = new ArrayList<>();
		for (int i = 1; i < layerValues.size(); i++) {
			overlays.add(itemTextureId(layerValues.get(i)));
		}

		RetroTexture base;
		Integer vanillaBase = vanillaSpriteOf(baseValue);
		if (vanillaBase != null && !overlays.isEmpty()) {
			base = RetroTextures.addItemTexture(id); // a fresh slot, just for this item
			BASE_FROM_VANILLA.put(base, vanillaBase);
		} else {
			base = registerItemTexture(baseValue);
		}
		if (!overlays.isEmpty()) {
			OVERLAYS.put(base, overlays);
		}
		item.setTextureId(base.id);
		RetroTextures.trackItem(item, base);
		return base;
	}

	/** Applies the item's model JSON if one exists. Returns true when it set the sprite. */
	public static boolean tryApply(Item item, NamespacedIdentifier id) {
		// Modern (26.1.2-era) ITEM MODEL DEFINITIONS first: assets/{ns}/items/{id}.json
		// holds a definition object whose model references point into models/item. The
		// older direct models/item/{id}.json path below stays for 1.8/StationAPI-era packs.
		JsonObject definition = readDefinitionJson(id.namespace(), id.identifier());
		if (definition != null && definition.has("model")) {
			List<String> refs = new ArrayList<>();
			collectModelRefs(definition.getAsJsonObject("model"), refs);
			if (!refs.isEmpty()) {
				return applyModelRefs(item, id, refs);
			}
		}

		JsonObject json = readJson(id.namespace(), id.identifier());
		if (json == null) {
			return false;
		}
		return applyModelJson(item, id, json);
	}

	/** The 26.1.2 definition types: minecraft:model (one ref) and minecraft:composite (layers). */
	private static void collectModelRefs(JsonObject model, List<String> refs) {
		String type = model.has("type") ? model.get("type").getAsString() : "minecraft:model";
		if (type.endsWith(":model") || type.equals("model")) {
			if (model.has("model")) {
				refs.add(model.get("model").getAsString());
			}
		} else if (type.endsWith(":composite") || type.equals("composite")) {
			for (com.google.gson.JsonElement el : model.getAsJsonArray("models")) {
				collectModelRefs(el.getAsJsonObject(), refs);
			}
		} else {
			RetroAPI.LOGGER.warn("Item model definition type {} unsupported; using any nested models", type);
			if (model.has("model") && model.get("model").isJsonObject()) {
				collectModelRefs(model.getAsJsonObject("model"), refs);
			}
		}
	}

	/** Resolves definition refs: the first model's layer0 is the base, the rest stack as overlays. */
	private static boolean applyModelRefs(Item item, NamespacedIdentifier id, List<String> refs) {
		List<String> layerValues = new ArrayList<>();
		for (String ref : refs) {
			int colon = ref.indexOf(':');
			String ns = colon >= 0 ? ref.substring(0, colon) : "minecraft";
			String path = colon >= 0 ? ref.substring(colon + 1) : ref;
			String name = path.startsWith("item/") ? path.substring("item/".length())
				: path.startsWith("items/") ? path.substring("items/".length()) : path;
			JsonObject json = readJson(ns, name);
			if (json == null) {
				RetroAPI.LOGGER.warn("Item definition for {} references missing model {}", id, ref);
				continue;
			}
			String parent = json.has("parent") ? json.get("parent").getAsString() : "";
			if (parent.endsWith("item/handheld")) {
				((com.periut.retroapi.register.item.RetroItemAccess) item).handheld();
			}
			JsonObject textures = json.getAsJsonObject("textures");
			if (textures == null) {
				continue;
			}
			for (int layer = 0; textures.has("layer" + layer); layer++) {
				layerValues.add(textures.get("layer" + layer).getAsString());
			}
		}
		return finalizeLayers(item, id, layerValues);
	}

	private static boolean applyModelJson(Item item, NamespacedIdentifier id, JsonObject json) {
		String parent = json.has("parent") ? json.get("parent").getAsString() : null;
		if (parent == null) {
			return false;
		}
		String parentPath = parent.indexOf(':') >= 0 ? parent.substring(parent.indexOf(':') + 1) : parent;
		// builtin/generated is the pre-1.9 spelling StationAPI-era packs carry.
		if (parentPath.equals("builtin/generated")) {
			parentPath = "item/generated";
		}

		if (parentPath.equals("item/generated") || parentPath.equals("item/handheld")) {
			// item/handheld means more than a texture source: it is the modern way of
			// saying "hold me like a tool", so the in-hand diagonal pose comes with it.
			if (parentPath.equals("item/handheld")) {
				((com.periut.retroapi.register.item.RetroItemAccess) item).handheld();
			}
			JsonObject textures = json.getAsJsonObject("textures");
			if (textures == null || !textures.has("layer0")) {
				RetroAPI.LOGGER.warn("Item model for {} has no layer0 texture", id);
				return false;
			}
			List<String> layerValues = new ArrayList<>();
			for (int layer = 0; textures.has("layer" + layer); layer++) {
				layerValues.add(textures.get("layer" + layer).getAsString());
			}
			return finalizeLayers(item, id, layerValues);
		}

		if (parentPath.startsWith("block/") || parent.contains(":block/")) {
			// Block items get this for free (the block's model renders in inventory through
			// the model render type); a plain item would need its own hook.
			if (!(item instanceof net.minecraft.item.BlockItem)) {
				RetroAPI.LOGGER.warn("Item model for {} has block parent {}; unsupported for non-block items in v1", id, parent);
			}
			return false;
		}

		RetroAPI.LOGGER.warn("Item model for {} has unsupported parent {}", id, parent);
		return false;
	}

	private static RetroTexture registerItemTexture(String value) {
		NamespacedIdentifier id = itemTextureId(value);
		// Vanilla references ("minecraft:item/apple") resolve straight to the vanilla
		// items.png sprite by name, exactly like block textures do for terrain.png.
		if ("minecraft".equals(id.namespace())) {
			Integer sprite = VanillaItemTextureNames.itemSprite(id.identifier());
			if (sprite != null) {
				return RetroTextures.vanillaTexture(id, sprite);
			}
		}
		return RetroTextures.addItemTexture(id);
	}

	private static NamespacedIdentifier itemTextureId(String value) {
		int colon = value.indexOf(':');
		String ns = colon >= 0 ? value.substring(0, colon) : "minecraft";
		String path = colon >= 0 ? value.substring(colon + 1) : value;
		if (path.startsWith("item/")) {
			path = path.substring("item/".length());
		} else if (path.startsWith("items/")) {
			path = path.substring("items/".length()); // pre-flattening plural
		}
		return NamespacedIdentifiers.from(ns, path);
	}

	/** The modern definition file: assets/{ns}/items/{id}.json. */
	private static JsonObject readDefinitionJson(String ns, String name) {
		String candidate = "/assets/" + ns + "/items/" + name + ".json";
		try (InputStream is = ItemModelLoader.class.getResourceAsStream(candidate)) {
			if (is != null) {
				return JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
			}
		} catch (Exception e) {
			RetroAPI.LOGGER.error("Failed to read item definition {}: {}", candidate, e.toString());
		}
		return null;
	}

	private static JsonObject readJson(String ns, String name) {
		String[] candidates = {
			"/assets/" + ns + "/retroapi/models/item/" + name + ".json",
			"/assets/" + ns + "/models/item/" + name + ".json",
			"/assets/" + ns + "/stationapi/models/item/" + name + ".json",
		};
		for (String candidate : candidates) {
			try (InputStream is = ItemModelLoader.class.getResourceAsStream(candidate)) {
				if (is != null) {
					return JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
				}
			} catch (Exception e) {
				RetroAPI.LOGGER.error("Failed to read item model {}: {}", candidate, e.toString());
				return null;
			}
		}
		return null;
	}
}
