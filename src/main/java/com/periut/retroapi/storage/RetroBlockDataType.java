package com.periut.retroapi.storage;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * A registered kind of auxiliary per-position data - one 32-bit value per block position, under a
 * namespaced key, saved with the world and sent to clients. See {@link RetroBlockData}.
 *
 * <p>Two flavors, and the difference is entirely about what happens to the value when the world is
 * opened again with a different set of mods:
 *
 * <ul>
 *   <li>{@link RetroBlockData#register} - a <b>raw</b> value. The int is stored as-is and means
 *       whatever the mod says it means. Right for counters, timers, colors, packed flags.</li>
 *   <li>{@link RetroBlockData#registerBlockRef} - a <b>block reference</b>. The value is
 *       {@code blockId | meta &lt;&lt; 12} ({@link RetroBlockData#encodeBlockRef}), and RetroAPI saves
 *       the modded half of it by string id through a per-chunk palette, exactly as it stores the
 *       modded blocks themselves. A raw type would be storing a runtime block id, and runtime block
 *       ids are a property of the installed mod set, not of the world - so the day a mod is added,
 *       removed or reordered, every stored reference would quietly point at a different block.</li>
 * </ul>
 */
public final class RetroBlockDataType {

	private final NamespacedIdentifier id;
	private final String key;
	private final boolean blockRef;

	RetroBlockDataType(NamespacedIdentifier id, boolean blockRef) {
		this.id = id;
		this.key = id.toString();
		this.blockRef = blockRef;
	}

	public NamespacedIdentifier getId() {
		return id;
	}

	/** The storage key: the identifier's string form ({@code "cladblocks:camo"}). */
	public String getKey() {
		return key;
	}

	/** True when values are {@code blockId | meta << 12} references saved by string id. */
	public boolean isBlockRef() {
		return blockRef;
	}

	@Override
	public String toString() {
		return key + (blockRef ? " (block ref)" : "");
	}
}
