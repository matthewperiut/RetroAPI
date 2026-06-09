package com.periut.retroapi.dimension;

import net.minecraft.world.dimension.Dimension;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.function.IntFunction;

/**
 * A registered modded dimension: a stable string identifier, the assigned numeric serial id (baked
 * into the {@code DIM<n>/} save folder + player NBT + the wire), and a factory that builds the
 * {@link Dimension} instance for that serial id. Plain holder, mirrors
 * {@link com.periut.retroapi.entity.EntityRegistration}.
 *
 * <p>The factory receives the serial id (mirrors StationAPI's {@code DimensionContainer} {@code IntFunction}
 * ctor; e.g. Aether's {@code AetherDimension} takes an {@code int serialId} and assigns it to {@code id}).
 */
public final class DimensionRegistration {
	private final NamespacedIdentifier id;
	private final IntFunction<Dimension> factory;
	private int serialId;

	public DimensionRegistration(NamespacedIdentifier id, IntFunction<Dimension> factory) {
		this.id = id;
		this.factory = factory;
	}

	public NamespacedIdentifier getId() { return id; }
	public IntFunction<Dimension> getFactory() { return factory; }
	public int getSerialId() { return serialId; }
	public void setSerialId(int serialId) { this.serialId = serialId; }

	/** Build a fresh Dimension instance carrying this registration's serial id. */
	public Dimension create() { return factory.apply(serialId); }
}
