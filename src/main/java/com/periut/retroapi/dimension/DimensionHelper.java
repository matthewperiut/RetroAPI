package com.periut.retroapi.dimension;

import com.periut.retroapi.RetroAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.dimension.PortalForcer;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * Moves a player between the overworld and a registered modded dimension - RetroAPI's own
 * generalisation of vanilla's hard-coded overworld&lt;-&gt;nether teleport, parameterised over an
 * arbitrary destination identifier, a coordinate scale, and a {@link PortalForcer}.
 *
 * <p>This class is environment-neutral: it references only generic classes so it loads on both the
 * client and the dedicated server. The actual move is delegated to a {@link DimensionTeleporter}
 * registered per environment ({@code ClientTeleporter} on the client, {@code ServerTeleporter} on the
 * server) - so neither side ever loads the other's environment-specific classes (this is what
 * prevents the "cannot load ServerPlayerEntity in environment type CLIENT" failure).
 *
 * <p>Like vanilla, a transition flips between the destination and the overworld: a player already in
 * the destination is sent back to the overworld.
 */
public final class DimensionHelper {
	private DimensionHelper() {}

	private static DimensionTeleporter teleporter;

	/** Registered once per environment from {@code RetroAPIClient} / {@code RetroAPIServer}. */
	public static void setTeleporter(DimensionTeleporter teleporter) {
		DimensionHelper.teleporter = teleporter;
	}

	public static void switchDimension(PlayerEntity player, NamespacedIdentifier destination, double scale, PortalForcer travelAgent) {
		DimensionRegistration reg = RetroDimensionRegistry.getByIdentifier(destination);
		if (reg == null) {
			RetroAPI.LOGGER.warn("switchDimension: no registered dimension {}", destination);
			return;
		}
		if (teleporter == null) {
			RetroAPI.LOGGER.warn("switchDimension: no DimensionTeleporter registered for this environment");
			return;
		}
		if (scale <= 0.0) scale = 1.0;
		int destId = reg.getSerialId();
		// Flip between the destination and the overworld (mirrors vanilla nether<->overworld).
		int target = (player.dimensionId == destId) ? RetroDimensionRegistry.OVERWORLD_ID : destId;
		boolean entering = (target == destId);
		// Vanilla scales nether coords by /8 entering, *8 leaving; scale==1 (the default) is identity.
		double coordFactor = entering ? (1.0 / scale) : scale;

		teleporter.teleport(player, target, destId, coordFactor, travelAgent);
	}
}
