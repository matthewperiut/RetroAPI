package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.util.PendingExplosionSource;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops ghast fireballs starting fires, without stopping the explosion. From MiscTweaks.
 *
 * <p>Whether the explosion also breaks blocks is decided in {@code ExplosionMixin}. {@code
 * createExplosion} is called with {@code source = null} here (vanilla), so this method also hands
 * the fireball itself to {@code ExplosionMixin} via {@link PendingExplosionSource} - see there for
 * why that is not the shared counter this mod deliberately moved away from.
 */
@Mixin(FireballEntity.class)
public class FireballEntityMixin {

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZ)Lnet/minecraft/world/explosion/Explosion;"))
	private Explosion retrotweaks$fireballExplosion(World world, Entity source, double x, double y, double z,
			float power, boolean fire, Operation<Explosion> original) {
		boolean startsFire = fire && Config.WORLD.fire.ghastExplosionsCauseFire;
		PendingExplosionSource.set((Entity) (Object) this);
		return original.call(world, source, x, y, z, power, startsFire);
	}
}
