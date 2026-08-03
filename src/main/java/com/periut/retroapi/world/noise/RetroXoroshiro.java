package com.periut.retroapi.world.noise;

/**
 * Xoroshiro128++, the random source modern Minecraft worldgen is seeded from.
 *
 * <p>Beta's {@code java.util.Random} is a 48-bit LCG. That matters here for a reason that is not
 * "newer is better": the modern noise stack does not seed one generator and draw from it in order, it
 * <em>forks</em> a generator per named noise and per position ({@link #forkPositional}), and an LCG
 * forked that way produces visibly correlated streams - neighbouring positions give neighbouring
 * values, and the octaves of one noise end up related to each other. Xoroshiro's forking (a 128-bit
 * state xored with an MD5 hash of the fork's name) is what makes a dozen independently-named noises
 * actually independent, which is the whole premise of a density-function generator.
 *
 * <p>Bit-for-bit equivalent to vanilla's {@code XoroshiroRandomSource}, so a noise built on it has the
 * same statistics as the one it was transcribed from.
 */
public final class RetroXoroshiro {

	private static final long GOLDEN_RATIO_64 = -7046029254386353131L;
	private static final long SILVER_RATIO_64 = 7640891576956012809L;

	/** Vanilla's constants are a float literal stored in a double/float field; keep the exact widths. */
	private static final double DOUBLE_UNIT = 1.110223E-16F;
	private static final float FLOAT_UNIT = 5.9604645E-8F;

	private long seedLo;
	private long seedHi;

	/** From a single legacy seed (a world seed), upgraded to 128 bits the way vanilla does. */
	public RetroXoroshiro(long seed) {
		long lo = seed ^ SILVER_RATIO_64;
		long hi = lo + GOLDEN_RATIO_64;
		this.seedLo = mixStafford13(lo);
		this.seedHi = mixStafford13(hi);
		guardAgainstZero();
	}

	public RetroXoroshiro(long seedLo, long seedHi) {
		this.seedLo = seedLo;
		this.seedHi = seedHi;
		guardAgainstZero();
	}

	private void guardAgainstZero() {
		// An all-zero state is a fixed point of the recurrence: it would emit zero forever.
		if ((this.seedLo | this.seedHi) == 0L) {
			this.seedLo = GOLDEN_RATIO_64;
			this.seedHi = SILVER_RATIO_64;
		}
	}

	static long mixStafford13(long z) {
		z = (z ^ z >>> 30) * -4658895280553007687L;
		z = (z ^ z >>> 27) * -7723592293110705685L;
		return z ^ z >>> 31;
	}

	public long nextLong() {
		long s0 = this.seedLo;
		long s1 = this.seedHi;
		long result = Long.rotateLeft(s0 + s1, 17) + s0;
		s1 ^= s0;
		this.seedLo = Long.rotateLeft(s0, 49) ^ s1 ^ s1 << 21;
		this.seedHi = Long.rotateLeft(s1, 28);
		return result;
	}

	public int nextInt() {
		return (int) nextLong();
	}

	/** Uniform in {@code [0, bound)}, with vanilla's unbiased-bucket rejection. */
	public int nextInt(int bound) {
		if (bound <= 0) {
			throw new IllegalArgumentException("Bound must be positive");
		}
		long randomBits = Integer.toUnsignedLong(nextInt());
		long multiplied = randomBits * bound;
		long fractional = multiplied & 0xFFFFFFFFL;
		if (fractional < bound) {
			int unbiasedStart = Integer.remainderUnsigned(~bound + 1, bound);
			while (fractional < unbiasedStart) {
				randomBits = Integer.toUnsignedLong(nextInt());
				multiplied = randomBits * bound;
				fractional = multiplied & 0xFFFFFFFFL;
			}
		}
		return (int) (multiplied >> 32);
	}

	/** Uniform in {@code [min, max]}, both inclusive. */
	public int nextInt(int min, int max) {
		return min >= max ? min : nextInt(max - min + 1) + min;
	}

	public boolean nextBoolean() {
		return (nextLong() & 1L) != 0L;
	}

	public float nextFloat() {
		return (float) nextBits(24) * FLOAT_UNIT;
	}

	public double nextDouble() {
		return nextBits(53) * DOUBLE_UNIT;
	}

	/** Burns {@code rounds} outputs. Needed to keep octave streams aligned when an amplitude is zero. */
	public void consumeCount(int rounds) {
		for (int i = 0; i < rounds; i++) {
			nextLong();
		}
	}

	private long nextBits(int bits) {
		return nextLong() >>> 64 - bits;
	}

	/** An independent generator drawn from this one. */
	public RetroXoroshiro fork() {
		return new RetroXoroshiro(nextLong(), nextLong());
	}

	/**
	 * A factory for generators addressed by name or by position, drawn from this one. This is the seam
	 * that keeps separately-named noises independent - see the class doc.
	 */
	public RetroPositionalRandom forkPositional() {
		return new RetroPositionalRandom(nextLong(), nextLong());
	}
}
