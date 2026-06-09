package com.periut.retroapi.register.blockentity;

import com.periut.retroapi.mixin.register.BlockEntityAccessor;
import net.minecraft.block.entity.BlockEntity;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * Registers modded block-entity classes into vanilla's string-id registry so they
 * round-trip through chunk NBT. The id is the identifier's {@code toString()}
 * (e.g. {@code "aether:freezer"}), mirroring StationAPI's {@code BlockEntityRegisterEvent}.
 *
 * <pre>{@code
 * RetroBlockEntities.register(id("freezer"), BlockEntityFreezer.class);
 * }</pre>
 */
public final class RetroBlockEntities {
	private RetroBlockEntities() {}

	public static void register(NamespacedIdentifier id, Class<? extends BlockEntity> blockEntityClass) {
		register(id.toString(), blockEntityClass);
	}

	public static void register(String id, Class<? extends BlockEntity> blockEntityClass) {
		BlockEntityAccessor.retroapi$create(blockEntityClass, id);
	}
}
