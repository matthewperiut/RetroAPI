package com.periut.retroapi.client.particle;

import com.periut.retroapi.client.model.RetroAtlas;
import com.periut.retroapi.register.block.RetroTexture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Tessellator;
import net.minecraft.world.World;

/**
 * A ready-made particle that draws one registered RetroAPI block texture - so "I want my own particle"
 * does not have to mean "I want to write a {@code Particle} subclass".
 *
 * <p>It behaves like vanilla's smoke/flame particles (drifts, ages out, optionally falls), but takes its
 * sprite from RetroAPI's expanded terrain atlas, so any {@code RetroTextures.addBlockTexture(...)} handle
 * works - including one that no block uses. Everything is chainable:
 *
 * <pre>
 * new RetroSpriteParticle(world, x, y, z, vx, vy, vz, MyMod.SPARK_TEXTURE)
 *     .lifetime(24)        // ticks before it disappears (a little randomised)
 *     .scale(0.9F)         // 1.0 is roughly a vanilla smoke puff
 *     .gravity(0.02F)      // 0 floats, positive falls, negative rises
 *     .tint(0xFFD070)      // 0xRRGGBB multiply
 *     .drag(0.96F);        // velocity kept per tick
 * </pre>
 *
 * <p>Unlike vanilla's digging particle it draws the WHOLE sprite, not a random quarter of it, so small
 * pixel art reads correctly.
 */
@Environment(EnvType.CLIENT)
public class RetroSpriteParticle extends Particle {

	/** Terrain-atlas render group; vanilla binds {@code terrain.png} for this one. */
	private static final int TERRAIN_GROUP = 1;

	private final RetroTexture texture;
	private float drag = 0.98F;
	private float baseScale = 1.0F;

	public RetroSpriteParticle(World world, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ, RetroTexture texture) {
		super(world, x, y, z, velocityX, velocityY, velocityZ);
		this.texture = texture;
		// Vanilla's Particle constructor adds its own random jitter to the velocity; keep exactly what
		// the caller asked for, since spawn code that wants scatter can ask for it (RetroParticles.spawnCloud).
		this.velocityX = velocityX;
		this.velocityY = velocityY;
		this.velocityZ = velocityZ;
		this.gravityStrength = 0.0F;
		this.maxParticleAge = 20;
		this.red = this.green = this.blue = 1.0F;
		this.textureId = texture != null ? texture.id : 0;
	}

	// --- chainable setup --------------------------------------------------------------------------

	/** Ticks to live, ±25% so a burst does not vanish all at once. */
	public RetroSpriteParticle lifetime(int ticks) {
		int jitter = Math.max(1, ticks / 4);
		this.maxParticleAge = Math.max(1, ticks - jitter / 2 + this.world.random.nextInt(jitter + 1));
		return this;
	}

	/** Exactly this many ticks, no randomisation. */
	public RetroSpriteParticle exactLifetime(int ticks) {
		this.maxParticleAge = Math.max(1, ticks);
		return this;
	}

	public RetroSpriteParticle scale(float scale) {
		this.baseScale = scale;
		this.scale = scale;
		return this;
	}

	/** Downward acceleration per tick; 0 floats, negative rises. */
	public RetroSpriteParticle gravity(float gravity) {
		this.gravityStrength = gravity;
		return this;
	}

	/** Fraction of velocity kept each tick (1.0 keeps moving forever, 0.9 stops quickly). */
	public RetroSpriteParticle drag(float drag) {
		this.drag = drag;
		return this;
	}

	/** {@code 0xRRGGBB} color multiply. */
	public RetroSpriteParticle tint(int rgb) {
		this.red = ((rgb >> 16) & 0xFF) / 255.0F;
		this.green = ((rgb >> 8) & 0xFF) / 255.0F;
		this.blue = (rgb & 0xFF) / 255.0F;
		return this;
	}

	/** Shrinks to nothing over its life instead of vanishing at full size. */
	public RetroSpriteParticle shrink() {
		this.shrinking = true;
		return this;
	}

	private boolean shrinking;

	// --- behavior --------------------------------------------------------------------------------

	@Override
	public void tick() {
		this.prevX = this.x;
		this.prevY = this.y;
		this.prevZ = this.z;

		if (this.particleAge++ >= this.maxParticleAge) {
			this.markDead();
			return;
		}
		this.velocityY -= 0.04D * this.gravityStrength;
		this.move(this.velocityX, this.velocityY, this.velocityZ);
		this.velocityX *= this.drag;
		this.velocityY *= this.drag;
		this.velocityZ *= this.drag;
		if (this.onGround) {
			this.velocityX *= 0.7D;
			this.velocityZ *= 0.7D;
		}
		if (this.shrinking) {
			float remaining = 1.0F - (float) this.particleAge / this.maxParticleAge;
			this.scale = this.baseScale * remaining;
		}
		// The sprite index can move when the atlas is rebuilt (resource pack change); re-read it.
		if (this.texture != null) {
			this.textureId = this.texture.id;
		}
	}

	@Override
	public void render(Tessellator tessellator, float tickDelta, float rotX, float rotXZ, float rotZ,
			float rotYZ, float rotXY) {
		// UVs come from RetroAPI's atlas strategy, so this draws correctly on the expanded vanilla atlas
		// AND on StationAPI's dynamically sized one - the whole sprite, not a quarter of it.
		double minU = RetroAtlas.terrainU(this.textureId, 0.2);
		double maxU = RetroAtlas.terrainU(this.textureId, 15.8);
		double minV = RetroAtlas.terrainV(this.textureId, 0.2);
		double maxV = RetroAtlas.terrainV(this.textureId, 15.8);
		float size = 0.1F * this.scale;

		float px = (float) (this.prevX + (this.x - this.prevX) * tickDelta - xOffset);
		float py = (float) (this.prevY + (this.y - this.prevY) * tickDelta - yOffset);
		float pz = (float) (this.prevZ + (this.z - this.prevZ) * tickDelta - zOffset);

		tessellator.color(this.red, this.green, this.blue, 1.0F);
		tessellator.vertex(px - rotX * size - rotYZ * size, py - rotXZ * size, pz - rotZ * size - rotXY * size,
			maxU, maxV);
		tessellator.vertex(px - rotX * size + rotYZ * size, py + rotXZ * size, pz - rotZ * size + rotXY * size,
			maxU, minV);
		tessellator.vertex(px + rotX * size + rotYZ * size, py + rotXZ * size, pz + rotZ * size + rotXY * size,
			minU, minV);
		tessellator.vertex(px + rotX * size - rotYZ * size, py - rotXZ * size, pz + rotZ * size - rotXY * size,
			minU, maxV);
	}

	@Override
	public int getGroup() {
		return TERRAIN_GROUP;
	}
}
