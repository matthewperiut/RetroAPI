package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.options.ModKeyBindings;
import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The FOV offset (Graphics-tab slider, and the vanilla screen while {@code fovOverride} is on) and
 * the Zoom key. From UniTweaks (mixin.tweaks.fov.GameRendererMixin).
 *
 * <p>Kept in its own file, separate from this class's other mixin(s), so it does not collide with
 * unrelated {@code GameRenderer} work landing around the same time (see {@code FpsLimitMixin} and
 * the sleeping-camera-rotation fix) - Mixin is happy to have several mixin classes target the same
 * vanilla class as long as each is registered.
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererFovMixin {

	@Shadow
	private Minecraft client;

	@Shadow
	private float viewDistance;

	@Shadow
	protected abstract float getFov(float tickDelta);

	@Unique
	private float fov = 70F;

	@Unique
	private float fovZoom = 0F;

	@Unique
	private boolean zoomedIn = false;

	/**
	 * Applies the slider offset to whatever vanilla already computed for the frame, then - while the
	 * Zoom key is held - divides it down for a telescope-style zoom and lets scrolling nudge the zoom
	 * level further. {@code isHand} takes a shallower zoom-in (a flat 50 degree cut) so the held item
	 * does not clip into the camera.
	 */
	@Unique
	private float getFovMultiplier(float partialTicks, boolean isHand, float originalValue) {
		fov = isHand ? originalValue : originalValue + ModOptions.fovOffsetDegrees();

		if (Keyboard.isKeyDown(ModKeyBindings.ZOOM.code) && client.currentScreen == null) {
			if (!zoomedIn) {
				ModOptions.zoomFovOffset = 0;
				fovZoom = 0F;
				zoomedIn = true;
			}

			if (!isHand) {
				fov /= 4F;

				fovZoom += ModOptions.zoomFovOffset * 5;
				ModOptions.zoomFovOffset = 0;

				fovZoom = Math.min(130F - fov, Math.max(5F - fov, fovZoom));
			} else {
				fov -= 50F;
			}

			fov += fovZoom;
		} else {
			zoomedIn = false;
		}

		return fov;
	}

	/** Lets the cinematic-mode mouse smoother take over while zooming too, for a steadier zoom. */
	@ModifyExpressionValue(method = "onFrameUpdate", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
		target = "Lnet/minecraft/client/option/GameOptions;cinematicMode:Z"), require = 1)
	private boolean retrotweaks$smoothCameraWhenZooming(boolean original) {
		if (!Config.GRAPHICS.video.fovOverride) {
			return original;
		}

		return original || Keyboard.isKeyDown(ModKeyBindings.ZOOM.code);
	}

	@WrapOperation(method = "renderWorld", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/GameRenderer;getFov(F)F"))
	private float retrotweaks$redirectToCustomFov(GameRenderer instance, float value, Operation<Float> originalOperation) {
		float original = originalOperation.call(instance, value);

		if (Config.GRAPHICS.video.fovOverride) {
			return getFovMultiplier(value, false, original);
		}

		return original;
	}

	@Inject(method = "renderFirstPersonHand", at = @At(value = "HEAD"))
	private void retrotweaks$adjustHandFov(float f, int i, CallbackInfo ci) {
		if (Config.GRAPHICS.video.fovOverride) {
			GL11.glMatrixMode(GL11.GL_PROJECTION);
			GL11.glLoadIdentity();
			GLU.gluPerspective(getFovMultiplier(f, true, this.getFov(f)), (float) client.displayWidth / (float) client.displayHeight, 0.05F, viewDistance * 2.0F);
			GL11.glMatrixMode(GL11.GL_MODELVIEW);
		}
	}
}
