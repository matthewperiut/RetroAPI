package com.periut.retroapi.dimension;

/**
 * Optionally implemented by a {@link net.minecraft.world.dimension.Dimension} subclass to supply the
 * translation keys shown while travelling into/out of it (the loading-screen message). When a
 * dimension does not implement this, the client shows an empty message. API-compatible with
 * StationAPI's {@code TravelMessageProvider}.
 */
public interface TravelMessageProvider {
	String getEnteringTranslationKey();

	String getLeavingTranslationKey();
}
