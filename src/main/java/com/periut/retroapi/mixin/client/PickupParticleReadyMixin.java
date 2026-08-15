package com.periut.retroapi.mixin.client;

import net.minecraft.client.particle.PickupParticle;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.entity.EntityRenderDispatcher;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops the item-pickup animation drawing before the entity renderers know where their textures are.
 *
 * <p>{@code PickupParticle.render} draws the collected item through
 * {@code EntityRenderDispatcher.INSTANCE}, and the only thing that ever fills that dispatcher's
 * {@code textureManager} in is {@code WorldRenderer.renderEntities} - which skips it entirely for the
 * first two frames of a world:
 *
 * <pre>
 * if (this.entityRenderCooldown &gt; 0) {
 *     this.entityRenderCooldown--;
 * } else {
 *     ...INSTANCE.init(this.world, this.textureManager, ...);
 * </pre>
 *
 * <p>{@code GameRenderer} calls {@code particleManager.renderLit} on the line after, cooldown or not,
 * so a pickup particle alive in either of those frames renders through a dispatcher whose
 * {@code textureManager} is still null and takes the game down inside {@code EntityRenderer.bindTexture}.
 *
 * <p>Vanilla can only reach this by picking an item up within two frames of a world appearing, which
 * is not a thing a player can do - but a mod that hands items over on join does it every single time.
 *
 * <p>Skipping the draw rather than initialising the dispatcher here: the frames in question are the
 * ones where the world is not on screen yet, this particle lives for three ticks, and any of it that
 * is missed is behind a loading screen. Guessing at an initialisation {@code WorldRenderer} has
 * deliberately deferred would be a much larger claim than the bug needs.
 */
@Mixin(PickupParticle.class)
public class PickupParticleReadyMixin {

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void retroapi$skipUntilRenderersReady(Tessellator tessellator, float partialTicks,
			float horizontalSize, float verticalSize, float depthSize, float widthOffset, float heightOffset,
			CallbackInfo ci) {
		if (EntityRenderDispatcher.INSTANCE == null || EntityRenderDispatcher.INSTANCE.textureManager == null) {
			ci.cancel();
		}
	}
}
