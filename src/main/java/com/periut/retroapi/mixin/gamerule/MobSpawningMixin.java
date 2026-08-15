package com.periut.retroapi.mixin.gamerule;

import com.periut.retroapi.gamerule.RetroGameRules;
import net.minecraft.world.NaturalSpawner;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code doMobSpawning}: the natural spawner does nothing at all. */
@Mixin(NaturalSpawner.class)
public class MobSpawningMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private static void retroapi$doMobSpawning(World world, boolean spawnMonsters, boolean spawnAnimals,
			CallbackInfoReturnable<Integer> cir) {
		if (!RetroGameRules.getBoolean(RetroGameRules.DO_MOB_SPAWNING)) {
			cir.setReturnValue(0);
		}
	}
}
