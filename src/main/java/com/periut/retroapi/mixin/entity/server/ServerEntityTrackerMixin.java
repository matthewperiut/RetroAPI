package com.periut.retroapi.mixin.entity.server;

import com.periut.retroapi.entity.EntityRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.server.entity.EntityTracker;
import net.minecraft.util.IntHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Starts tracking modded entities using their {@link EntityRegistration} params, after vanilla has had its
 * chance in {@code onEntityAdded}. Collapses StationAPI's TrackEntityEvent + HasTrackingParameters +
 * TrackingParametersProvider + CustomTracking machinery (5 files) into one mixin reading the registration.
 */
@Mixin(EntityTracker.class)
public abstract class ServerEntityTrackerMixin {

	@Shadow private IntHashMap entriesById;

	@Shadow public abstract void startTracking(Entity entity, int trackingDistance, int updatePeriod);

	@Shadow public abstract void startTracking(Entity entity, int trackingDistance, int updatePeriod, boolean sendVelocity);

	@Shadow public abstract void onEntityRemoved(Entity entity);

	@Inject(method = "onEntityAdded", at = @At("RETURN"))
	private void retroapi$trackModded(Entity entity, CallbackInfo ci) {
		EntityRegistration reg = RetroRegistry.getEntityRegistration(entity.getClass());
		if (reg == null) {
			return; // not a RetroAPI entity; vanilla already handled (or ignored) it
		}
		if (this.entriesById.containsKey(entity.id)) {
			this.onEntityRemoved(entity); // re-track guard, mirrors StationAPI TrackingParametersImpl
		}
		if (reg.getSendVelocity() == EntityRegistration.SEND_VELOCITY_UNSET) {
			this.startTracking(entity, reg.getTrackingDistance(), reg.getUpdatePeriod());
		} else {
			this.startTracking(entity, reg.getTrackingDistance(), reg.getUpdatePeriod(),
				reg.getSendVelocity() == EntityRegistration.SEND_VELOCITY_TRUE);
		}
	}
}
