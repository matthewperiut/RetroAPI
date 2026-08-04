package com.periut.retroapi.mixin.client;

import com.periut.retroapi.register.rendertype.BlockRenderContext;
import com.periut.retroapi.register.rendertype.CustomBlockRenderer;
import com.periut.retroapi.register.rendertype.RetroBlockRendererAccess;
import com.periut.retroapi.register.rendertype.RenderType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.world.BlockView;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderManager.class)
@Environment(EnvType.CLIENT)
public class BlockRendererMixin implements RetroBlockRendererAccess {
	@Shadow private BlockView blockView;
	@Shadow private boolean useAo;
	@Shadow private float firstVertexRed;
	@Shadow private float firstVertexGreen;
	@Shadow private float firstVertexBlue;
	@Shadow private float secondVertexRed;
	@Shadow private float secondVertexGreen;
	@Shadow private float secondVertexBlue;
	@Shadow private float thirdVertexRed;
	@Shadow private float thirdVertexGreen;
	@Shadow private float thirdVertexBlue;
	@Shadow private float fourthVertexRed;
	@Shadow private float fourthVertexGreen;
	@Shadow private float fourthVertexBlue;
	@Shadow public boolean flipTextureHorizontally;
	@Shadow public boolean skipFaceCulling;
	@Shadow public int bottomFaceRotation;
	@Shadow public int topFaceRotation;
	@Shadow public int northFaceRotation;
	@Shadow public int southFaceRotation;
	@Shadow public int westFaceRotation;
	@Shadow public int eastFaceRotation;

	@Override
	public void retroapi$setupSmoothFace(float v1, float v2, float v3, float v4, float shade) {
		this.useAo = true;
		this.firstVertexRed = this.firstVertexGreen = this.firstVertexBlue = shade * v1;
		this.secondVertexRed = this.secondVertexGreen = this.secondVertexBlue = shade * v2;
		this.thirdVertexRed = this.thirdVertexGreen = this.thirdVertexBlue = shade * v3;
		this.fourthVertexRed = this.fourthVertexGreen = this.fourthVertexBlue = shade * v4;
	}

	@Override
	public void retroapi$cleanupSmoothFace() {
		this.useAo = false;
	}

	@Override
	public void retroapi$setTextureOverride(int sprite) {
		this.textureOverride = sprite;
	}

	@Override
	public void retroapi$setFlipTexture(boolean flipped) {
		this.flipTextureHorizontally = flipped;
	}

	@Override
	public void retroapi$setFaceRotation(int face, int quarterTurns) {
		int rotation = quarterTurns & 3;
		switch (face) {
			case 0: this.bottomFaceRotation = rotation; break;
			case 1: this.topFaceRotation = rotation; break;
			case 2: this.northFaceRotation = rotation; break;
			case 3: this.southFaceRotation = rotation; break;
			case 4: this.westFaceRotation = rotation; break;
			default: this.eastFaceRotation = rotation; break;
		}
	}

	@Override
	public void retroapi$setSkipFaceCulling(boolean skip) {
		this.skipFaceCulling = skip;
	}

	// --- Custom render type handling ---

	@Inject(method = "render(Lnet/minecraft/block/Block;III)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleCustomRenderType(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		int type = block.getRenderType();
		if (RenderType.isCustom(type)) {
			CustomBlockRenderer renderer = RenderType.getRenderer(type);
			if (renderer != null) {
				block.updateBoundingBox(this.blockView, x, y, z);
				BlockRenderContext ctx = new BlockRenderContext(
					(BlockRenderManager) (Object) this, block, x, y, z, this.blockView
				);
				cir.setReturnValue(renderer.render(ctx));
			}
		}
	}

