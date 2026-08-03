package com.periut.retroapi.world.noise;

/**
 * One octave of Perlin's improved noise: a 256-entry permutation, a 16-entry gradient table, and
 * trilinear interpolation through a quintic fade.
 *
 * <p>This is the same algorithm beta's {@code PerlinNoiseSampler} implements, and it is here anyway,
 * because the thing that makes modern noise <em>look</em> modern is not the single octave - it is how
 * octaves are weighted and combined ({@link RetroPerlinNoise}) and how they are seeded
 * ({@link RetroPositionalRandom}). Reusing beta's sampler would mean matching beta's seeding, which is
 * exactly the part that has to change. Owning the octave keeps the whole stack consistent and lets it
 * be built from any {@link RetroXoroshiro}.
 */
public final class RetroImprovedNoise {

	/**
	 * The 16 gradient vectors. Note it is not 12 distinct directions padded to 16 - entries 12-15
	 * duplicate earlier ones, which is Perlin's original table and a source of very slight directional
	 * bias that every Minecraft world ever generated has. Reproduce it exactly; "fixing" it changes
	 * every shape.
	 */
	private static final int[][] GRADIENT = {
		{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
		{1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
		{0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
		{1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}
	};

	private final byte[] p = new byte[256];
	public final double xo;
	public final double yo;
	public final double zo;

	public RetroImprovedNoise(RetroXoroshiro random) {
		this.xo = random.nextDouble() * 256.0;
		this.yo = random.nextDouble() * 256.0;
		this.zo = random.nextDouble() * 256.0;

		for (int i = 0; i < 256; i++) {
			this.p[i] = (byte) i;
		}
		// Fisher-Yates over the shrinking tail, matching vanilla's draw count exactly: the number of
		// nextInt calls here is what an octave "costs" a random stream, and RetroPerlinNoise relies on
		// that cost being stable when it skips a zero-amplitude octave.
		for (int i = 0; i < 256; i++) {
			int offset = random.nextInt(256 - i);
			byte tmp = this.p[i];
			this.p[i] = this.p[i + offset];
			this.p[i + offset] = tmp;
		}
	}

	public double noise(double x, double y, double z) {
		double px = x + this.xo;
		double py = y + this.yo;
		double pz = z + this.zo;
		int xf = RetroDensity.floor(px);
		int yf = RetroDensity.floor(py);
		int zf = RetroDensity.floor(pz);
		double xr = px - xf;
		double yr = py - yf;
		double zr = pz - zf;
		return sampleAndLerp(xf, yf, zf, xr, yr, zr);
	}

	private int p(int index) {
		return this.p[index & 0xFF] & 0xFF;
	}

	private static double gradDot(int hash, double x, double y, double z) {
		int[] g = GRADIENT[hash & 15];
		return g[0] * x + g[1] * y + g[2] * z;
	}

	private double sampleAndLerp(int x, int y, int z, double xr, double yr, double zr) {
		int x0 = p(x);
		int x1 = p(x + 1);
		int xy00 = p(x0 + y);
		int xy01 = p(x0 + y + 1);
		int xy10 = p(x1 + y);
		int xy11 = p(x1 + y + 1);
		double d000 = gradDot(p(xy00 + z), xr, yr, zr);
		double d100 = gradDot(p(xy10 + z), xr - 1.0, yr, zr);
		double d010 = gradDot(p(xy01 + z), xr, yr - 1.0, zr);
		double d110 = gradDot(p(xy11 + z), xr - 1.0, yr - 1.0, zr);
		double d001 = gradDot(p(xy00 + z + 1), xr, yr, zr - 1.0);
		double d101 = gradDot(p(xy10 + z + 1), xr - 1.0, yr, zr - 1.0);
		double d011 = gradDot(p(xy01 + z + 1), xr, yr - 1.0, zr - 1.0);
		double d111 = gradDot(p(xy11 + z + 1), xr - 1.0, yr - 1.0, zr - 1.0);
		double xAlpha = RetroDensity.smoothstep(xr);
		double yAlpha = RetroDensity.smoothstep(yr);
		double zAlpha = RetroDensity.smoothstep(zr);
		return RetroDensity.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
	}
}
