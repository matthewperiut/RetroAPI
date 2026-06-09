package com.periut.retroapi.client.model;

import com.periut.retroapi.register.block.RetroTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A parsed, parent-resolved model (vanilla 1.8-era schema): elements with faces, the
 * texture-variable map resolved down to {@link RetroTexture} handles, ambient occlusion
 * flag, and display transforms. Pure data; baking happens in {@link RetroBakedModel}.
 */
public final class RetroModel {

	/** Texture variable name (without #) to resolved texture. */
	public final Map<String, RetroTexture> textures = new HashMap<>();
	public final List<Element> elements = new ArrayList<>();
	public boolean ambientOcclusion = true;
	/** Display transforms by context name (gui, firstperson_righthand, thirdperson_righthand). */
	public final Map<String, Transform> display = new HashMap<>();

	/** The "particle" texture, or any texture as fallback, or null. */
	public RetroTexture particle() {
		RetroTexture p = textures.get("particle");
		if (p != null) {
			return p;
		}
		return textures.isEmpty() ? null : textures.values().iterator().next();
	}

	/** rotation/translation/scale, each xyz; vanilla display schema. */
	public static final class Transform {
		public final float[] rotation;
		public final float[] translation;
		public final float[] scale;

		public Transform(float[] rotation, float[] translation, float[] scale) {
			this.rotation = rotation;
			this.translation = translation;
			this.scale = scale;
		}
	}

	/** One cuboid element: from/to in 0-16 box coordinates plus optional rotation. */
	public static final class Element {
		public final float[] from;
		public final float[] to;
		public final boolean shade;
		/** null when the element is unrotated. */
		public final Rotation rotation;
		/** Faces by index 0=down,1=up,2=north,3=south,4=west,5=east; null = face omitted. */
		public final Face[] faces = new Face[6];

		public Element(float[] from, float[] to, boolean shade, Rotation rotation) {
			this.from = from;
			this.to = to;
			this.shade = shade;
			this.rotation = rotation;
		}
	}

	public static final class Rotation {
		public final float[] origin;
		public final char axis;
		public final float angle;
		public final boolean rescale;

		public Rotation(float[] origin, char axis, float angle, boolean rescale) {
			this.origin = origin;
			this.axis = axis;
			this.angle = angle;
			this.rescale = rescale;
		}
	}

	public static final class Face {
		/** u1, v1, u2, v2 in 0-16 pixel space; never null after parsing (derived when omitted). */
		public final float[] uv;
		/** Texture variable name without the leading #. */
		public final String texture;
		/** Cull direction 0-5, or -1 for none. */
		public final int cullface;
		/** UV rotation: 0, 90, 180, 270. */
		public final int rotation;
		public final int tintIndex;

		public Face(float[] uv, String texture, int cullface, int rotation, int tintIndex) {
			this.uv = uv;
			this.texture = texture;
			this.cullface = cullface;
			this.rotation = rotation;
			this.tintIndex = tintIndex;
		}
	}
}
