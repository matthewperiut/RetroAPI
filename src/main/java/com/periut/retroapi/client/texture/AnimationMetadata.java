package com.periut.retroapi.client.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The parsed {@code animation} section of a {@code .png.mcmeta} sidecar, modern schema:
 *
 * <pre>
 * { "animation": { "frametime": 2, "interpolate": true,
 *                  "frames": [0, 1, {"index": 2, "time": 8}, 1] } }
 * </pre>
 *
 * <ul>
 *   <li>{@code frametime} (default 1): default ticks per frame.</li>
 *   <li>{@code frames} (optional): ints or {@code {index, time}} objects. Omitted means
 *       all frames in PNG order at {@code frametime}.</li>
 *   <li>{@code interpolate} (default false): per-pixel lerp toward the next frame.</li>
 * </ul>
 *
 * <p>{@code width}/{@code height} (non-square frames) are unsupported: warned once and
 * ignored. Frames are square, stacked vertically (height = frameCount * width).</p>
 */
public final class AnimationMetadata {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/Animation");
	private static boolean warnedNonSquare = false;

	public final int frametime;
	public final boolean interpolate;
	/** Frame indices in play order, or null for "all frames in PNG order". */
	public final int[] frameOrder;
	/** Per-played-frame times (parallel to frameOrder), or null when frameOrder is null. */
	public final int[] frameTimes;

	private AnimationMetadata(int frametime, boolean interpolate, int[] frameOrder, int[] frameTimes) {
		this.frametime = frametime;
		this.interpolate = interpolate;
		this.frameOrder = frameOrder;
		this.frameTimes = frameTimes;
	}

	/** A default animation (all frames, uniform time), used for code-driven registration. */
	public static AnimationMetadata uniform(int ticksPerFrame) {
		return new AnimationMetadata(Math.max(1, ticksPerFrame), false, null, null);
	}

	/** Parses a .png.mcmeta stream; returns null when there is no {@code animation} section. */
	public static AnimationMetadata parse(InputStream in) {
		try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonObject anim = root.getAsJsonObject("animation");
			if (anim == null) {
				return null;
			}
			if ((anim.has("width") || anim.has("height")) && !warnedNonSquare) {
				warnedNonSquare = true;
				LOGGER.warn("mcmeta 'width'/'height' (non-square frames) are unsupported; ignoring");
			}
			int frametime = anim.has("frametime") ? anim.get("frametime").getAsInt() : 1;
			boolean interpolate = anim.has("interpolate") && anim.get("interpolate").getAsBoolean();

			int[] order = null;
			int[] times = null;
			if (anim.has("frames")) {
				JsonArray frames = anim.getAsJsonArray("frames");
				List<int[]> parsed = new ArrayList<>();
				for (JsonElement element : frames) {
					if (element.isJsonObject()) {
						JsonObject obj = element.getAsJsonObject();
						int index = obj.get("index").getAsInt();
						int time = obj.has("time") ? obj.get("time").getAsInt() : frametime;
						parsed.add(new int[]{index, time});
					} else {
						parsed.add(new int[]{element.getAsInt(), frametime});
					}
				}
				order = new int[parsed.size()];
				times = new int[parsed.size()];
				for (int i = 0; i < parsed.size(); i++) {
					order[i] = parsed.get(i)[0];
					times[i] = Math.max(1, parsed.get(i)[1]);
				}
			}
			return new AnimationMetadata(Math.max(1, frametime), interpolate, order, times);
		} catch (Exception e) {
			LOGGER.error("Bad .mcmeta animation section: {}", e.toString());
			return null;
		}
	}
}