	@Inject(method = "render(Lnet/minecraft/block/Block;IF)V", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleCustomRenderAsItem(Block block, int metadata, float brightness, CallbackInfo ci) {
		int type = block.getRenderType();
		if (RenderType.isCustom(type)) {
			// Model blocks render their baked model in inventory/hand instead of a plain cube.
			if (com.periut.retroapi.client.model.BlockstateLoader.tableFor(block) != null) {
				GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
				com.periut.retroapi.client.model.ModelBlockRenderer.renderInventory(block, metadata);
				GL11.glTranslatef(0.5F, 0.5F, 0.5F);
				ci.cancel();
				return;
			}
			block.setupRenderBoundingBox();
			GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
			Tessellator tesselator = Tessellator.INSTANCE;
			BlockRenderManager self = (BlockRenderManager) (Object) this;

			tesselator.startQuads();
			tesselator.normal(0.0F, -1.0F, 0.0F);
			self.renderBottomFace(block, 0.0, 0.0, 0.0, block.getTexture(0, metadata));
			tesselator.draw();

			tesselator.startQuads();
			tesselator.normal(0.0F, 1.0F, 0.0F);
			self.renderTopFace(block, 0.0, 0.0, 0.0, block.getTexture(1, metadata));
			tesselator.draw();

			tesselator.startQuads();
			tesselator.normal(0.0F, 0.0F, -1.0F);
			self.renderEastFace(block, 0.0, 0.0, 0.0, block.getTexture(2, metadata));
			tesselator.draw();

			tesselator.startQuads();
			tesselator.normal(0.0F, 0.0F, 1.0F);
			self.renderWestFace(block, 0.0, 0.0, 0.0, block.getTexture(3, metadata));
			tesselator.draw();

			tesselator.startQuads();
			tesselator.normal(-1.0F, 0.0F, 0.0F);
			self.renderNorthFace(block, 0.0, 0.0, 0.0, block.getTexture(4, metadata));
			tesselator.draw();

			tesselator.startQuads();
			tesselator.normal(1.0F, 0.0F, 0.0F);
			self.renderSouthFace(block, 0.0, 0.0, 0.0, block.getTexture(5, metadata));
			tesselator.draw();

			GL11.glTranslatef(0.5F, 0.5F, 0.5F);
			ci.cancel();
		}
	}

	/**
	 * Custom render types draw their item form as a little 3D block, like a cube does, unless the type
	 * declared itself flat ({@code RenderType.setFlatItem}). Vanilla makes the same split, and draws a
	 * door, a ladder or a pane as a sprite for the same reason: those shapes are unreadable at inventory
	 * size, and their real look is not six faces of one texture.
	 */
	@Inject(method = "isSideLit", at = @At("HEAD"), cancellable = true)
	private static void retroapi$customIsItem3d(int type, CallbackInfoReturnable<Boolean> cir) {
		if (RenderType.isCustom(type)) {
			cir.setReturnValue(!RenderType.isFlatItem(type));
		}
	}

	// --- Layered blocks: RetroBlockAccess.overlay(...) --------------------------------------------

	@Shadow public int textureOverride;

	/**
	 * Draws a layered block's overlay passes after its own faces, the way vanilla paints grass' green
	 * edge over plain dirt - except any block can declare any number of them from code. Each pass forces
	 * its sprite through the renderer's texture override and its tint through
	 * {@link com.periut.retroapi.register.block.RetroBlockLayerDraw}, so the ordinary block draw path does
	 * the work and no custom renderer is involved.
	 */
	@Inject(method = "render(Lnet/minecraft/block/Block;III)Z", at = @At("RETURN"), require = 0)
	private void retroapi$renderOverlayLayers(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (com.periut.retroapi.register.block.RetroBlockLayerDraw.drawing) {
			return; // we are inside an overlay pass ourselves
		}
		if (!Boolean.TRUE.equals(cir.getReturnValue())) {
			return; // nothing was drawn (fully occluded): nothing to overlay
		}
		com.periut.retroapi.register.block.RetroBlockAccess access =
			(com.periut.retroapi.register.block.RetroBlockAccess) block;
		if (!access.hasOverlayLayers()) {
			return;
		}
		java.util.List<com.periut.retroapi.register.block.RetroBlockLayer> layers = access.getOverlayLayers(
			com.periut.retroapi.state.RetroStates.get(this.blockView, x, y, z), this.blockView, x, y, z);
		if (layers.isEmpty()) {
			return;
		}
		BlockRenderManager self = (BlockRenderManager) (Object) this;
		int saved = this.textureOverride;
		try {
			for (com.periut.retroapi.register.block.RetroBlockLayer layer : layers) {
				int sprite = layer.spriteId();
				if (sprite < 0) continue;
				this.textureOverride = sprite;
				com.periut.retroapi.register.block.RetroBlockLayerDraw.begin(layer);
				self.render(block, x, y, z);
				com.periut.retroapi.register.block.RetroBlockLayerDraw.end();
			}
		} finally {
			com.periut.retroapi.register.block.RetroBlockLayerDraw.end();
			this.textureOverride = saved;
		}
	}

	/** The same overlay passes for the inventory/hand form, so a layered block looks right as an item. */
	@Inject(method = "render(Lnet/minecraft/block/Block;IF)V", at = @At("RETURN"), require = 0)
	private void retroapi$renderOverlayLayersAsItem(Block block, int metadata, float brightness, CallbackInfo ci) {
		if (com.periut.retroapi.register.block.RetroBlockLayerDraw.drawing) {
			return;
		}
		com.periut.retroapi.register.block.RetroBlockAccess access =
			(com.periut.retroapi.register.block.RetroBlockAccess) block;
		if (!access.hasOverlayLayers()) {
			return;
		}
		java.util.List<com.periut.retroapi.register.block.RetroBlockLayer> layers = access.getOverlayLayers(
			com.periut.retroapi.state.RetroStates.fromIndex(block, metadata), null, 0, 0, 0);
		if (layers.isEmpty()) {
			return;
		}
		BlockRenderManager self = (BlockRenderManager) (Object) this;
		int saved = this.textureOverride;
		try {
			for (com.periut.retroapi.register.block.RetroBlockLayer layer : layers) {
				int sprite = layer.spriteId();
				if (sprite < 0) continue;
				this.textureOverride = sprite;
				com.periut.retroapi.register.block.RetroBlockLayerDraw.begin(layer);
				self.render(block, metadata, brightness);
				com.periut.retroapi.register.block.RetroBlockLayerDraw.end();
			}
		} finally {
			com.periut.retroapi.register.block.RetroBlockLayerDraw.end();
			this.textureOverride = saved;
		}
	}
}

