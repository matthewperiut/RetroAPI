package com.periut.retroapi.entity;

import com.periut.retroapi.entity.client.EntityFactory;
import com.periut.retroapi.entity.client.MobFactory;
import net.minecraft.entity.Entity;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * One modded entity registration: its namespaced id, class, tracking params, living-ness, and the
 * client factory. Mirrors StationAPI's per-entity provider data but collapsed into one record so the
 * tracker mixin can read everything from a single object (massive code reduction vs StationAPI's
 * HasTrackingParameters / TrackingParametersProvider / CustomTracking machinery).
 *
 * <p>The entity id is stored as a {@link NamespacedIdentifier}; the string written into vanilla's
 * {@code EntityRegistry} (and therefore the saved {@code Entities} NBT {@code id} tag) is
 * {@code id.toString()} - byte-identical to what StationAPI writes (the entity-id flattening contract).
 */
public class EntityRegistration {
	/** sendVelocity tri-state: let the tracker decide (vanilla default). */
	public static final int SEND_VELOCITY_UNSET = -1;
	public static final int SEND_VELOCITY_FALSE = 0;
	public static final int SEND_VELOCITY_TRUE = 1;

	private final NamespacedIdentifier id;
	private final Class<? extends Entity> entityClass;

	private int trackingDistance = 80;
	private int updatePeriod = 3;
	private int sendVelocity = SEND_VELOCITY_UNSET;
	private boolean living = false;
	private EntityFactory entityFactory;
	private MobFactory mobFactory;

	public EntityRegistration(NamespacedIdentifier id, Class<? extends Entity> entityClass) {
		this.id = id;
		this.entityClass = entityClass;
	}

	public NamespacedIdentifier getId() { return id; }
	public Class<? extends Entity> getEntityClass() { return entityClass; }
	public int getTrackingDistance() { return trackingDistance; }
	public int getUpdatePeriod() { return updatePeriod; }
	public int getSendVelocity() { return sendVelocity; }
	public boolean isLiving() { return living; }
	public EntityFactory getEntityFactory() { return entityFactory; }
	public MobFactory getMobFactory() { return mobFactory; }

	/** Tracking parameters; {@code sendVelocity} is one of the SEND_VELOCITY_* constants. */
	public EntityRegistration tracking(int distance, int period, int sendVelocity) {
		this.trackingDistance = distance;
		this.updatePeriod = period;
		this.sendVelocity = sendVelocity;
		return this;
	}

	/** Register a non-living entity factory (4-arg world+pos). */
	public EntityRegistration factory(EntityFactory factory) {
		this.entityFactory = factory;
		return this;
	}

	/** Register a living (mob) factory (world only); marks this registration living. */
	public EntityRegistration factory(MobFactory factory) {
		this.mobFactory = factory;
		this.living = true;
		return this;
	}

	public EntityRegistration living() { this.living = true; return this; }
	public EntityRegistration nonLiving() { this.living = false; return this; }
}
