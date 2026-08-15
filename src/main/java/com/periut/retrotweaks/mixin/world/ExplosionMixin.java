package com.periut.retrotweaks.mixin.world;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.util.PendingExplosionSource;

import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Controls what explosions destroy and how much they drop. From MiscTweaks.
 *
 * <p>Reworked: MiscTweaks decided this with static counters that creepers, TNT and ghasts each
 * incremented before exploding and this class decremented afterwards. Two explosions in the same
 * tick could take each other's counter, so a creeper could spare a TNT blast's blocks or vice
 * versa. {@code Explosion.source} already says who caused it for creepers ({@code
 * CreeperEntity.attack()} passes {@code this}), so that case is asked directly. TNT and ghast
 * fireballs are not so lucky: {@code TntEntity.explode()} and {@code FireballEntity.tick()} both
 * call {@code World.createExplosion(null, ...)}, so {@code source} is always null for them. {@code
 * retrotweaks$cause} fills that gap per-instance, via {@link PendingExplosionSource} - see there for
 * why that single slot does not reintroduce MiscTweaks' cross-explosion race.
 */
@Mixin(Explosion.class)
public class ExplosionMixin {

	@Shadow private World world;
	@Shadow public double x;
	@Shadow public double y;
	@Shadow public double z;
	@Shadow public Entity source;

	/** {@link #source}, falling back to {@link PendingExplosionSource} when vanilla left it null. */
	@Unique private Entity retrotweaks$cause;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void retrotweaks$captureCause(CallbackInfo ci) {
		this.retrotweaks$cause = this.source != null ? this.source : PendingExplosionSource.takeAndClear();
	}

	@Inject(method = "playExplosionSound", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$maybeSpareBlocks(boolean addParticles, CallbackInfo ci) {
		if (!retrotweaks$shouldSpareBlocks()) return;

		// The sound and particles are vanilla's; only the block destruction loop below them is
		// being skipped, so an explosion with block damage off still looks and sounds like one.
		if (Config.SOUNDS.explosionSoundWithoutBlocks) {
			this.world.playSound(this.x, this.y, this.z, "random.explode", 4.0F,
				(1.0F + (this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.2F) * 0.7F);
		}
		if (addParticles) {
			this.world.addParticle("explode", this.x, this.y, this.z, 0, 1, 0);
			this.world.addParticle("smoke", this.x, this.y, this.z, 0, 1, 0);
		}
		ci.cancel();
	}

	private boolean retrotweaks$shouldSpareBlocks() {
		Config.Explosions explosions = Config.WORLD.explosions;
		if (explosions.disableAllBlockBreaking) return true;
		if (this.retrotweaks$cause instanceof CreeperEntity) return explosions.disableCreeperBlockBreaking;
		if (this.retrotweaks$cause instanceof FireballEntity) return explosions.disableGhastBlockBreaking;
		if (this.retrotweaks$cause instanceof TntEntity) return explosions.disableTntBlockBreaking;
		return false;
	}

	/**
	 * The chance each destroyed block drops itself. Vanilla hard-codes 30%.
	 *
	 * <p>{@code ordinal = 1} matters: the method contains an earlier 0.3F that scales debris
	 * particle velocity, and MiscTweaks replaced both, so raising the drop chance also made the
	 * particles fly further.
	 */
	@ModifyConstant(method = "playExplosionSound", constant = @Constant(floatValue = 0.3F, ordinal = 1), require = 0)
	private float retrotweaks$blockDropChance(float vanilla) {
		float chance = Config.WORLD.explosions.blockDropChance;
		// dropStacks compares against a random float, so a negative luck value drops nothing at all.
		return chance > 0.0F ? chance : -1.0F;
	}
}
