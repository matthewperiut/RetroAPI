package com.periut.retrotweaks.feature.options;

/**
 * Pure arithmetic for the Graphics tab's render scale row and resample-filter cycle button: no
 * Minecraft/LWJGL/RetroDragon types anywhere in this file, on purpose, so every formula here can be
 * checked by the {@code main} below instead of trusted on faith - run it with {@code javac} + {@code
 * java -ea} (assertions are off by default in plain {@code java}). Same convention as {@code
 * feature.hud.HudEditorMath}.
 *
 * <p>This file never calls RetroDragon and never clamps against anything RetroDragon has not already
 * told the caller (min/max come from {@code RetroSettings}' own documented {@code [0.1, 4.0]},
 * duplicated here only so the button math has something to test against without a running game).
 */
public final class RenderScaleMath {

	private RenderScaleMath() {}

	/** Mirrors {@code RetroSettings.setRenderScale}'s documented clamp range. */
	public static final float MIN_SCALE = 0.1F;
	public static final float MAX_SCALE = 4.0F;

	/**
	 * The scale after moving by {@code delta} and clamping into {@code [MIN_SCALE, MAX_SCALE]},
	 * rounded to the nearest 0.01. The rounding step exists because {@code current + delta} in raw
	 * float arithmetic drifts - repeated {@code -0.05F} taps starting from {@code 1.0F} land on
	 * {@code 0.79999995F} rather than {@code 0.8F} without it - so every step is snapped back onto a
	 * clean hundredth before the next one is applied, rather than letting error accumulate across
	 * taps. The production caller ({@code ConfigScreen}) still sends this result through {@code
	 * ApiBridge.setRenderScale}, whose OWN return value (RetroDragon's clamp) is what actually updates
	 * the on-screen readout - this is only ever a best-effort request, never the value trusted after
	 * the call.
	 */
	public static float step(float current, float delta) {
		float clamped = Math.max(MIN_SCALE, Math.min(MAX_SCALE, current + delta));
		return Math.round(clamped * 100.0F) / 100.0F;
	}

	/**
	 * The index in {@code filters} the cycle button should land on next, or {@code -1} when
	 * {@code filters} is empty (nothing to select at all).
	 *
	 * <p>When {@code current} is not found in {@code filters} - it vanished because the scale crossed
	 * 1.0 and this direction no longer offers it, or nothing has ever been selected - this starts from
	 * the first entry going forward or the last entry going backward, rather than throwing or silently
	 * stalling on a filter that no longer exists.
	 */
	public static int nextFilterIndex(String[] filters, String current, boolean backwards) {
		if (filters.length == 0) return -1;
		int currentIndex = -1;
		for (int i = 0; i < filters.length; i++) {
			if (filters[i].equals(current)) {
				currentIndex = i;
				break;
			}
		}
		if (currentIndex < 0) return backwards ? filters.length - 1 : 0;
		return Math.floorMod(currentIndex + (backwards ? -1 : 1), filters.length);
	}

	public static void main(String[] args) {
		// step(): clean values, no float drift.
		expect(step(1.0F, 0.05F) == 1.05F, "1.0 + 0.05 = 1.05");
		expect(step(1.0F, -0.05F) == 0.95F, "1.0 - 0.05 = 0.95");
		expect(step(1.0F, 0.1F) == 1.1F, "1.0 + 0.1 = 1.1");
		expect(step(1.0F, -0.1F) == 0.9F, "1.0 - 0.1 = 0.9");
		expect(step(0.95F, 0.05F) == 1.0F, "0.95 + 0.05 = 1.0 exactly, got " + step(0.95F, 0.05F));

		float v = 1.0F;
		for (int i = 0; i < 4; i++) v = step(v, -0.05F);
		expect(v == 0.8F, "four -0.05 taps from 1.0 land exactly on 0.8, got " + v);

		v = 1.0F;
		for (int i = 0; i < 3; i++) v = step(v, 0.1F);
		expect(v == 1.3F, "three +0.1 taps from 1.0 land exactly on 1.3, got " + v);

		// clamp at the floor and the ceiling.
		expect(step(0.1F, -0.05F) == 0.1F, "cannot go below 0.1");
		expect(step(0.12F, -0.1F) == 0.1F, "clamps to 0.1 rather than going negative");
		expect(step(4.0F, 0.05F) == 4.0F, "cannot go above 4.0");
		expect(step(3.98F, 0.1F) == 4.0F, "clamps to 4.0 rather than overshooting");

		// nextFilterIndex(): empty and single-entry lists.
		expect(nextFilterIndex(new String[0], "bicubic", false) == -1, "empty list has no next index");
		expect(nextFilterIndex(new String[]{"bilinear"}, "bilinear", false) == 0, "single-entry list stays put forward");
		expect(nextFilterIndex(new String[]{"bilinear"}, "bilinear", true) == 0, "single-entry list stays put backward");

		// normal cycling, both directions, with wraparound.
		String[] three = {"nearest", "bilinear", "bicubic"};
		expect(nextFilterIndex(three, "nearest", false) == 1, "forward from the first entry");
		expect(nextFilterIndex(three, "bicubic", false) == 0, "forward wraps at the end");
		expect(nextFilterIndex(three, "nearest", true) == 2, "backward wraps at the start");
		expect(nextFilterIndex(three, "bilinear", true) == 0, "backward from the middle");

		// the current filter is no longer in the list (e.g. the scale crossed 1.0).
		expect(nextFilterIndex(three, "fsr1", false) == 0, "a vanished current filter lands on the first entry going forward");
		expect(nextFilterIndex(three, "fsr1", true) == 2, "a vanished current filter lands on the last entry going backward");
		expect(nextFilterIndex(three, null, false) == 0, "a null current filter (nothing selected yet) lands on the first entry");

		System.out.println("RenderScaleMath self-check OK");
	}

	private static void expect(boolean condition, String what) {
		if (!condition) throw new AssertionError(what);
	}
}
