package com.periut.retrotweaks.feature.vehicle;

import net.minecraft.nbt.NbtCompound;

/**
 * Implemented by {@code PlayerEntity} through a mixin, so every player carries a record of the
 * vehicle they were riding, surviving logout/relog. From AnnoyanceFix and UniTweaksTelsAddons
 * (both ship the same feature under their own vehicle-logout-login-fix mixins).
 *
 * <p>An interface on the entity, following how {@link com.periut.retrotweaks.feature.scoring.ScoreHolder}
 * is attached to {@code PlayerEntity} in this codebase, rather than a map keyed by player: the
 * record then lives and dies with the player object and is saved and loaded with it.
 */
public interface VehicleHolder {

	String retrotweaks$vehicleName();

	void retrotweaks$vehicleName(String vehicleName);

	NbtCompound retrotweaks$vehicleTag();

	void retrotweaks$vehicleTag(NbtCompound vehicleTag);
}
