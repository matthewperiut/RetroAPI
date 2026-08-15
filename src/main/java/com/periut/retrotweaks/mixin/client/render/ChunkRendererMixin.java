package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.render.world.ChunkRenderer;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps chunk offsets in double precision so terrain stops shaking far from spawn. From UniTweaks.
 *
 * <p>Vanilla narrows the camera offset to a float before translating, and a float runs out of
 * precision well before the world border - which is what makes the Far Lands (and anywhere past a
 * few hundred thousand blocks) visibly jitter.
 */
@Mixin(ChunkRenderer.class)
public class ChunkRendererMixin {

	@Shadow private int x;
	@Shadow private int y;
	@Shadow private int z;

	@Unique private double retrotweaks$offsetX;
	@Unique private double retrotweaks$offsetY;
	@Unique private double retrotweaks$offsetZ;

	@Inject(method = "init", at = @At("HEAD"))
	private void retrotweaks$keepDoubleOffsets(int x, int y, int z, double offsetX, double offsetY, double offsetZ, CallbackInfo ci) {
		this.retrotweaks$offsetX = offsetX;
		this.retrotweaks$offsetY = offsetY;
		this.retrotweaks$offsetZ = offsetZ;
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V", remap = false))
	private void retrotweaks$translateInDoubles(float x, float y, float z, Operation<Void> original) {
		if (!Config.BUGFIXES.farLandsJitterFix) {
			original.call(x, y, z);
			return;
		}
		GL11.glTranslated(this.x - retrotweaks$offsetX, this.y - retrotweaks$offsetY, this.z - retrotweaks$offsetZ);
	}
}
