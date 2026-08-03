package com.periut.retroapi.world.noise;

/**
 * The noise every modern density function actually samples - vanilla's {@code NormalNoise}: two
 * independent {@link RetroPerlinNoise}es, the second sampled at a slightly irrational scale factor, and
 * the sum normalised to a roughly known deviation.
 *
 * <p>Both halves earn their keep. Summing two Perlins with incommensurable scales
 * ({@code 1.0181268882175227}, chosen so the two lattices never line up again) destroys the axis-aligned
 * grid artifacts a single Perlin has - which matters enormously for caves, because a lone Perlin's
 * lattice makes tunnels prefer to run along the axes. The normalisation is what lets a density function
 * be written with literal thresholds like {@code 0.27} and {@code -0.075} and mean the same thing for
 * every noise regardless of how many octaves it has.
 *
 * <pre>{@code
 * RetroPositionalRandom forks = new RetroXoroshiro(world.getSeed()).forkPositional();
 * RetroNormalNoise cheese = RetroNormalNoise.create(
 *     forks.fromHashOf("cavebiomes:cave_cheese"), -8, 0.5, 1, 2, 1, 2, 1, 0, 2, 0);
 *
 * double d = cheese.getValue(x, y * 0.6666666666666666, z);
 * }</pre>
 */
public final class RetroNormalNoise {

	/** Deliberately not a round number: the two lattices must never realign. */
	private static final double INPUT_FACTOR = 1.0181268882175227;

	private final RetroPerlinNoise first;
	private final RetroPerlinNoise second;
	private final double valueFactor;

	private RetroNormalNoise(RetroXoroshiro random, int firstOctave, double[] amplitudes) {
		this.first = new RetroPerlinNoise(random, firstOctave, amplitudes);
		this.second = new RetroPerlinNoise(random, firstOctave, amplitudes);

		// The expected deviation depends only on how many octaves actually carry weight, so a noise with
		// silenced octaves still lands in the same range as one without.
		int minOctave = Integer.MAX_VALUE;
		int maxOctave = Integer.MIN_VALUE;
		for (int i = 0; i < amplitudes.length; i++) {
			if (amplitudes[i] != 0.0) {
				minOctave = Math.min(minOctave, i);
				maxOctave = Math.max(maxOctave, i);
			}
		}
		if (minOctave > maxOctave) {
			throw new IllegalArgumentException("All amplitudes are zero");
		}
		this.valueFactor = (1.0 / 6.0) / expectedDeviation(maxOctave - minOctave);
	}

	/**
	 * @param random     a stream forked for this noise by name - see {@link RetroPositionalRandom}
	 * @param firstOctave the power of two the lowest octave samples at
	 * @param amplitudes one weight per octave, low frequency first
	 */
	public static RetroNormalNoise create(RetroXoroshiro random, int firstOctave, double... amplitudes) {
		return new RetroNormalNoise(random, firstOctave, amplitudes);
	}

	private static double expectedDeviation(int octaveSpan) {
		return 0.1 * (1.0 + 1.0 / (octaveSpan + 1));
	}

	public double getValue(double x, double y, double z) {
		return (this.first.getValue(x, y, z)
			+ this.second.getValue(x * INPUT_FACTOR, y * INPUT_FACTOR, z * INPUT_FACTOR)) * this.valueFactor;
	}

	/**
	 * Samples with the {@code xz_scale} / {@code y_scale} a density function's {@code noise} node
	 * applies, so a transcribed formula reads the way its JSON did.
	 */
	public double getValue(double x, double y, double z, double xzScale, double yScale) {
		return getValue(x * xzScale, y * yScale, z * xzScale);
	}
}
