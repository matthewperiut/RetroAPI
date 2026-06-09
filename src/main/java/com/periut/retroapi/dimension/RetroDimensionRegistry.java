package com.periut.retroapi.dimension;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * In-RAM registry of modded dimensions (mirrors {@link com.periut.retroapi.registry.RetroRegistry}).
 * Vanilla ids {@code -1/0/1} (Nether/Overworld/Skylands) are RESERVED and never registered here -
 * RetroAPI must never override them. Modded serial ids are assigned starting at {@code 2}, ascending.
 *
 * <p>NOTE: serial ids here are assigned in registration order this session. Stable-across-reopen
 * persistence (so the same identifier always maps to the same {@code DIM<n>/} folder + player-NBT id)
 * is a later increment (a {@code DimensionIdAssigner} mirroring {@code IdAssigner}). Until then a world
 * with a single modded dimension is stable because registration order is deterministic.
 */
public final class RetroDimensionRegistry {
	private RetroDimensionRegistry() {}

	public static final int OVERWORLD_ID = 0;
	public static final int NETHER_ID = -1;
	public static final int SKYLANDS_ID = 1;
	private static final int FIRST_MODDED_ID = 2;

	private static final List<DimensionRegistration> DIMENSIONS = new ArrayList<>();
	private static int nextSerialId = FIRST_MODDED_ID;

	public static DimensionRegistration register(DimensionRegistration reg) {
		DimensionRegistration existing = getByIdentifier(reg.getId());
		if (existing != null) return existing;
		reg.setSerialId(nextSerialId++);
		DIMENSIONS.add(reg);
		return reg;
	}

	public static List<DimensionRegistration> getAll() { return DIMENSIONS; }

	public static DimensionRegistration getByIdentifier(NamespacedIdentifier id) {
		for (DimensionRegistration r : DIMENSIONS) {
			if (r.getId().equals(id)) return r;
		}
		return null;
	}

	public static DimensionRegistration getBySerialId(int serialId) {
		for (DimensionRegistration r : DIMENSIONS) {
			if (r.getSerialId() == serialId) return r;
		}
		return null;
	}

	/** True for the three vanilla-reserved dimension ids RetroAPI must never override. */
	public static boolean isVanillaId(int id) {
		return id == OVERWORLD_ID || id == NETHER_ID || id == SKYLANDS_ID;
	}
}
