package com.periut.retroapi.mixin.client;

import com.periut.retroapi.register.rendertype.BlockRenderContext;
import com.periut.retroapi.register.rendertype.CustomBlockRenderer;
import com.periut.retroapi.register.rendertype.RetroBlockRendererAccess;
import com.periut.retroapi.register.rendertype.RenderType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockRenderer;
import net.minecraft.client.render.vertex.Tesselator;
import net.minecraft.world.WorldView;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockRenderer.class)
@Environment(EnvType.CLIENT)
public class BlockRendererMixin implements RetroBlockRendererAccess {
	@Shadow private WorldView world;
#if MC_VER >= 140
	@Shadow private boolean ambientOcclusion;
	@Shadow private float vertex1R;
	@Shadow private float vertex1G;
	@Shadow private float vertex1B;
	@Shadow private float vertex2R;
	@Shadow private float vertex2G;
	@Shadow private float vertex2B;
	@Shadow private float vertex3R;
	@Shadow private float vertex3G;
	@Shadow private float vertex3B;
	@Shadow private float vertex4R;
	@Shadow private float vertex4G;
	@Shadow private float vertex4B;
#endif

	@Override
	public void retroapi$setupSmoothFace(float v1, float v2, float v3, float v4, float shade) {
#if MC_VER >= 140
		this.ambientOcclusion = true;
		this.vertex1R = this.vertex1G = this.vertex1B = shade * v1;
		this.vertex2R = this.vertex2G = this.vertex2B = shade * v2;
		this.vertex3R = this.vertex3G = this.vertex3B = shade * v3;
		this.vertex4R = this.vertex4G = this.vertex4B = shade * v4;
#endif
	}

	@Override
	public void retroapi$cleanupSmoothFace() {
#if MC_VER >= 140
		this.ambientOcclusion = false;
#endif
	}

	// --- Custom render type handling ---

	@Inject(method = "tesselateInWorld", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleCustomRenderType(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		int type = block.getRenderType();
		if (RenderType.isCustom(type)) {
			CustomBlockRenderer renderer = RenderType.getRenderer(type);
			if (renderer != null) {
				block.updateShape(this.world, x, y, z);
				BlockRenderContext ctx = new BlockRenderContext(
					(BlockRenderer) (Object) this, block, x, y, z, this.world
				);
				cir.setReturnValue(renderer.render(ctx));
			}
		}
	}

	@Inject(method = "renderAsItem", at = @At("HEAD"), cancellable = true)
#if MC_VER >= 170
	private void retroapi$handleCustomRenderAsItem(Block block, int metadata, float brightness, CallbackInfo ci) {
#elif MC_VER >= 120
	private void retroapi$handleCustomRenderAsItem(Block block, int metadata, CallbackInfo ci) {
#else
	private void retroapi$handleCustomRenderAsItem(Block block, CallbackInfo ci) {
#endif
#if MC_VER < 120
		int metadata = 0;
#endif
		int type = block.getRenderType();
		if (RenderType.isCustom(type)) {
			block.resetShape();
			GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
			Tesselator tesselator = Tesselator.INSTANCE;
			BlockRenderer self = (BlockRenderer) (Object) this;

			tesselator.begin();
			tesselator.normal(0.0F, -1.0F, 0.0F);
			self.tesselateBottomFace(block, 0.0, 0.0, 0.0, block.getSprite(0, metadata));
			tesselator.end();

			tesselator.begin();
			tesselator.normal(0.0F, 1.0F, 0.0F);
			self.tesselateTopFace(block, 0.0, 0.0, 0.0, block.getSprite(1, metadata));
			tesselator.end();

			tesselator.begin();
			tesselator.normal(0.0F, 0.0F, -1.0F);
			self.tesselateNorthFace(block, 0.0, 0.0, 0.0, block.getSprite(2, metadata));
			tesselator.end();

			tesselator.begin();
			tesselator.normal(0.0F, 0.0F, 1.0F);
			self.tesselateSouthFace(block, 0.0, 0.0, 0.0, block.getSprite(3, metadata));
			tesselator.end();

			tesselator.begin();
			tesselator.normal(-1.0F, 0.0F, 0.0F);
			self.tesselateWestFace(block, 0.0, 0.0, 0.0, block.getSprite(4, metadata));
			tesselator.end();

			tesselator.begin();
			tesselator.normal(1.0F, 0.0F, 0.0F);
			self.tesselateEastFace(block, 0.0, 0.0, 0.0, block.getSprite(5, metadata));
			tesselator.end();

			GL11.glTranslatef(0.5F, 0.5F, 0.5F);
			ci.cancel();
		}
	}

	@Inject(method = "isItem3d", at = @At("HEAD"), cancellable = true)
	private static void retroapi$customIsItem3d(int type, CallbackInfoReturnable<Boolean> cir) {
		if (RenderType.isCustom(type)) {
			cir.setReturnValue(true);
		}
	}
}
