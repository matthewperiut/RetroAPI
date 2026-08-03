package com.periut.retroapi.world.noise;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Makes independent {@link RetroXoroshiro}s addressed by <em>name</em> or by <em>position</em>, the way
 * modern worldgen's {@code PositionalRandomFactory} does.
 *
 * <p>Naming is the important half. A density-function generator has a dozen or more noises in flight at
 * once, and every one of them has to be statistically unrelated to the others or the caves they combine
 * into acquire structure nobody asked for (tunnels that only ever appear inside cheese caverns, pillars
 * that line up with spaghetti). Drawing them in sequence from one generator does not give that. Hashing
 * the noise's <em>name</em> into the seed does, and it has the bonus property that adding a new noise
 * later does not shift every existing one - the world you already generated stays the world you had.
 */
public final class RetroPositionalRandom {

	private final long seedLo;
	private final long seedHi;

	RetroPositionalRandom(long seedLo, long seedHi) {
		this.seedLo = seedLo;
		this.seedHi = seedHi;
	}

	/**
	 * The generator for a named fork, e.g. {@code "cavebiomes:cave_cheese"} or {@code "octave_-8"}.
	 * The name is hashed with MD5-128 and xored into this factory's state, so the same name under the
	 * same world seed always gives the same stream.
	 */
	public RetroXoroshiro fromHashOf(String name) {
		byte[] hash = md5(name);
		long hashLo = longFromBytes(hash, 0);
		long hashHi = longFromBytes(hash, 8);
		return new RetroXoroshiro(hashLo ^ this.seedLo, hashHi ^ this.seedHi);
	}

	/** The generator for a raw seed value. */
	public RetroXoroshiro fromSeed(long seed) {
		return new RetroXoroshiro(seed ^ this.seedLo, seed ^ this.seedHi);
	}

	/**
	 * The generator for a block position - the per-position source features use so a decoration is
	 * reproducible from where it stands rather than from how many chunks were generated before it.
	 */
	public RetroXoroshiro at(int x, int y, int z) {
		long positionalSeed = positionSeed(x, y, z);
		return new RetroXoroshiro(positionalSeed ^ this.seedLo, this.seedHi);
	}

	/** Vanilla's {@code Mth.getSeed} position scramble. */
	public static long positionSeed(int x, int y, int z) {
		long seed = x * 3129871L ^ z * 116129781L ^ y;
		seed = seed * seed * 42317861L + seed * 11L;
		return seed >> 16;
	}

	private static long longFromBytes(byte[] bytes, int offset) {
		long value = 0L;
		for (int i = 0; i < 8; i++) {
			value = value << 8 | (bytes[offset + i] & 0xFFL);
		}
		return value;
	}

	private static byte[] md5(String input) {
		try {
			// A fresh digest per call: MessageDigest is not thread safe and chunk generation can run off
			// the main thread under some launchers. Forks happen once per noise at construction, not per
			// sample, so the allocation is not on any hot path.
			return MessageDigest.getInstance("MD5").digest(input.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("MD5 is required for positional random seeding", e);
		}
	}
}
