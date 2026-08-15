package com.periut.retrotweaks.mixin.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3i;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The private respawn point, so a bed can set it without the player actually sleeping. */
@Mixin(PlayerEntity.class)
public interface PlayerSpawnPosAccessor {

	@Accessor("spawnPos")
	void retrotweaks$setSpawnPos(Vec3i pos);
}
