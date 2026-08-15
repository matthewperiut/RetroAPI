package com.periut.retrotweaks.feature.vehicle;

/**
 * Bridges a player's {@code readNbt} (which recreates a saved vehicle but, in singleplayer, may run
 * before that vehicle's chunk has actually loaded the old entity back in) to the world's
 * {@code spawnEntity} hook that completes the remount once a matching entity does appear.
 *
 * <p>Ported from AnnoyanceFix's {@code ModHelper.ModHelperFields} (isVehicleSaved/savedVehicleClass/
 * savedVehicleX/Y/Z) and UniTweaksTelsAddons's identical {@code VehicleHelper} - both mods carry the
 * exact same static fields for the exact same purpose, just under different class names.
 */
public final class VehicleState {

	private VehicleState() {}

	public static boolean isVehicleSaved = false;
	public static Class<?> savedVehicleClass = null;
	public static double savedVehicleX = 0.0D;
	public static double savedVehicleY = 0.0D;
	public static double savedVehicleZ = 0.0D;
}
