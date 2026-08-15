package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;

import net.minecraft.entity.Entity;
import net.minecraft.entity.FlyingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;

/**
 * A ghast's own fireball kills it outright, so deflecting one is a real kill. From MiscTweaks.
 *
 * <p>Reworked: MiscTweaks copied the whole of {@code damage} into an override to change one number,
 * which would silently drift from vanilla and stamp on any other mod touching ghast damage. Raising
 * the damage before handing it to vanilla does the same job in one line.
 */
@Mixin(GhastEntity.class)
public abstract class GhastEntityMixin extends FlyingEntity {

	protected GhastEntityMixin(World world) {
		super(world);
	}

	@Override
	public boolean damage(Entity attacker, int amount) {
		if (Config.MOBS.ghastFireballsKillGhasts
			&& (attacker == null || attacker instanceof GhastEntity || attacker instanceof FireballEntity)) {
			// A ghast has 10 health; 20 is comfortably lethal whatever else has hit it.
			amount = 20;
		}
		return super.damage(attacker, amount);
	}
}
