package com.periut.retroapi.world.noise;

/**
 * The scalar math the modern noise stack is built out of: the interpolation and remapping helpers that
 * live in {@code Mth}, plus the density-function transforms ({@code squeeze}, {@code quarter_negative},
 * {@code interval_select}, ...) that modern worldgen composes noises with.
 *
 * <p>They are here rather than inlined at each call site because a density function is a <em>formula</em>
 * transcribed from data, and a formula reads correctly only when its pieces are named the same as in the
 * source it came from. {@link #squeeze} is not a general-purpose utility anybody would guess at; it is
 * the exact curve vanilla flattens final density with, and a cave suite that omits it produces different
 * caves.
 *
 * <p>Beta has its own {@code MathHelper}, which has none of these - the modern generator was built on a
 * much larger math surface than beta ever had.
 */
public final class RetroDensity {

	private RetroDensity() {}

	// --- basics ---------------------------------------------------------------------------------

	public static int floor(double v) {
		return (int) Math.floor(v);
	}

	public static long lfloor(double v) {
		return (long) Math.floor(v);
	}

	public static double clamp(double value, double min, double max) {
		return value < min ? min : Math.min(value, max);
	}

	public static int clamp(int value, int min, int max) {
		return value < min ? min : Math.min(value, max);
	}

	public static double lerp(double alpha, double from, double to) {
		return from + alpha * (to - from);
	}

	public static double lerp2(double alphaX, double alphaY, double x00, double x10, double x01, double x11) {
		return lerp(alphaY, lerp(alphaX, x00, x10), lerp(alphaX, x01, x11));
	}

	public static double lerp3(double alphaX, double alphaY, double alphaZ,
			double x000, double x100, double x010, double x110,
			double x001, double x101, double x011, double x111) {
		return lerp(alphaZ,
			lerp2(alphaX, alphaY, x000, x100, x010, x110),
			lerp2(alphaX, alphaY, x001, x101, x011, x111));
	}

	public static double inverseLerp(double value, double min, double max) {
		return (value - min) / (max - min);
	}

	public static double clampedLerp(double alpha, double from, double to) {
		if (alpha < 0.0) {
			return from;
		}
		return alpha > 1.0 ? to : lerp(alpha, from, to);
	}

	/** Remaps a value from one range to another and clamps to the target range. */
	public static double clampedMap(double value, double fromMin, double fromMax, double toMin, double toMax) {
		return clampedLerp(inverseLerp(value, fromMin, fromMax), toMin, toMax);
	}

	/** The fade curve Perlin noise interpolates with: {@code 6t^5 - 15t^4 + 10t^3}. */
	public static double smoothstep(double x) {
		return x * x * x * (x * (x * 6.0 - 15.0) + 10.0);
	}

	// --- density-function transforms ------------------------------------------------------------

	/** {@code minecraft:cube}. */
	public static double cube(double v) {
		return v * v * v;
	}

	/** {@code minecraft:square}. */
	public static double square(double v) {
		return v * v;
	}

	/** {@code minecraft:half_negative}: leaves positives alone, halves negatives. */
	public static double halfNegative(double v) {
		return v > 0.0 ? v : v * 0.5;
	}

	/** {@code minecraft:quarter_negative}: leaves positives alone, quarters negatives. */
	public static double quarterNegative(double v) {
		return v > 0.0 ? v : v * 0.25;
	}

	/**
	 * {@code minecraft:squeeze}. Clamps to [-1, 1] then applies {@code c/2 - c^3/24}, the S-curve modern
	 * worldgen runs final density through so the transition from rock to air is not a step.
	 */
	public static double squeeze(double v) {
		double c = clamp(v, -1.0, 1.0);
		return c / 2.0 - c * c * c / 24.0;
	}

	/**
	 * {@code minecraft:y_clamped_gradient}: {@code fromValue} at or below {@code fromY}, {@code toValue}
	 * at or above {@code toY}, linear between. This is how modern worldgen makes a rule depend on depth.
	 */
	public static double yGradient(double y, double fromY, double toY, double fromValue, double toValue) {
		return clampedMap(y, fromY, toY, fromValue, toValue);
	}

	/**
	 * {@code minecraft:range_choice}: {@code inRange} when {@code input} is in
	 * {@code [min, maxExclusive)}, otherwise {@code outOfRange}.
	 */
	public static double rangeChoice(double input, double min, double maxExclusive, double inRange, double outOfRange) {
		return input >= min && input < maxExclusive ? inRange : outOfRange;
	}

	/**
	 * {@code minecraft:interval_select}: picks a slot by where {@code input} falls among
	 * {@code thresholds}, and returns {@code values[slot]}. {@code values} must be one longer than
	 * {@code thresholds}.
	 *
	 * <p>Vanilla uses this to switch between differently-scaled samplings of the <em>same</em> noise,
	 * which is what gives spaghetti tunnels their varying width along their length.
	 */
	public static double intervalSelect(double input, double[] thresholds, double[] values) {
		for (int i = 0; i < thresholds.length; i++) {
			if (input < thresholds[i]) {
				return values[i];
			}
		}
		return values[thresholds.length];
	}

	/** The slot {@link #intervalSelect} would pick, for callers that want to sample lazily. */
	public static int intervalIndex(double input, double[] thresholds) {
		for (int i = 0; i < thresholds.length; i++) {
			if (input < thresholds[i]) {
				return i;
			}
		}
		return thresholds.length;
	}
}
