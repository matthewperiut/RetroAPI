package com.periut.retroapi.client.render;

import net.minecraft.block.Block;

/**
 * Corrects smooth lighting on blocks that are not a full cube.
 *
 * <h2>The bug</h2>
 * Beta computes ambient occlusion once per face, as four brightness values, one for each corner of the
 * <em>whole block face</em>. It then hands those four values to {@code renderBottomFace} and friends,
 * which draw the quad at the block's current bounding box. For a full cube those are the same four
 * corners, so nothing is wrong and nothing here changes. For anything smaller they are not: a stair's
 * upper step spans half the block, but it is drawn with the corner values belonging to the full face, so
 * the whole light gradient is stretched across half the distance.
 *
 * <p>That is why a beta stair has a seam. Its two boxes are two separate {@code renderBlock} passes, each
 * stretching the same gradient over a different sub-rectangle, so where the step meets the slab the two
 * gradients disagree and the discontinuity is visible straight down the block. Every partial block has
 * some version of it; stairs just show it best, because they put two mismatched pieces side by side.
 *
 * <h2>The fix</h2>
 * What the four numbers really describe is a bilinear field over the face: brightness as a function of
 * position. Sampling that field at the corners the quad <em>actually</em> occupies is all that is needed,
 * and it is what modern Minecraft does - its ambient-occlusion pass blends the same four corner values by
 * each vertex's position within the face rather than assuming the vertex sits on a corner.
 *
 * <p>A full-size face samples the field at its own corners and gets its own values back unchanged, so
 * ordinary blocks render exactly as before, bit for bit.
 */
public final class RetroSmoothLighting {

	/** Vanilla face order: 0 bottom, 1 top, 2 north (-Z), 3 south (+Z), 4 west (-X), 5 east (+X). */
	public static final int FACES = 6;

	/**
	 * The two block axes each face spans, as {@code {axisA, axisB}} where 0 is X, 1 is Y, 2 is Z. A face
	 * is flat along its third axis, which is the one it faces.
	 */
	private static final int[][] FACE_AXES = {
		{0, 2}, // bottom: X, Z
		{0, 2}, // top:    X, Z
		{0, 1}, // north:  X, Y
		{0, 1}, // south:  X, Y
		{1, 2}, // west:   Y, Z
		{1, 2}, // east:   Y, Z
	};

	/**
	 * Where each of the four vertices sits on a FULL face, in the two axes above, as 0 for the low edge
	 * and 1 for the high edge. Read straight off the order the vanilla face methods emit vertices in, and
	 * that order is not the same for any two faces, which is exactly why this table exists rather than a
	 * clever loop.
	 */
	private static final int[][][] FACE_CORNERS = {
		{{0, 1}, {0, 0}, {1, 0}, {1, 1}}, // bottom
		{{1, 1}, {1, 0}, {0, 0}, {0, 1}}, // top
		{{0, 1}, {1, 1}, {1, 0}, {0, 0}}, // north
		{{0, 1}, {0, 0}, {1, 0}, {1, 1}}, // south
		{{1, 1}, {1, 0}, {0, 0}, {0, 1}}, // west
		{{0, 1}, {0, 0}, {1, 0}, {1, 1}}, // east
	};

	private RetroSmoothLighting() {}

	/**
	 * True when a face covers its whole block face, and so needs no correction at all. Checked before
	 * anything else so that full cubes, which are almost every block drawn in a frame, cost one compare
	 * per axis and nothing more.
	 */
	public static boolean isFullFace(Block block, int face) {
		int[] axes = FACE_AXES[face];
		return min(block, axes[0]) <= 0.0 && max(block, axes[0]) >= 1.0
			&& min(block, axes[1]) <= 0.0 && max(block, axes[1]) >= 1.0;
	}

	/**
	 * Resamples one channel's four corner values at the positions the quad actually occupies.
	 *
	 * @param values the four vertex values, in vanilla's emission order, overwritten in place
	 */
	public static void resample(Block block, int face, float[] values) {
		int[] axes = FACE_AXES[face];
		int[][] corners = FACE_CORNERS[face];

		// Recover the field from the four values: each vertex names the corner it was computed for.
		float c00 = 0.0F, c10 = 0.0F, c01 = 0.0F, c11 = 0.0F;
		for (int i = 0; i < 4; i++) {
			int a = corners[i][0];
			int b = corners[i][1];
			if (a == 0) {
				if (b == 0) c00 = values[i]; else c01 = values[i];
			} else {
				if (b == 0) c10 = values[i]; else c11 = values[i];
			}
		}

		double minA = clamp(min(block, axes[0]));
		double maxA = clamp(max(block, axes[0]));
		double minB = clamp(min(block, axes[1]));
		double maxB = clamp(max(block, axes[1]));

		for (int i = 0; i < 4; i++) {
			double a = corners[i][0] == 0 ? minA : maxA;
			double b = corners[i][1] == 0 ? minB : maxB;
			values[i] = bilinear(c00, c10, c01, c11, a, b);
		}
	}

	private static float bilinear(float c00, float c10, float c01, float c11, double a, double b) {
		double low = c00 + (c10 - c00) * a;
		double high = c01 + (c11 - c01) * a;
		return (float) (low + (high - low) * b);
	}

	private static double min(Block block, int axis) {
		return axis == 0 ? block.minX : (axis == 1 ? block.minY : block.minZ);
	}

	private static double max(Block block, int axis) {
		return axis == 0 ? block.maxX : (axis == 1 ? block.maxY : block.maxZ);
	}

	/**
	 * Bounds outside the block are legal in beta and several blocks use them, but the light field is only
	 * defined across the face, so sampling it beyond the edge would extrapolate into nonsense.
	 */
	private static double clamp(double value) {
		return value < 0.0 ? 0.0 : (value > 1.0 ? 1.0 : value);
	}
}
