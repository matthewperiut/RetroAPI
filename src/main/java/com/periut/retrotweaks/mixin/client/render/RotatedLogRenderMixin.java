package com.periut.retrotweaks.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.block.Block;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.world.BlockView;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws rotated logs, by turning each face's texture to match the log's axis.
 *
 * <p>b1.7.3's block renderer has no notion of a rotated cube, so render type 18 is claimed for one:
 * it renders as a normal cube with the per-face rotation fields set, then clears them again so no
 * other block inherits the rotation. From MiscTweaks.
 *
 * <p>Deliberately its own class rather than part of {@link BlockRenderManagerMixin}: that one holds
 * UniTweaks-derived fixes and is skipped wholesale when UniTweaks is installed, while log rotation is
 * RetroTweaks' own feature and {@code LogBlockMixin} keeps answering render type 18 regardless - so
 * this renderer has to stay applied or every log turns invisible (nothing draws type 18, but the
 * block is still opaque, so its neighbours cull their faces against it and the world x-rays). That
 * is exactly the breakage the two classes being one file caused in the field.
 *
 * <p>Safe alongside UniTweaks 0.29.0 by instruction-level check of its four BlockRenderManager
 * mixins: they wrap {@code renderSmooth}'s getLuminance calls, {@code render(Block;IF)V}'s
 * renderTopFace call and {@code renderTiltedTorch}'s vertices - none of which is an instruction
 * this class matches ({@code getRenderType} in {@code render(Block;IF)V}, {@code isSideLit}'s HEAD,
 * {@code render(Block;III)Z}'s TAIL).
 */
@Mixin(BlockRenderManager.class)
public abstract class RotatedLogRenderMixin {

	@Shadow private BlockView blockView;
	@Shadow private int eastFaceRotation;
	@Shadow private int westFaceRotation;
	@Shadow private int southFaceRotation;
	@Shadow private int northFaceRotation;
	@Shadow private int topFaceRotation;
	@Shadow private int bottomFaceRotation;

	@Shadow public abstract boolean renderBlock(Block block, int x, int y, int z);

	@Unique
	private static final int RETROTWEAKS_ROTATED_LOG_RENDER_TYPE = 18;

	/** The inventory/hand renderer has no rotation to apply, so it draws type 18 as a plain cube. */
	@WrapOperation(method = "render(Lnet/minecraft/block/Block;IF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getRenderType()I"))
	private int retrotweaks$renderAsCubeInHand(Block block, Operation<Integer> original) {
		int renderType = original.call(block);
		return renderType == RETROTWEAKS_ROTATED_LOG_RENDER_TYPE ? 0 : renderType;
	}

	@Inject(method = "isSideLit", at = @At("HEAD"), cancellable = true)
	private static void retrotweaks$rotatedLogsAreLit(int renderType, CallbackInfoReturnable<Boolean> cir) {
		if (renderType == RETROTWEAKS_ROTATED_LOG_RENDER_TYPE) cir.setReturnValue(true);
	}

	@Inject(method = "render(Lnet/minecraft/block/Block;III)Z", at = @At("TAIL"), cancellable = true)
	private void retrotweaks$renderRotatedLog(Block block, int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) return;
		if (block.getRenderType() != RETROTWEAKS_ROTATED_LOG_RENDER_TYPE) return;
		block.updateBoundingBox(this.blockView, x, y, z);
		cir.setReturnValue(retrotweaks$renderLog(block, x, y, z));
	}

	@Unique
	private boolean retrotweaks$renderLog(Block block, int x, int y, int z) {
		switch (this.blockView.getBlockMeta(x, y, z) & 0xC) {
			case 0x4 -> {
				this.southFaceRotation = 1;
				this.northFaceRotation = 2;
			}
			case 0x8 -> {
				this.eastFaceRotation = 2;
				this.westFaceRotation = 1;
				this.topFaceRotation = 1;
				this.bottomFaceRotation = 2;
			}
			default -> { /* upright: vanilla orientation */ }
		}

		this.renderBlock(block, x, y, z);

		// Cleared unconditionally: these fields are shared by every block the renderer touches.
		this.eastFaceRotation = 0;
		this.westFaceRotation = 0;
		this.southFaceRotation = 0;
		this.northFaceRotation = 0;
		this.topFaceRotation = 0;
		this.bottomFaceRotation = 0;
		return true;
	}
}
