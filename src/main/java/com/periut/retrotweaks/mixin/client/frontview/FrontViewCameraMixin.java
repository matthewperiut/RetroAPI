package com.periut.retrotweaks.mixin.client.frontview;

import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Front-facing third person camera. Ported from UniTweaks.
 *
 * <p>Three hooks, and all three are needed:
 *
 * <ul>
 *   <li>the yaw the camera offset is derived from gains half a turn, which moves the camera to the
 *       far side of the player;</li>
 *   <li>an extra rotation turns the view round so it looks back at the player - without this the
 *       camera sits in front but still faces forwards, and the player is behind the lens;</li>
 *   <li>the wall-collision raycast has its end point mirrored about the camera's height, because
 *       that check is still aimed with the un-flipped pitch and would otherwise pull the camera in
 *       against the wrong surface.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class FrontViewCameraMixin {

	@Shadow private Minecraft client;

	@Unique
	private boolean retrotweaks$frontView() {
		return ModOptions.isFrontView(this.client);
	}

	/** Mirrors a point's height about {@code aroundY}. */
	@Unique
	private static Vec3d retrotweaks$flipY(Vec3d vec, double aroundY) {
		return Vec3d.create(vec.x, aroundY + (aroundY - vec.y), vec.z);
	}

	@WrapOperation(method = "applyCameraTransform", at = @At(value = "FIELD",
		target = "Lnet/minecraft/entity/LivingEntity;yaw:F", ordinal = 1, opcode = Opcodes.GETFIELD))
	private float retrotweaks$offsetToTheFront(LivingEntity camera, Operation<Float> original) {
		float yaw = original.call(camera);
		return retrotweaks$frontView() ? yaw + 180.0F : yaw;
	}

	@Inject(method = "applyCameraTransform", at = @At(value = "INVOKE",
		target = "Lorg/lwjgl/opengl/GL11;glRotatef(FFFF)V", ordinal = 6, remap = false))
	private void retrotweaks$lookBack(float tickDelta, CallbackInfo ci) {
		if (retrotweaks$frontView()) GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
	}

	@WrapOperation(method = "applyCameraTransform", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/World;raycast(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/hit/HitResult;"))
	private HitResult retrotweaks$mirrorCollisionRay(World world, Vec3d start, Vec3d end, Operation<HitResult> original) {
		if (!retrotweaks$frontView()) return original.call(world, start, end);
		return original.call(world, start, retrotweaks$flipY(end, this.client.camera.y));
	}
}
