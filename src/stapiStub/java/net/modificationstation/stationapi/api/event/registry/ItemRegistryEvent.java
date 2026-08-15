package net.modificationstation.stationapi.api.event.registry;

import net.mine_diver.unsafeevents.Event;

/**
 * Stub - see src/stapiStub/java/README.md. Fired while StationAPI's item registry is open.
 *
 * <p>The real class carries the registry and a family of {@code register} overloads through a generic
 * superclass; none of them is called here - registration goes through
 * {@code ItemTemplate.onConstructor}, exactly as StationAPI's own {@code TemplateItem} does - so the
 * stub needs nothing but the type, which is all the listener's descriptor names.
 */
public class ItemRegistryEvent extends Event {
}
