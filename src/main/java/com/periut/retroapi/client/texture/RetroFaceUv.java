package com.periut.retroapi.client.texture;

/**
 * The single source of truth for how a textured face is projected onto the texture:
 * given a face and a position on it (in block space, 0..16), it returns the world-aligned
 * (u, v). This is the projection the model path uses for uvlock, factored out so the SAME
 * mapping can drive other render paths (e.g. beta's vanilla face renderers) and everything,
 * walls, stairs, model blocks, agrees on where the texture sits.
 *
 * <p>Faces use the model convention: 0=down, 1=up, 2=north(-z), 3=south(+z), 4=west(-x),
 * 5=east(+x). The mapping is world-aligned: the texture is "painted on the world" so two
 * faces at the same world position show the same texture column, which is what makes the
 * seams line up across a multi-box shape like a wall or a stair.</p>
 */
public final class RetroFaceUv {

	private RetroFaceUv() {
	}

	/**
	 * Writes the world-aligned (u, v) for a point on a face into {@code out} (length >= 2),
	 * in block space (0..16). {@code px/py/pz} are the point's block-space coordinates.
	 */
	public static void project(int face, float px, float py, float pz, float[] out) {
		switch (face) {
			case 0:  out[0] = px;          out[1] = 16.0f - pz; break; // down
			case 1:  out[0] = px;          out[1] = pz;         break; // up
			case 2:  out[0] = 16.0f - px;  out[1] = 16.0f - py; break; // north (-z)
			case 3:  out[0] = px;          out[1] = 16.0f - py; break; // south (+z)
			case 4:  out[0] = pz;          out[1] = 16.0f - py; break; // west (-x)
			default: out[0] = 16.0f - pz;  out[1] = 16.0f - py; break; // east (+x)
		}
	}
}
