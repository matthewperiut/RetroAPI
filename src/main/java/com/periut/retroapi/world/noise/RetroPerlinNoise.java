package com.periut.retroapi.world.noise;

/**
 * Octaves of {@link RetroImprovedNoise} summed with explicit per-octave amplitudes - modern Minecraft's
 * {@code PerlinNoise}.
 *
 * <p>The difference from beta's {@code OctavePerlinNoiseSampler} is the whole point. Beta takes an octave
 * <em>count</em> and halves the amplitude each step, which produces one shape of noise: a fixed pink
 * spectrum. Modern worldgen takes a {@code firstOctave} (which frequency the lowest octave sits at) and
 * an amplitude <em>per octave</em>, so a noise can be deliberately lumpy at one scale and smooth at
 * another - {@code cave_cheese} is {@code [0.5, 1, 2, 1, 2, 1, 0, 2, 0]}, with two octaves silenced
 * entirely. That spectrum is what makes a cheese cavern read as a cavern instead of as fog.
 *
 * <p>Octave {@code i} samples at frequency {@code 2^(firstOctave + i)}; a zero amplitude leaves its
 * octave unbuilt.
 */
public final class RetroPerlinNoise {

	private static final double ROUND_OFF = 3.3554432E7;

	private final RetroImprovedNoise[] noiseLevels;
	private final double[] amplitudes;
	private final int firstOctave;
	private final double lowestFreqInputFactor;
	private final double lowestFreqValueFactor;

	/**
	 * @param random      the stream to build octaves from; forked per octave by name, so which octaves
	 *                    are silenced does not shift the others
	 * @param firstOctave the (usually negative) power of two the lowest octave samples at
	 * @param amplitudes  one weight per octave, low frequency first; zeros are allowed
	 */
	public RetroPerlinNoise(RetroXoroshiro random, int firstOctave, double... amplitudes) {
		if (amplitudes.length == 0) {
			throw new IllegalArgumentException("Need some octaves");
		}
		this.firstOctave = firstOctave;
		this.amplitudes = amplitudes.clone();
		int octaves = amplitudes.length;
		this.noiseLevels = new RetroImprovedNoise[octaves];

		RetroPositionalRandom positional = random.forkPositional();
		for (int i = 0; i < octaves; i++) {
			if (amplitudes[i] != 0.0) {
				this.noiseLevels[i] = new RetroImprovedNoise(positional.fromHashOf("octave_" + (firstOctave + i)));
			}
		}

		this.lowestFreqInputFactor = Math.pow(2.0, firstOctave);
		// Normalises the sum so that adding octaves does not grow the range without bound.
		this.lowestFreqValueFactor = Math.pow(2.0, octaves - 1) / (Math.pow(2.0, octaves) - 1.0);
	}

	public double getValue(double x, double y, double z) {
		double value = 0.0;
		double inputFactor = this.lowestFreqInputFactor;
		double valueFactor = this.lowestFreqValueFactor;

		for (int i = 0; i < this.noiseLevels.length; i++) {
			RetroImprovedNoise noise = this.noiseLevels[i];
			if (noise != null) {
				value += this.amplitudes[i] * valueFactor
					* noise.noise(wrap(x * inputFactor), wrap(y * inputFactor), wrap(z * inputFactor));
			}
			inputFactor *= 2.0;
			valueFactor /= 2.0;
		}
		return value;
	}

	public int firstOctave() {
		return this.firstOctave;
	}

	/**
	 * Folds a coordinate back into a range where {@code double} still has fractional precision. Without
	 * it, noise degenerates into flat bands out at extreme coordinates - the classic far-lands artifact.
	 */
	public static double wrap(double x) {
		return x - RetroDensity.lfloor(x / ROUND_OFF + 0.5) * ROUND_OFF;
	}
}
