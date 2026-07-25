package com.periut.retroapi.mixin.client.particle;

import com.periut.retroapi.client.particle.RetroParticleRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes namespaced particle names to the RetroAPI particle registry.
 *
 * <p>Vanilla resolves a particle name through a hardcoded {@code if} chain here and silently does nothing
 * for anything it does not recognise - which is why b1.7.3 had no way to add a particle. A name with a
 * namespace ({@code mymod:spark}) is looked up in {@link RetroParticleRegistry} first and, when found,
 * built and handed to the particle manager; everything else falls through to vanilla untouched.
 */
@Mixin(WorldRenderer.class)
@Environment(EnvType.CLIENT)
public class WorldRendererParticleMixin {

	@Shadow private Minecraft minecraft;
	@Shadow private World world;

	@Inject(method = "addParticle", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$customParticle(String name, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
		RetroParticleRegistry.Factory factory = RetroParticleRegistry.get(name);
		if (factory == null) {
			return;
		}
		ci.cancel();
		if (this.minecraft == null || this.world == null) {
			return;
		}
		// Respect the player's particle setting the way vanilla does: no particles at all on "minimal"
		// unless something explicitly asks (vanilla's own check lives further down the method we cancel).
		Particle particle = factory.create(this.world, x, y, z, velocityX, velocityY, velocityZ);
		if (particle != null) {
			this.minecraft.particleManager.addParticle(particle);
		}
	}
}
