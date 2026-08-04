package com.periut.retroapi.mixin.client.render;

import com.periut.retroapi.client.render.RetroSmoothLighting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Resamples ambient occlusion to the box actually being drawn, so partial blocks stop wearing a full
 * cube's light gradient. See {@link RetroSmoothLighting} for what goes wrong without it.
 *
 * <p>Hooked on the six face methods rather than on the renderer above them, which is what makes it
 * general: everything that draws a lit face goes through these, so vanilla stairs, vanilla slabs, beta's
 * pressure plates and cake, RetroAPI's own {@code BlockRenderContext.renderLitFace} and any mod's custom
 * renderer are all corrected by the same code, and none of them had to ask.
 *
 * <p>The values are put back on the way out. Beta recomputes them before each face it draws, so leaving
 * the resampled ones in place would be harmless today and a trap for the first caller that draws one face
 * twice.
 */
@Mixin(BlockRenderManager.class)
@Environment(EnvType.CLIENT)
public abstract class SmoothLightingMixin {

	@Shadow private boolean useAo;

	@Shadow public float firstVertexRed;
	@Shadow public float firstVertexGreen;
	@Shadow public float firstVertexBlue;
	@Shadow public float secondVertexRed;
	@Shadow public float secondVertexGreen;
	@Shadow public float secondVertexBlue;
	@Shadow public float thirdVertexRed;
	@Shadow public float thirdVertexGreen;
	@Shadow public float thirdVertexBlue;
	@Shadow public float fourthVertexRed;
	@Shadow public float fourthVertexGreen;
	@Shadow public float fourthVertexBlue;

	/** The values as beta computed them, kept so the renderer is handed back exactly what it had. */
	@Unique private final float[] retroapi$saved = new float[12];
	@Unique private final float[] retroapi$channel = new float[4];
	@Unique private boolean retroapi$resampled;

	@Inject(method = "renderBottomFace", at = @At("HEAD"))
	private void retroapi$resampleBottom(Block block, double x, double y, double z, int texture, CallbackInfo ci) {
		retroapi$resample(block, 0);
	}

	@Inject(method = "renderTopFace", at = @At("HEAD"))
	private void retroapi$resampleTop(Block block, double x, double y, double z, int texture, CallbackInfo ci) {
		retroapi$resample(block, 1);
	}

	@Inject(method = "renderEastFace", at = @At("HEAD"))
	private void retroapi$resampleNorth(Block block, double x, double y, double z, int texture, CallbackInfo ci) {
		retroapi$resample(block, 2);
	}

	@Inject(method = "renderWestFace", at = @At("HEAD"))
	private void retroapi$resampleSouth(Block block, double x, double y, double z, int texture, CallbackInfo ci) {
		retroapi$resample(block, 3);
	}

	@Inject(method = "renderNorthFace", at = @At("HEAD"))
	private void retroapi$resampleWest(Block block, double x, double y, double z, int texture, CallbackInfo ci) {
		retroapi$resample(block, 4);
	}

	@Inject(method = "renderSouthFace", at = @At("HEAD"))
	private void retroapi$resampleEast(Block block, double x, double y, double z, int texture, CallbackInfo ci) {
		retroapi$resample(block, 5);
	}

	@Inject(method = {"renderBottomFace", "renderTopFace", "renderEastFace", "renderWestFace",
		"renderNorthFace", "renderSouthFace"}, at = @At("RETURN"))
	private void retroapi$restore(Block block, double x, double y, double z, int texture, CallbackInfo ci) {
		if (!retroapi$resampled) {
			return;
		}
		retroapi$resampled = false;
		float[] saved = retroapi$saved;
		this.firstVertexRed = saved[0];
		this.firstVertexGreen = saved[1];
		this.firstVertexBlue = saved[2];
		this.secondVertexRed = saved[3];
		this.secondVertexGreen = saved[4];
		this.secondVertexBlue = saved[5];
		this.thirdVertexRed = saved[6];
		this.thirdVertexGreen = saved[7];
		this.thirdVertexBlue = saved[8];
		this.fourthVertexRed = saved[9];
		this.fourthVertexGreen = saved[10];
		this.fourthVertexBlue = saved[11];
	}

	@Unique
	private void retroapi$resample(Block block, int face) {
		// Flat lighting has one colour for the whole face, so there is no gradient to get wrong, and a
		// full-size face samples its own corners and gets its own values back. Both leave early.
		if (!this.useAo || RetroSmoothLighting.isFullFace(block, face)) {
			return;
		}

		float[] saved = retroapi$saved;
		saved[0] = this.firstVertexRed;
		saved[1] = this.firstVertexGreen;
		saved[2] = this.firstVertexBlue;
		saved[3] = this.secondVertexRed;
		saved[4] = this.secondVertexGreen;
		saved[5] = this.secondVertexBlue;
		saved[6] = this.thirdVertexRed;
		saved[7] = this.thirdVertexGreen;
		saved[8] = this.thirdVertexBlue;
		saved[9] = this.fourthVertexRed;
		saved[10] = this.fourthVertexGreen;
		saved[11] = this.fourthVertexBlue;
		retroapi$resampled = true;

		float[] channel = retroapi$channel;

		channel[0] = saved[0]; channel[1] = saved[3]; channel[2] = saved[6]; channel[3] = saved[9];
		RetroSmoothLighting.resample(block, face, channel);
		this.firstVertexRed = channel[0];
		this.secondVertexRed = channel[1];
		this.thirdVertexRed = channel[2];
		this.fourthVertexRed = channel[3];

		channel[0] = saved[1]; channel[1] = saved[4]; channel[2] = saved[7]; channel[3] = saved[10];
		RetroSmoothLighting.resample(block, face, channel);
		this.firstVertexGreen = channel[0];
		this.secondVertexGreen = channel[1];
		this.thirdVertexGreen = channel[2];
		this.fourthVertexGreen = channel[3];

		channel[0] = saved[2]; channel[1] = saved[5]; channel[2] = saved[8]; channel[3] = saved[11];
		RetroSmoothLighting.resample(block, face, channel);
		this.firstVertexBlue = channel[0];
		this.secondVertexBlue = channel[1];
		this.thirdVertexBlue = channel[2];
		this.fourthVertexBlue = channel[3];
	}
}
