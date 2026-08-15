package com.periut.retrotweaks.mixin.client.particle;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.render.WorldRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Turns individual particle types off, for performance or for taste. From UniTweaksTelsAddons.
 *
 * <p>Reworked: the original hung sixteen separate injections off the ordinal of each
 * {@code addParticle} call inside the method, so inserting a particle type anywhere in vanilla's
 * if-chain would silently shift every option onto the wrong particle. The particle's name is
 * already the method's first argument, so one hook that switches on it says the same thing and
 * cannot slip.
 */
@Mixin(WorldRenderer.class)
public class ParticleDisablerMixin {

	@Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$filterParticles(String particle, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
		if (retrotweaks$isDisabled(particle)) ci.cancel();
	}

	private static boolean retrotweaks$isDisabled(String particle) {
		Config.Particles particles = Config.PARTICLES;
		if (particles.disableAll) return true;
		return switch (particle) {
			case "bubble" -> particles.bubble;
			case "smoke" -> particles.smoke;
			case "largesmoke" -> particles.largeSmoke;
			case "note" -> particles.note;
			case "portal" -> particles.portal;
			case "explode" -> particles.explosion;
			case "flame" -> particles.flame;
			case "lava" -> particles.lava;
			case "footstep" -> particles.footstep;
			case "splash" -> particles.splash;
			case "reddust" -> particles.redDust;
			case "snowballpoof" -> particles.snowball;
			case "snowshovel" -> particles.snowShovel;
			case "slime" -> particles.slime;
			case "heart" -> particles.heart;
			default -> false;
		};
	}
}
