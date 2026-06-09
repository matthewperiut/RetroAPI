package com.periut.retroapi.mixin.entity;

import com.periut.retroapi.entity.RetroSpawnGroups;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds modded mobs to the per-category count {@code NaturalSpawner} uses for the spawn
 * cap, so a custom mob extending {@code LivingEntity} (not a vanilla creature base) still
 * stops spawning once its category is full. See {@link RetroSpawnGroups}.
 */
@Mixin(World.class)
public class WorldSpawnCountMixin {

	@Inject(method = "countEntities", at = @At("RETURN"), cancellable = true)
	private void retroapi$countModdedMobs(Class<?> creatureClass, CallbackInfoReturnable<Integer> cir) {
		int extra = RetroSpawnGroups.extraCount((World) (Object) this, creatureClass);
		if (extra > 0) {
			cir.setReturnValue(cir.getReturnValue() + extra);
		}
	}
}
