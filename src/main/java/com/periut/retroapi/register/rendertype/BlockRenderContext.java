package com.periut.retroapi.register.rendertype;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.world.BlockView;

/**
 * Context passed to {@link CustomBlockRenderer} implementations.
 * Provides access to the block, position, world, and rendering helpers.
 * Not annotated @Environment(CLIENT) so that lambdas referencing this type
 * can be created on the server without triggering class-loading errors.
 */
public class BlockRenderContext {
	private static final boolean HAS_AMBIENT_OCCLUSION;
	static {
		boolean found;
		try {
			Minecraft.class.getMethod("isAmbientOcclusionEnabled");
			found = true;
		} catch (NoSuchMethodException e) {
			found = false;
		}
		HAS_AMBIENT_OCCLUSION = found;
	}

	private static final float[] FACE_SHADES = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};

	// [face][3] - normal direction offset
	private static final int[][] FACE_NORMALS = {
		{0, -1, 0}, // bottom
		{0, +1, 0}, // top
		{0, 0, -1}, // north
		{0, 0, +1}, // south
		{-1, 0, 0}, // west
		{+1, 0, 0}, // east
	};

	// [face][vertex][edge_index][3] - two tangent edge offsets per vertex for AO sampling.
	// Vertex order matches the vanilla face rendering winding.
	private static final int[][][][] FACE_AO_EDGES = {
		// Bottom (face 0): V1=SW, V2=NW, V3=NE, V4=SE
		{{{-1, 0, 0}, {0, 0, +1}}, {{-1, 0, 0}, {0, 0, -1}}, {{+1, 0, 0}, {0, 0, -1}}, {{+1, 0, 0}, {0, 0, +1}}},
		// Top (face 1): V1=SE, V2=NE, V3=NW, V4=SW
		{{{+1, 0, 0}, {0, 0, +1}}, {{+1, 0, 0}, {0, 0, -1}}, {{-1, 0, 0}, {0, 0, -1}}, {{-1, 0, 0}, {0, 0, +1}}},
		// North (face 2): V1=WestTop, V2=EastTop, V3=EastBot, V4=WestBot
		{{{-1, 0, 0}, {0, +1, 0}}, {{+1, 0, 0}, {0, +1, 0}}, {{+1, 0, 0}, {0, -1, 0}}, {{-1, 0, 0}, {0, -1, 0}}},
		// South (face 3): V1=WestTop, V2=WestBot, V3=EastBot, V4=EastTop
		{{{-1, 0, 0}, {0, +1, 0}}, {{-1, 0, 0}, {0, -1, 0}}, {{+1, 0, 0}, {0, -1, 0}}, {{+1, 0, 0}, {0, +1, 0}}},
		// West (face 4): V1=TopSouth, V2=TopNorth, V3=BotNorth, V4=BotSouth
		{{{0, +1, 0}, {0, 0, +1}}, {{0, +1, 0}, {0, 0, -1}}, {{0, -1, 0}, {0, 0, -1}}, {{0, -1, 0}, {0, 0, +1}}},
		// East (face 5): V1=BotSouth, V2=BotNorth, V3=TopNorth, V4=TopSouth
		{{{0, -1, 0}, {0, 0, +1}}, {{0, -1, 0}, {0, 0, -1}}, {{0, +1, 0}, {0, 0, -1}}, {{0, +1, 0}, {0, 0, +1}}},
	};

	private final BlockRenderManager blockRenderer;
	private final Block block;
	private final int x, y, z;
	private final BlockView world;

	public BlockRenderContext(BlockRenderManager blockRenderer, Block block, int x, int y, int z, BlockView world) {
		this.blockRenderer = blockRenderer;
		this.block = block;
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
	}

	public Block getBlock() { return block; }
	public int getX() { return x; }
	public int getY() { return y; }
	public int getZ() { return z; }
	public BlockView getWorld() { return world; }
	public BlockRenderManager getBlockRenderer() { return blockRenderer; }

	/** Get the Tesselator instance for direct vertex submission. */
	public Tessellator tesselator() { return Tessellator.INSTANCE; }

	/** Get block metadata at this position. */
	public int getMetadata() { return world.getBlockMeta(x, y, z); }

	/**
	 * Get the flattened block state at this position. Blocks with no declared properties
	 * get the implicit {@code meta} property, so this never returns null for a valid block.
	 */
	public com.periut.retroapi.state.RetroBlockState getState() {
		com.periut.retroapi.state.RetroBlockState state = com.periut.retroapi.state.RetroStates.get(world, x, y, z);
		return state != null ? state : com.periut.retroapi.state.RetroStates.getDefault(block);
	}

	/** Get brightness at this block's position. */
	public float getBrightness() { return block.getLuminance(world, x, y, z); }

	/** Get brightness at an arbitrary position. */
	public float getBrightness(int bx, int by, int bz) {
		return block.getLuminance(world, bx, by, bz);
	}

	/**
	 * Get the sprite index for a specific face.
	 * Face indices: 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east.
	 */
	public int getSprite(int face) { return block.getTextureId(world, x, y, z, face); }

	/**
	 * Check if a face should be rendered (accounts for neighbor occlusion).
	 * Face indices: 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east.
	 */
	public boolean shouldRenderFace(int face) {
		int nx = x, ny = y, nz = z;
		switch (face) {
			case 0: ny--; break;
			case 1: ny++; break;
			case 2: nz--; break;
			case 3: nz++; break;
			case 4: nx--; break;
			case 5: nx++; break;
		}
		return block.isSideVisible(world, nx, ny, nz, face);
	}

	/** Render this block as a standard full cube with vanilla lighting (flat or smooth). */
	public boolean renderFullCube() {
		return blockRenderer.renderBlock(block, x, y, z);
	}

	// === Lit face rendering ===

	/** True when the player has smooth lighting (ambient occlusion) enabled. */
	public static boolean isSmoothLighting() {
		return HAS_AMBIENT_OCCLUSION && Minecraft.isAmbientOcclusionEnabled();
	}

	/**
	 * Render a face with automatic lighting.
	 * Uses smooth lighting (ambient occlusion) when enabled in settings,
	 * otherwise uses flat per-face lighting.
	 * <p>
	 * Face indices: 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east.
	 */
	public void renderLitFace(int face, int sprite) {
		if (isSmoothLighting()) {
			renderSmoothFace(face, sprite);
		} else {
			renderFlatFace(face, sprite);
		}
	}

	/**
	 * Render all 6 faces with automatic lighting, using the block's sprites.
	 * Only renders faces that pass the visibility check.
	 */
	public void renderAllLitFaces() {
		for (int face = 0; face < 6; face++) {
			if (shouldRenderFace(face)) {
				renderLitFace(face, getSprite(face));
			}
		}
	}

	/**
	 * Render all 6 faces with automatic lighting using a single sprite.
	 * Only renders faces that pass the visibility check.
	 */
	public void renderAllLitFaces(int sprite) {
		for (int face = 0; face < 6; face++) {
			if (shouldRenderFace(face)) {
				renderLitFace(face, sprite);
			}
		}
	}

	private void renderFlatFace(int face, int sprite) {
		float shade = FACE_SHADES[face];
		int[] n = FACE_NORMALS[face];
		float brightness = block.getLuminance(world, x + n[0], y + n[1], z + n[2]);
		float center = block.getLuminance(world, x, y, z);
		if (brightness < center) brightness = center;

		Tessellator t = Tessellator.INSTANCE;
		t.color(shade * brightness, shade * brightness, shade * brightness);
		renderFace(face, sprite);
	}

	private void renderSmoothFace(int face, int sprite) {
		float shade = FACE_SHADES[face];
		int[] n = FACE_NORMALS[face];

		// Sample center brightness in the face neighbor plane
		int cx = x + n[0], cy = y + n[1], cz = z + n[2];
		float centerB = block.getLuminance(world, cx, cy, cz);

		// Compute per-vertex brightness using AO sampling
		float[] vb = new float[4];
		int[][][] faceEdges = FACE_AO_EDGES[face];
		for (int v = 0; v < 4; v++) {
			int[] e1 = faceEdges[v][0];
			int[] e2 = faceEdges[v][1];

			float edge1B = block.getLuminance(world, cx + e1[0], cy + e1[1], cz + e1[2]);
			float edge2B = block.getLuminance(world, cx + e2[0], cy + e2[1], cz + e2[2]);

			// Check edge translucency for AO occlusion
			boolean e1Trans = Block.BLOCKS_ALLOW_VISION[world.getBlockId(cx + e1[0], cy + e1[1], cz + e1[2])];
			boolean e2Trans = Block.BLOCKS_ALLOW_VISION[world.getBlockId(cx + e2[0], cy + e2[1], cz + e2[2])];

			float cornerB;
			if (!e1Trans && !e2Trans) {
				// Both edges opaque: corner is occluded (AO shadow)
				cornerB = edge1B;
			} else {
				cornerB = block.getLuminance(world,
					cx + e1[0] + e2[0], cy + e1[1] + e2[1], cz + e1[2] + e2[2]);
			}

			vb[v] = (centerB + edge1B + edge2B + cornerB) / 4.0F;
		}

		// Set per-vertex colors on the BlockRenderer and render
		RetroBlockRendererAccess access = (RetroBlockRendererAccess) blockRenderer;
		access.retroapi$setupSmoothFace(vb[0], vb[1], vb[2], vb[3], shade);
		renderFace(face, sprite);
		access.retroapi$cleanupSmoothFace();
	}

	// === Raw face rendering (no lighting) ===

	/**
	 * Render a single face by index (no lighting applied).
	 * Face indices: 0=bottom, 1=top, 2=north, 3=south, 4=west, 5=east.
	 */
	public void renderFace(int face, int sprite) {
		switch (face) {
			case 0: blockRenderer.renderBottomFace(block, x, y, z, sprite); break;
			case 1: blockRenderer.renderTopFace(block, x, y, z, sprite); break;
			case 2: blockRenderer.renderEastFace(block, x, y, z, sprite); break;
			case 3: blockRenderer.renderWestFace(block, x, y, z, sprite); break;
			case 4: blockRenderer.renderNorthFace(block, x, y, z, sprite); break;
			case 5: blockRenderer.renderSouthFace(block, x, y, z, sprite); break;
		}
	}

	// === Texture control: sprite override, flip, rotation, sub-rectangles =========================
	// These are the primitives a connected-texture / trim / decal renderer needs. Without them the only
	// way to draw "part of a sprite, mirrored" was to shrink the block's bounding box per corner and
	// keep a second, pre-flipped copy of every texture in the atlas.

	/**
	 * Forces every face drawn from here on to use one sprite, whatever the block says. Pair with
	 * {@link #clearSpriteOverride()} (or pass -1) - it is renderer state, not per-call.
	 */
	public void spriteOverride(int sprite) {
		((RetroBlockRendererAccess) blockRenderer).retroapi$setTextureOverride(sprite);
	}

	/** Drops a {@link #spriteOverride(int)} and goes back to the block's own textures. */
	public void clearSpriteOverride() {
		((RetroBlockRendererAccess) blockRenderer).retroapi$setTextureOverride(-1);
	}

	/**
	 * Mirrors the texture horizontally on the faces drawn from here on - the missing half of a connected
	 * texture set, without shipping a mirrored copy of every sprite. Remember to switch it back off.
	 */
	public void flipTexture(boolean flipped) {
		((RetroBlockRendererAccess) blockRenderer).retroapi$setFlipTexture(flipped);
	}

	/**
	 * Rotates a face's texture in 90° steps (0-3), the way vanilla varies grass and sand. Face indices are
	 * the usual 0 bottom, 1 top, 2 north, 3 south, 4 west, 5 east.
	 */
	public void faceRotation(int face, int quarterTurns) {
		((RetroBlockRendererAccess) blockRenderer).retroapi$setFaceRotation(face, quarterTurns);
	}

	/** Clears every per-face rotation set by {@link #faceRotation}. */
	public void clearFaceRotations() {
		for (int face = 0; face < 6; face++) {
			faceRotation(face, 0);
		}
	}

	/** Draws faces even where a neighboring block would normally hide them (vanilla's "render all faces"). */
	public void renderAllFaces(boolean all) {
		((RetroBlockRendererAccess) blockRenderer).retroapi$setSkipFaceCulling(all);
	}

	/**
	 * Draws one face with an explicit sub-rectangle of a sprite, in 0-16 pixel coordinates, optionally
	 * mirrored - full UV control with no tessellator boilerplate and no bounding-box tricks.
	 *
	 * <pre>
	 * // top-left quarter of the sprite, on the north face
	 * ctx.renderFaceUv(2, sprite, 0, 0, 8, 8, false);
	 * </pre>
	 *
	 * Coordinates are the block's current bounding box, so shrinking the box still works if you want it;
	 * the lighting is the flat per-face shade (call {@link #renderLitFace} for the vanilla-lit variant).
	 */
	public void renderFaceUv(int face, int sprite, double u0, double v0, double u1, double v1, boolean flipU) {
		double su0 = com.periut.retroapi.client.model.RetroAtlas.terrainU(sprite, flipU ? u1 : u0);
		double su1 = com.periut.retroapi.client.model.RetroAtlas.terrainU(sprite, flipU ? u0 : u1);
		double sv0 = com.periut.retroapi.client.model.RetroAtlas.terrainV(sprite, v0);
		double sv1 = com.periut.retroapi.client.model.RetroAtlas.terrainV(sprite, v1);

		double minX = x + block.minX, maxX = x + block.maxX;
		double minY = y + block.minY, maxY = y + block.maxY;
		double minZ = z + block.minZ, maxZ = z + block.maxZ;

		Tessellator t = Tessellator.INSTANCE;
		float shade = FACE_SHADES[face];
		int[] n = FACE_NORMALS[face];
		float brightness = block.getLuminance(world, x + n[0], y + n[1], z + n[2]);
		t.color(shade * brightness, shade * brightness, shade * brightness);

		switch (face) {
			case 0: // bottom (-Y)
				t.vertex(minX, minY, maxZ, su0, sv1);
				t.vertex(minX, minY, minZ, su0, sv0);
				t.vertex(maxX, minY, minZ, su1, sv0);
				t.vertex(maxX, minY, maxZ, su1, sv1);
				break;
			case 1: // top (+Y)
				t.vertex(maxX, maxY, maxZ, su1, sv1);
				t.vertex(maxX, maxY, minZ, su1, sv0);
				t.vertex(minX, maxY, minZ, su0, sv0);
				t.vertex(minX, maxY, maxZ, su0, sv1);
				break;
			case 2: // north (-Z)
				t.vertex(minX, maxY, minZ, su1, sv0);
				t.vertex(maxX, maxY, minZ, su0, sv0);
				t.vertex(maxX, minY, minZ, su0, sv1);
				t.vertex(minX, minY, minZ, su1, sv1);
				break;
			case 3: // south (+Z)
				t.vertex(minX, maxY, maxZ, su0, sv0);
				t.vertex(minX, minY, maxZ, su0, sv1);
				t.vertex(maxX, minY, maxZ, su1, sv1);
				t.vertex(maxX, maxY, maxZ, su1, sv0);
				break;
			case 4: // west (-X)
				t.vertex(minX, maxY, maxZ, su1, sv0);
				t.vertex(minX, maxY, minZ, su0, sv0);
				t.vertex(minX, minY, minZ, su0, sv1);
				t.vertex(minX, minY, maxZ, su1, sv1);
				break;
			default: // east (+X)
				t.vertex(maxX, minY, maxZ, su1, sv1);
				t.vertex(maxX, minY, minZ, su0, sv1);
				t.vertex(maxX, maxY, minZ, su0, sv0);
				t.vertex(maxX, maxY, maxZ, su1, sv0);
				break;
		}
	}

	/** {@link #renderFaceUv} without mirroring. */
	public void renderFaceUv(int face, int sprite, double u0, double v0, double u1, double v1) {
		renderFaceUv(face, sprite, u0, v0, u1, v1, false);
	}

	/**
	 * Draws one QUARTER of a sprite on a face - the corner-by-corner pass a connected-texture block
	 * makes, expressed directly instead of by resizing the block six times per corner.
	 *
	 * @param corner 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right
	 */
	public void renderFaceCorner(int face, int sprite, int corner, boolean flipU) {
		double u0 = (corner & 1) == 0 ? 0.0 : 8.0;
		double v0 = corner < 2 ? 0.0 : 8.0;
		renderFaceUv(face, sprite, u0, v0, u0 + 8.0, v0 + 8.0, flipU);
	}

	public void renderBottomFace(int sprite) { blockRenderer.renderBottomFace(block, x, y, z, sprite); }
	public void renderTopFace(int sprite) { blockRenderer.renderTopFace(block, x, y, z, sprite); }
	public void renderNorthFace(int sprite) { blockRenderer.renderEastFace(block, x, y, z, sprite); }
	public void renderSouthFace(int sprite) { blockRenderer.renderWestFace(block, x, y, z, sprite); }
	public void renderWestFace(int sprite) { blockRenderer.renderNorthFace(block, x, y, z, sprite); }
	public void renderEastFace(int sprite) { blockRenderer.renderSouthFace(block, x, y, z, sprite); }
}
