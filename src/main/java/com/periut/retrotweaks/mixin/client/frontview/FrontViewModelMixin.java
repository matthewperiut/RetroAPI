package com.periut.retrotweaks.mixin.client.frontview;

import com.periut.retrotweaks.feature.options.ModOptions;
import com.periut.retrotweaks.mixin.client.MinecraftAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns entity models to match the flipped camera. From UniTweaks.
 *
 * <p>Without this the camera swings round to the front but every model keeps facing as though it
 * were still behind, so the player is seen from the back while the world is seen from the front.
 * The dispatcher's yaw and pitch are what every renderer orients against, so flipping them here
 * covers the player and everything else in one place.
 *
 * <p>Note the full parameter list: {@code init} takes six arguments, and Mixin will not attach an
 * injector whose descriptor does not match.
 */
@Environment(EnvType.CLIENT)
@Mixin(EntityRenderDispatcher.class)
public class FrontViewModelMixin {

	@Shadow public GameOptions options;
	@Shadow public float yaw;
	@Shadow public float pitch;

	@Inject(method = "init", at = @At("TAIL"))
	private void retrotweaks$flipModelAngles(World world, TextureManager textureManager, TextRenderer textRenderer,
			LivingEntity camera, GameOptions options, float tickDelta, CallbackInfo ci) {
		if (this.options == null) return;
		if (!ModOptions.isFrontView(MinecraftAccessor.retrotweaks$getInstance())) return;
		this.yaw += 180.0F;
		this.pitch = -this.pitch;
	}
}
