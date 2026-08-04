package com.periut.retroapi.mixin.client.render;

import com.periut.retroapi.client.model.RetroAtlas;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Puts a partial block's texture where a whole block's texture would be, on the two faces beta draws
 * mirrored.
 *
 * <h2>The bug</h2>
 * Every face method picks its texture coordinates from the block's bounding box, so a box covering half
 * the block gets half the texture. That is right, and on four of the six faces it is also where you would
 * expect it. On the other two it is not, because those two emit their horizontal coordinate reversed:
 *
 * <pre>
 * renderEastFace  (-Z): the vertex at maxX is given the U computed from minX
 * renderSouthFace (+X): the vertex at minZ is given the U computed from maxZ
 * </pre>
 *
 * On a full cube that only mirrors the tile, which is invisible on a symmetric texture and is a beta quirk
 * old enough to count as the intended look. On a <em>partial</em> box it also picks the wrong half: a
 * stair's upper step, spanning x 0.5 to 1, is drawn with texture columns 8 to 16, while a whole block in
 * that same space shows columns 0 to 8 there, because it is mirrored too. The step's texture does not line
 * up with the block beside it, and the seam is plainly visible along the side of every staircase.
 *
 * <h2>The fix</h2>
 * Keep the mirroring, and pick the sub-rectangle that the mirroring implies: a point at {@code c} across
 * the block shows column {@code 16 - 16c}. Written that way a full face comes out with byte-identical
 * coordinates to vanilla's, epsilon and all, so nothing about ordinary blocks changes, and a partial face
 * lands where its neighbours are.
 *
 * <p>Redirecting the vertex call rather than patching the local it comes from: the coordinate is then
 * derived from the vertex's own position, which is unambiguous, instead of from a local variable slot
 * whose index is a compiler detail.
 */
@Mixin(BlockRenderManager.class)
@Environment(EnvType.CLIENT)
public abstract class PartialFaceUvMixin {

	@Shadow public int textureOverride;
	@Shadow public boolean flipTextureHorizontally;
	@Shadow public int eastFaceRotation;
	@Shadow public int southFaceRotation;

	@Redirect(method = "renderEastFace",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;vertex(DDDDD)V"))
	private void retroapi$alignEastFaceUv(Tessellator tessellator, double vx, double vy, double vz,
			double u, double v, Block block, double x, double y, double z, int texture) {
		tessellator.vertex(vx, vy, vz,
			retroapi$alignedU(u, vx - x, block.minX, block.maxX, texture, eastFaceRotation), v);
	}

	@Redirect(method = "renderSouthFace",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Tessellator;vertex(DDDDD)V"))
	private void retroapi$alignSouthFaceUv(Tessellator tessellator, double vx, double vy, double vz,
			double u, double v, Block block, double x, double y, double z, int texture) {
		tessellator.vertex(vx, vy, vz,
			retroapi$alignedU(u, vz - z, block.minZ, block.maxZ, texture, southFaceRotation), v);
	}

	/**
	 * The texture column this vertex should sit at, given that the face reads its texture backwards.
	 *
	 * @param original what vanilla computed, returned untouched whenever the correction cannot apply
	 * @param local    the vertex's position across the block, 0 to 1
	 * @param min      the box's low edge on that axis
	 * @param max      the box's high edge on that axis
	 */
	@Unique
	private double retroapi$alignedU(double original, double local, double min, double max,
			int texture, int rotation) {
		// A whole face already lands correctly, and its numbers are vanilla's to the last decimal.
		if (min <= 0.0 && max >= 1.0) {
			return original;
		}
		// Out-of-range bounds get the whole tile from vanilla's own clamp; a rotation permutes the
		// coordinates so this no longer knows which one it is holding; and a caller asking for a flip
		// is asking for exactly the mirroring this would undo. Leave all three alone.
		if (min < 0.0 || max > 1.0 || rotation != 0 || flipTextureHorizontally) {
			return original;
		}

		int tile = this.textureOverride >= 0 ? this.textureOverride : texture;

		// Mirrored: the near edge of the box shows the far end of the tile. The epsilon is vanilla's,
		// kept on the same edge it keeps it on, so the tile still stops a hair short of its neighbour.
		boolean atMin = Math.abs(local - min) <= Math.abs(local - max);
		double pixel = atMin ? (16.0 - min * 16.0 - 0.01) : (16.0 - max * 16.0);

		// Through RetroAtlas, never by hand. Vanilla's own coordinates in these methods are written
		// against a 256 pixel sheet of 16 pixel tiles, and RetroAPI rewrites those constants for the
		// expanded atlas it actually stitches (and stands aside for StationAPI's, which is sized
		// dynamically). Hand-rolling the vanilla arithmetic here computed coordinates for a sheet that
		// is not the one being sampled: the tile came out at a fraction of its size, tucked into a
		// corner of where it belonged, with the rest of the sheet showing through around it.
		return RetroAtlas.terrainU(tile, pixel);
	}
}
