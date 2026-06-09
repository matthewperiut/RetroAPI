package com.periut.retroapi.client.model;

import com.periut.retroapi.register.block.RetroTexture;

import java.util.ArrayList;
import java.util.List;

/**
 * A model baked for one variant rotation: a list of quads with positions in block space
 * (0-1), pixel UVs, the texture handle, cullface, shade flag and tint index. UVs resolve
 * to atlas coordinates at RENDER time through {@link RetroAtlas}, so atlas reloads and
 * slot reassignment never invalidate baked models.
 */
public final class RetroBakedModel {

	/** Quad winding per face, CCW seen from outside, as corner selectors {x,y,z} (0=min, 1=max). */
	private static final int[][][] FACE_CORNERS = {
		{{0,0,1},{0,0,0},{1,0,0},{1,0,1}}, // down
		{{0,1,0},{0,1,1},{1,1,1},{1,1,0}}, // up
		{{1,1,0},{1,0,0},{0,0,0},{0,1,0}}, // north
		{{0,1,1},{0,0,1},{1,0,1},{1,1,1}}, // south
		{{0,1,0},{0,0,0},{0,0,1},{0,1,1}}, // west
		{{1,1,1},{1,0,1},{1,0,0},{1,1,0}}, // east
	};

	/** Face shade factors, vanilla terrain convention (down/up/ns/we). */
	public static final float[] FACE_SHADES = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};

	public static final class BakedQuad {
		/** 4 vertices, xyz each, block space. */
		public final float[] positions = new float[12];
		/** 4 vertices, uv each, 0-16 pixel space. */
		public final float[] uvs = new float[8];
		public final RetroTexture texture;
		/** Cull direction after variant rotation, or -1. */
		public int cullface;
		/** The face this quad geometrically points toward (for shading), 0-5. */
		public int lightFace;
		public final boolean shade;
		public final int tintIndex;

		BakedQuad(RetroTexture texture, int cullface, int lightFace, boolean shade, int tintIndex) {
			this.texture = texture;
			this.cullface = cullface;
			this.lightFace = lightFace;
			this.shade = shade;
			this.tintIndex = tintIndex;
		}
	}

	public final List<BakedQuad> quads = new ArrayList<>();
	public final boolean ambientOcclusion;

	private RetroBakedModel(boolean ambientOcclusion) {
		this.ambientOcclusion = ambientOcclusion;
	}

	/** Bakes a model with a variant rotation (x/y in 90-degree steps), without uvlock. */
	public static RetroBakedModel bake(RetroModel model, int xRot, int yRot) {
		return bake(model, xRot, yRot, false);
	}

	/**
	 * Bakes a model with a variant rotation. With {@code uvlock}, UVs are re-derived from
	 * the FINAL (rotated) positions, so textures stay world-aligned no matter how the
	 * variant spins the geometry, which is what wall/fence side models expect.
	 */
	public static RetroBakedModel bake(RetroModel model, int xRot, int yRot, boolean uvlock) {
		RetroBakedModel baked = new RetroBakedModel(model.ambientOcclusion);
		for (RetroModel.Element element : model.elements) {
			for (int face = 0; face < 6; face++) {
				RetroModel.Face modelFace = element.faces[face];
				if (modelFace == null) {
					continue;
				}
				RetroTexture texture = model.textures.get(modelFace.texture);
				if (texture == null) {
					com.periut.retroapi.RetroAPI.LOGGER.warn("Model face references unresolved texture variable #{}", modelFace.texture);
					continue;
				}
				BakedQuad quad = new BakedQuad(texture, modelFace.cullface, face, element.shade, modelFace.tintIndex);

				// Corner positions from the element box (16ths to block space).
				int[][] corners = FACE_CORNERS[face];
				for (int v = 0; v < 4; v++) {
					quad.positions[v * 3] = (corners[v][0] == 0 ? element.from[0] : element.to[0]) / 16.0f;
					quad.positions[v * 3 + 1] = (corners[v][1] == 0 ? element.from[1] : element.to[1]) / 16.0f;
					quad.positions[v * 3 + 2] = (corners[v][2] == 0 ? element.from[2] : element.to[2]) / 16.0f;
				}

				// UVs: corner order is (u1,v1) (u1,v2) (u2,v2) (u2,v1) rotated by face rotation.
				float u1 = modelFace.uv[0], v1 = modelFace.uv[1], u2 = modelFace.uv[2], v2 = modelFace.uv[3];
				float[][] uvCorners = {{u1, v1}, {u1, v2}, {u2, v2}, {u2, v1}};
				int shift = ((modelFace.rotation % 360) + 360) % 360 / 90;
				for (int v = 0; v < 4; v++) {
					float[] uv = uvCorners[(v + shift) & 3];
					quad.uvs[v * 2] = uv[0];
					quad.uvs[v * 2 + 1] = uv[1];
				}

				// Element rotation (single-axis, +-45 or +-22.5 degrees), about the origin.
				if (element.rotation != null) {
					applyElementRotation(quad, element.rotation);
				}

				// Variant rotation in 90-degree steps, about the block center.
				for (int i = 0; i < ((xRot % 360) + 360) % 360 / 90; i++) {
					rotateQuadX(quad);
				}
				for (int i = 0; i < ((yRot % 360) + 360) % 360 / 90; i++) {
					rotateQuadY(quad);
				}

				// uvlock re-derives UVs from world-space position, which must happen for
				// EVERY uvlock face, not just rotated ones. A multipart block like a wall
				// mixes rotated parts (the connecting arms) with non-rotated parts (the
				// post, and the straight-through arm at rotation 0). If only the rotated
				// parts were relocked, the non-rotated ones kept their element-relative
				// UV and disagreed by the element's offset, the wall's +x faces looked
				// shifted ~8px against each other. Relocking all uvlock faces makes them
				// share one world-aligned mapping.
				if (uvlock) {
					relockUvs(quad);
				}

				baked.quads.add(quad);
			}
		}
		return baked;
	}

	private static void applyElementRotation(BakedQuad quad, RetroModel.Rotation rotation) {
		double radians = Math.toRadians(rotation.angle);
		float sin = (float) Math.sin(radians);
		float cos = (float) Math.cos(radians);
		float scale = rotation.rescale ? (float) (1.0 / Math.cos(Math.toRadians(Math.abs(rotation.angle)))) : 1.0f;
		float ox = rotation.origin[0] / 16.0f, oy = rotation.origin[1] / 16.0f, oz = rotation.origin[2] / 16.0f;
		for (int v = 0; v < 4; v++) {
			float x = quad.positions[v * 3] - ox;
			float y = quad.positions[v * 3 + 1] - oy;
			float z = quad.positions[v * 3 + 2] - oz;
			float nx = x, ny = y, nz = z;
			switch (rotation.axis) {
				case 'x':
					ny = y * cos - z * sin;
					nz = y * sin + z * cos;
					ny *= scale; nz *= scale;
					break;
				case 'y':
					nx = x * cos + z * sin;
					nz = -x * sin + z * cos;
					nx *= scale; nz *= scale;
					break;
				case 'z':
					nx = x * cos - y * sin;
					ny = x * sin + y * cos;
					nx *= scale; ny *= scale;
					break;
			}
			quad.positions[v * 3] = nx + ox;
			quad.positions[v * 3 + 1] = ny + oy;
			quad.positions[v * 3 + 2] = nz + oz;
		}
	}

	/**
	 * uvlock: re-derives each vertex's UV from its final world-space position, using the
	 * same face projection {@code deriveUv} uses for unrotated elements (down/up: u = x,
	 * v = z; z faces: u = x, v = 16 - y; x faces: u = z, v = 16 - y). The texture stays
	 * put while the geometry rotates underneath it.
	 */
	private static void relockUvs(BakedQuad quad) {
		// The texture must read the same on a rotated face as on a non-rotated one. The
		// trick is that the base bake derives UVs from the ELEMENT bounds, so the "back"
		// faces (north, east) count their horizontal axis DOWN from the element's far
		// edge. Block-relative coords (0-16) only match that when the element is centred;
		// on an off-centre piece like a wall segment (z 0-8) the +x face came out shifted
		// by 8. So for those two faces we project u against the quad's OWN extent
		// (min+max-pos), which reproduces the element-relative result on any geometry.
		float[] uv = new float[2];
		for (int v = 0; v < 4; v++) {
			// Shared projection (see RetroFaceUv): one world-aligned mapping used by both
			// the model path here and, eventually, the vanilla face renderers, so a wall
			// and a stair drawn through different paths still line their textures up.
			com.periut.retroapi.client.texture.RetroFaceUv.project(
				quad.lightFace,
				quad.positions[v * 3] * 16.0f,
				quad.positions[v * 3 + 1] * 16.0f,
				quad.positions[v * 3 + 2] * 16.0f,
				uv);
			quad.uvs[v * 2] = uv[0];
			quad.uvs[v * 2 + 1] = uv[1];
		}
	}

	// Face remap tables for 90-degree variant rotations.
	private static final int[] X_FACE = {2, 3, 1, 0, 4, 5}; // down,up,north,south,west,east after +90 about X
	private static final int[] Y_FACE = {0, 1, 5, 4, 2, 3}; // after +90 about Y

	private static void rotateQuadX(BakedQuad quad) {
		for (int v = 0; v < 4; v++) {
			float y = quad.positions[v * 3 + 1] - 0.5f;
			float z = quad.positions[v * 3 + 2] - 0.5f;
			quad.positions[v * 3 + 1] = -z + 0.5f;
			quad.positions[v * 3 + 2] = y + 0.5f;
		}
		if (quad.cullface >= 0) quad.cullface = X_FACE[quad.cullface];
		quad.lightFace = X_FACE[quad.lightFace];
	}

	private static void rotateQuadY(BakedQuad quad) {
		for (int v = 0; v < 4; v++) {
			float x = quad.positions[v * 3] - 0.5f;
			float z = quad.positions[v * 3 + 2] - 0.5f;
			quad.positions[v * 3] = -z + 0.5f;
			quad.positions[v * 3 + 2] = x + 0.5f;
		}
		if (quad.cullface >= 0) quad.cullface = Y_FACE[quad.cullface];
		quad.lightFace = Y_FACE[quad.lightFace];
	}
}
