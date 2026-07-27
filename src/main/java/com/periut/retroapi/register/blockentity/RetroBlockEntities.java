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

	/**
	 * Pushes a {@link RetroSyncedBlockEntity}'s state to the clients that can see it. Call this whenever
	 * server-side state changes in a way the client should show:
	 *
	 * <pre>{@code
	 * public void setStored(ItemStack stack) {
	 *     this.stored = stack;
	 *     RetroBlockEntities.sync(this);
	 * }
	 * }</pre>
	 *
	 * <p>Marks the block dirty, which is what routes it through vanilla's own per-chunk player tracking -
	 * so only players actually watching that chunk get the packet, and RetroAPI never has to keep a
	 * second list of who is where. A block entity that does not implement {@link RetroSyncedBlockEntity},
	 * or that has no world yet, is ignored; so is the client side, where b1.7.3 singleplayer already holds
	 * the real block entity.
	 */
	public static void sync(BlockEntity blockEntity) {
		if (!(blockEntity instanceof RetroSyncedBlockEntity)
			|| blockEntity.world == null || blockEntity.world.isRemote) {
			return;
		}
		blockEntity.markDirty();
		blockEntity.world.setBlockDirty(blockEntity.x, blockEntity.y, blockEntity.z);
	}
}
