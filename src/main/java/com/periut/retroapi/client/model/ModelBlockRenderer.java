package com.periut.retroapi.client.model;

import com.periut.retroapi.client.render.RetroBlockColors;
import com.periut.retroapi.register.rendertype.BlockRenderContext;
import com.periut.retroapi.register.rendertype.CustomBlockRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.render.Tessellator;
import net.minecraft.world.BlockView;

import java.util.List;

/**
 * The built-in {@code retroapi:model} render type: draws the baked model selected by the
 * block's state through the ordinary custom-renderer path. Cullface quads honor neighbor
 * occlusion and flat per-face lighting (the vanilla terrain look); free quads (rotated
 * elements, crosses) shade by their geometric face. Weighted variants pick from a
 * position-seeded hash, stable across re-renders.
 */
public final class ModelBlockRenderer implements CustomBlockRenderer {

	@Override
	public boolean render(BlockRenderContext ctx) {
		Block block = ctx.getBlock();
		BlockstateLoader.BlockModelTable table = BlockstateLoader.tableFor(block);
		if (table == null) {
			return false;
		}
		int index = ctx.getState().getIndex();
		List<BlockstateLoader.WeightedList> units = table.forState(index);
		if (units.isEmpty()) {
			return false;
		}

		int x = ctx.getX(), y = ctx.getY(), z = ctx.getZ();
		long seed = positionSeed(x, y, z);
		BlockView world = ctx.getWorld();
		Tessellator tessellator = ctx.tesselator();
		RetroBlockColors.Provider tint = RetroBlockColors.get(block);

		// While a block is being mined, the world renderer re-renders it with the crack
		// sprite as a texture override; honoring it draws the breaking animation over the
		// model's own geometry, exactly like vanilla shapes.
		int override = ((com.periut.retroapi.mixin.client.BlockRenderManagerAccessor) ctx.getBlockRenderer())
			.retroapi$getTextureOverride();

		boolean smooth = com.periut.retroapi.register.rendertype.BlockRenderContext.isSmoothLighting()
			&& override < 0;

		for (BlockstateLoader.WeightedList unit : units) {
			RetroBakedModel baked = unit.pick(seed).baked();
			for (RetroBakedModel.BakedQuad quad : baked.quads) {
				if (quad.cullface >= 0 && !ctx.shouldRenderFace(quad.cullface)) {
					continue;
				}

				float shade = quad.shade ? RetroBakedModel.FACE_SHADES[quad.lightFace] : 1.0f;
				float tintR = 1.0f, tintG = 1.0f, tintB = 1.0f;
				if (quad.tintIndex >= 0 && tint != null && override < 0) {
					int color = tint.getColor(ctx.getState(), world, x, y, z, quad.tintIndex);
					tintR = ((color >> 16) & 0xFF) / 255.0f;
					tintG = ((color >> 8) & 0xFF) / 255.0f;
					tintB = (color & 0xFF) / 255.0f;
				}

				int slot = override >= 0 ? override : quad.texture.id;

				// Lighting is driven by the quad's GEOMETRIC face for every shaded quad,
				// cullface or not, exactly like the lit-face pipeline custom render types
				// use: smooth lighting samples the vanilla AO corners (bilinearly
				// interpolated for partial faces), flat lighting takes the neighbor cell
				// maxed with the center. Mixing free quads onto a different rule made
				// model blocks visibly darker than their lit-face siblings.
				float[] corners = (smooth && baked.ambientOcclusion && quad.shade)
					? smoothCorners(block, world, x, y, z, quad.lightFace)
					: null;
				float flat = corners == null ? faceBrightness(block, world, x, y, z, quad) : 0.0f;

				for (int v = 0; v < 4; v++) {
					float px = quad.positions[v * 3];
					float py = quad.positions[v * 3 + 1];
					float pz = quad.positions[v * 3 + 2];
					float brightness;
					if (corners != null) {
						float u01 = axisValue(quad.lightFace, true, px, py, pz);
						float v01 = axisValue(quad.lightFace, false, px, py, pz);
						float b0 = corners[0] + (corners[1] - corners[0]) * u01;
						float b1 = corners[2] + (corners[3] - corners[2]) * u01;
						brightness = b0 + (b1 - b0) * v01;
					} else {
						brightness = flat;
					}
					tessellator.color(shade * brightness * tintR, shade * brightness * tintG, shade * brightness * tintB);
					tessellator.vertex(
						x + px, y + py, z + pz,
						RetroAtlas.terrainU(slot, quad.uvs[v * 2]),
						RetroAtlas.terrainV(slot, quad.uvs[v * 2 + 1]));
				}
			}
		}
		return true;
	}

	/** The face-plane coordinate (0-1) of a position along the face's u or v axis. */
	private static float axisValue(int face, boolean uAxis, float px, float py, float pz) {
		switch (face) {
			case 0: case 1: return uAxis ? px : pz; // horizontal faces: u = x, v = z
			case 2: case 3: return uAxis ? px : py; // z faces: u = x, v = y
			default: return uAxis ? pz : py;        // x faces: u = z, v = y
		}
	}

	private static final int[][] FACE_NORMAL_OFFSETS = {
		{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0},
	};

