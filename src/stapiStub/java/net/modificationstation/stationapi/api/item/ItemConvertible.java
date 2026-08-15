package net.modificationstation.stationapi.api.item;

import net.minecraft.item.Item;

/**
 * Stub - see src/stapiStub/java/README.md.
 *
 * <p>Vanilla {@code Block} and {@code Item} both satisfy this at runtime, through StationAPI's own
 * {@code StationFlatteningBlock}/{@code StationFlatteningItem} - which is why the listener casts
 * through {@code Object} to hand a plain b1.7.3 block over.
 */
public interface ItemConvertible {
	Item asItem();
}
