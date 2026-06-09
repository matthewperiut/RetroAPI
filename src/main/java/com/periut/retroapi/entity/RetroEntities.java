package com.periut.retroapi.entity;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.mixin.entity.EntityRegistryAccessor;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.entity.Entity;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.Map;

/**
 * Consumer-facing facade for registering modded entities. Usage inside an
 * {@link com.periut.retroapi.entity.event.EntityRegistrationCallback} listener:
 *
 * <pre>{@code
 * RetroEntities.register(id("Moa"), EntityMoa.class)
 *     .factory((world) -> new EntityMoa(world))
 *     .tracking(80, 3, EntityRegistration.SEND_VELOCITY_UNSET);
 * }</pre>
 *
 * <p>Registration inserts the entity into vanilla {@code EntityRegistry}'s string&lt;-&gt;class maps keyed by
 * {@code id.toString()} so it round-trips through save/load - identically whether StationAPI is present or
 * not (the entity-id flattening contract). The spawn/render <i>backend</i> differs (RetroAPI OSL channels
 * vs StationAPI's MessagePacket path) but is decided by the tracker/spawn mixins + factory delegation, not
 * here. The client spawn listener (added in the networking layer) resolves the factory by string id via
 * {@link RetroRegistry#getEntityById}.
 */
public final class RetroEntities {
	private RetroEntities() {}

	public static EntityRegistration register(NamespacedIdentifier id, Class<? extends Entity> clazz) {
		EntityRegistration reg = new EntityRegistration(id, clazz);
		RetroRegistry.registerEntity(reg);
		insertIntoVanillaRegistry(id.toString(), clazz);
		return reg;
	}

	/** True if this entity's class is registered with RetroAPI (drives the tracking + spawn mixins). */
	public static boolean isRetroEntity(Entity entity) {
		return entity != null && RetroRegistry.getEntityRegistration(entity.getClass()) != null;
	}

	/**
	 * Insert into vanilla {@code EntityRegistry.idToClass/classToId} (the same maps StationAPI uses).
	 * Idempotent-guarded so a duplicate id is logged, not fatal.
	 */
	private static void insertIntoVanillaRegistry(String key, Class<? extends Entity> clazz) {
		Map<String, Class<? extends Entity>> idToClass = EntityRegistryAccessor.retroapi$getIdToClass();
		Map<Class<? extends Entity>, String> classToId = EntityRegistryAccessor.retroapi$getClassToId();
		if (idToClass.containsKey(key)) {
			RetroAPI.LOGGER.warn("Duplicate entity identifier {} - skipping registration", key);
			return;
		}
		idToClass.put(key, clazz);
		classToId.put(clazz, key);
	}
}
