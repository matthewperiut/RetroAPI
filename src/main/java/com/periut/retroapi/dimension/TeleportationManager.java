package com.periut.retroapi.dimension;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Drives a single dimension transition for a player. A portal block (or other trigger) implements
 * this (usually via {@link CustomPortal}) and is attached to the player through
 * {@link HasTeleportationManager}; the dimension-change mixins invoke {@link #switchDimension} when
 * the player's portal timer fires. API-compatible with StationAPI's same-named interface so a
 * consuming mod barely changes when migrating.
 */
public interface TeleportationManager {
	void switchDimension(PlayerEntity player);
}
