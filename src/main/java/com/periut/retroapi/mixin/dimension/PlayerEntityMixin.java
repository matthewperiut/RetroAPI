package com.periut.retroapi.mixin.dimension;

import com.periut.retroapi.dimension.HasTeleportationManager;
import com.periut.retroapi.dimension.TeleportationManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a per-player {@link TeleportationManager} slot so a portal can attach the transition it wants the
 * player to run. Read by RetroAPI's server/client dimension-change redirects when StationAPI is absent.
 *
 * <p><b>Stays active under StationAPI</b> (unlike the rest of RetroAPI's dimension mixins): a RetroAPI
 * portal casts the player to RetroAPI's {@code HasTeleportationManager} and attaches its manager, so this
 * must be present or that cast crashes. Under StationAPI, RetroAPI's own change redirects are disabled and
 * StationAPI redirects the switch to ITS teleport manager - so on attach we also mirror the manager onto
 * the player's StationAPI slot via the RetroAPI StationAPI bridge ({@code StationBridge#attachPortal}), which runs the
 * transition through StationAPI's dimension system (where the worlds live under StationAPI). The two
 * PlayerEntity mixins coexist: each adds its own duck interface + field.
 */
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin implements HasTeleportationManager {
	@Unique private TeleportationManager retroapi$teleportationManager;

	@Override
	public TeleportationManager getTeleportationManager() {
		return retroapi$teleportationManager;
	}

	@Override
	public void setTeleportationManager(TeleportationManager manager) {
		retroapi$teleportationManager = manager;
		// Mirror onto StationAPI's teleport slot so a RetroAPI portal works under StationAPI too. Gated +
		// indirected so the StationAPI reference never loads without StationAPI. Portal-collision only,
		// not a hot path.
		if (FabricLoader.getInstance().isModLoaded("stationapi")) {
			com.periut.retroapi.compat.StationBridges.get().attachPortal((PlayerEntity) (Object) this, manager);
		}
	}
}