	/**
	 * Brightness at the four face corners (u0v0, u1v0, u0v1, u1v1), sampled in the
	 * neighbor plane with the vanilla AO corner-occlusion rule.
	 */
	private static float[] smoothCorners(Block block, BlockView world, int x, int y, int z, int face) {
		int[] n = FACE_NORMAL_OFFSETS[face];
		int cx = x + n[0], cy = y + n[1], cz = z + n[2];
		float center = block.getLuminance(world, cx, cy, cz);

		float[] corners = new float[4];
		for (int i = 0; i < 4; i++) {
			int cu = (i & 1) == 0 ? -1 : 1;
			int cv = (i & 2) == 0 ? -1 : 1;
			int[] e1 = axisOffset(face, true, cu);
			int[] e2 = axisOffset(face, false, cv);

			float edge1B = block.getLuminance(world, cx + e1[0], cy + e1[1], cz + e1[2]);
			float edge2B = block.getLuminance(world, cx + e2[0], cy + e2[1], cz + e2[2]);

			boolean e1Trans = Block.BLOCKS_ALLOW_VISION[world.getBlockId(cx + e1[0], cy + e1[1], cz + e1[2])];
			boolean e2Trans = Block.BLOCKS_ALLOW_VISION[world.getBlockId(cx + e2[0], cy + e2[1], cz + e2[2])];

			float cornerB;
			if (!e1Trans && !e2Trans) {
				cornerB = edge1B; // both edges opaque: the corner is occluded
			} else {
				cornerB = block.getLuminance(world,
					cx + e1[0] + e2[0], cy + e1[1] + e2[1], cz + e1[2] + e2[2]);
			}
			corners[i] = (center + edge1B + edge2B + cornerB) / 4.0f;
		}
		return corners;
	}

	/** A unit offset along the face's u or v axis, signed. */
	private static int[] axisOffset(int face, boolean uAxis, int sign) {
		switch (face) {
			case 0: case 1: return uAxis ? new int[]{sign, 0, 0} : new int[]{0, 0, sign};
			case 2: case 3: return uAxis ? new int[]{sign, 0, 0} : new int[]{0, sign, 0};
			default: return uAxis ? new int[]{0, 0, sign} : new int[]{0, sign, 0};
		}
	}

	/** Flat lighting: every shaded quad samples its facing neighbor maxed with the center. */
	private static float faceBrightness(Block block, BlockView world, int x, int y, int z, RetroBakedModel.BakedQuad quad) {
		float center = block.getLuminance(world, x, y, z);
		if (!quad.shade) {
			return center;
		}
		int nx = x, ny = y, nz = z;
		switch (quad.lightFace) {
			case 0: ny--; break;
			case 1: ny++; break;
			case 2: nz--; break;
			case 3: nz++; break;
			case 4: nx--; break;
			case 5: nx++; break;
		}
		float neighbor = block.getLuminance(world, nx, ny, nz);
		return Math.max(neighbor, center);
	}

	/** Vanilla-style coordinate hash: stable, decorrelated, no Math.random. */
	public static long positionSeed(int x, int y, int z) {
		long seed = (x * 3129871L) ^ (z * 116129781L) ^ y;
		seed = seed * seed * 42317861L + seed * 11L;
		return seed >> 16;
	}

	private static final float[][] FACE_NORMALS = {
		{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0},
	};

	/**
	 * Inventory/hand form: the baked model at full brightness with per-face shading,
	 * drawn around the origin (the caller translates by -0.5 like vanilla's cube path).
	 * The metadata (item damage) selects the state's low bits.
	 */
	public static void renderInventory(Block block, int metadata) {
		BlockstateLoader.BlockModelTable table = BlockstateLoader.tableFor(block);
		if (table == null) {
			return;
		}
		// Metadata 0 renders the block's DEFAULT state, not raw index 0: for multipart
		// blocks like walls, index 0 (every property at its first value) often applies
		// no parts at all and would draw nothing in the inventory. Nonzero metadata
		// still selects meta subtypes (the RetroMetaBlockItem pattern).
		int index = metadata == 0
			? com.periut.retroapi.state.RetroStates.getDefault(block).getIndex()
			: com.periut.retroapi.state.RetroStates.fromIndex(block, metadata & 15).getIndex();
		List<BlockstateLoader.WeightedList> units = table.forState(index);
		if (units.isEmpty()) {
			// Belt and braces: an empty state falls back to the default state's parts.
			units = table.forState(com.periut.retroapi.state.RetroStates.getDefault(block).getIndex());
		}
		Tessellator tessellator = Tessellator.INSTANCE;
		RetroBlockColors.Provider tint = RetroBlockColors.get(block);

		for (BlockstateLoader.WeightedList unit : units) {
			RetroBakedModel baked = unit.options.get(0).baked();
			for (RetroBakedModel.BakedQuad quad : baked.quads) {
				// GL lighting shades item-form geometry from the normals (exactly like the
				// vanilla cube path, which sets only normals); baking FACE_SHADES into the
				// vertex colors on top of that double-applies the shading and items render
				// noticeably darker than their placed siblings. Color is only set for tint.
				float[] normal = FACE_NORMALS[quad.lightFace];
				tessellator.startQuads();
				tessellator.normal(normal[0], normal[1], normal[2]);
				if (quad.tintIndex >= 0 && tint != null) {
					int color = tint.getColor(null, null, 0, 0, 0, quad.tintIndex);
					tessellator.color(((color >> 16) & 0xFF) / 255.0f,
						((color >> 8) & 0xFF) / 255.0f, (color & 0xFF) / 255.0f);
				} else {
					tessellator.color(1.0f, 1.0f, 1.0f);
				}
				int slot = quad.texture.id;
				for (int v = 0; v < 4; v++) {
					tessellator.vertex(
						quad.positions[v * 3],
						quad.positions[v * 3 + 1],
						quad.positions[v * 3 + 2],
						RetroAtlas.terrainU(slot, quad.uvs[v * 2]),
						RetroAtlas.terrainV(slot, quad.uvs[v * 2 + 1]));
				}
				tessellator.draw();
			}
		}
	}
}
