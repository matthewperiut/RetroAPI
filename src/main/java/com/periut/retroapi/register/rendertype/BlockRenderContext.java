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

	public void renderBottomFace(int sprite) { blockRenderer.renderBottomFace(block, x, y, z, sprite); }
	public void renderTopFace(int sprite) { blockRenderer.renderTopFace(block, x, y, z, sprite); }
	public void renderNorthFace(int sprite) { blockRenderer.renderEastFace(block, x, y, z, sprite); }
	public void renderSouthFace(int sprite) { blockRenderer.renderWestFace(block, x, y, z, sprite); }
	public void renderWestFace(int sprite) { blockRenderer.renderNorthFace(block, x, y, z, sprite); }
	public void renderEastFace(int sprite) { blockRenderer.renderSouthFace(block, x, y, z, sprite); }
}
