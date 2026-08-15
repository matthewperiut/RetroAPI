package net.modificationstation.stationapi.api.client.event.color.item;

import net.mine_diver.unsafeevents.Event;
import net.modificationstation.stationapi.api.client.color.block.BlockColors;
import net.modificationstation.stationapi.api.client.color.item.ItemColors;

/**
 * Stub - see src/stapiStub/java/README.md.
 *
 * <p>The real fields are {@code final}; these are not, because final vs non-final changes nothing at
 * the access site's bytecode and a non-final field needs no initialiser here.
 */
public class ItemColorsRegisterEvent extends Event {
	public BlockColors blockColors;
	public ItemColors itemColors;
}
