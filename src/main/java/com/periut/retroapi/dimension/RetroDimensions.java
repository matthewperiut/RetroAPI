package com.periut.retroapi.dimension;

import net.minecraft.world.dimension.Dimension;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.function.IntFunction;

/**
 * Consumer-facing facade for registering modded dimensions, inside a
 * {@link com.periut.retroapi.dimension.event.DimensionRegistrationCallback} listener:
 *
 * <pre>{@code
 * RetroDimensions.register(id("the_aether"), AetherDimension::new);
 * }</pre>
 *
 * <p>The factory receives the assigned serial id (an {@code IntFunction<Dimension>}, mirroring
 * StationAPI's {@code DimensionContainer}). API-compatible in spirit with StationAPI so a mod barely
 * changes when migrating. Once registered, {@link com.periut.retroapi.mixin.dimension.DimensionMixin}
 * makes the serial id resolvable via vanilla {@code Dimension.fromId}.
 */
public final class RetroDimensions {
	private RetroDimensions() {}

	public static DimensionRegistration register(NamespacedIdentifier id, IntFunction<Dimension> factory) {
		return RetroDimensionRegistry.register(new DimensionRegistration(id, factory));
	}
}
