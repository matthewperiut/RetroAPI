package com.periut.retroapi.register.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;

/**
 * Marks a block entity whose data the server should send to clients - the thing beta simply does not
 * have for modded blocks.
 *
 * <p>b1.7.3's protocol has no generic block-entity packet. {@code BlockEntity.createUpdatePacket()}
 * exists, but the only packet in the protocol that can carry block-entity data is the sign packet, so a
 * modded block entity has exactly one vanilla-shaped way to reach the client: pretend to be a container
 * and push its contents through the inventory/window packets. That is why "sync it through an inventory"
 * became the standard trick, and why anything that is NOT an inventory (a tank's fluid level, a machine's
 * progress bar, a barrel's displayed stack, a lookie barrel) has no answer at all.
 *
 * <p>Implement this on the block entity and RetroAPI carries its NBT over its own channel:
 *
 * <pre>
 * public class BarrelBlockEntity extends BlockEntity implements RetroSyncedBlockEntity {
 *     public ItemStack stored;
 *
 *     &#64;Override public void writeNbt(NbtCompound nbt) { ... }   // used for both disk and wire
 *     &#64;Override public void readNbt(NbtCompound nbt)  { ... }
 * }
 *
 * // whenever the contents change on the server:
 * RetroBlockEntities.sync(this);
 * </pre>
 *
 * <p>Sync happens automatically in two places once the interface is present: when a chunk is first sent
 * to a player, and whenever the block is marked dirty ({@code world.setBlockDirty(x, y, z)}), which is
 * also what {@link RetroBlockEntities#sync} does. Singleplayer needs none of it - b1.7.3 singleplayer is
 * client-only, so the client already holds the real block entity - and the sync path is skipped there.
 *
 * <p>Override {@link #writeSyncNbt}/{@link #readSyncNbt} when the wire form should differ from the disk
 * form, e.g. to leave a big inventory out of a packet that only needs to update a progress bar.
 *
 * <p>The block entity must be registered with
 * {@link RetroBlockEntities#register(net.ornithemc.osl.core.api.util.NamespacedIdentifier, Class)}: the
 * wire form is NBT, and vanilla's {@code writeNbt} refuses to write a class it has no id for. That is the
 * same call the block entity already needs to survive a save, so if it round-trips to disk it can sync.
 */
public interface RetroSyncedBlockEntity {

	/**
	 * Writes what the client needs. Defaults to the same NBT that goes to disk, which is right for most
	 * block entities; narrow it when the disk form is bigger than the client needs.
	 */
	default void writeSyncNbt(NbtCompound nbt) {
		((BlockEntity) this).writeNbt(nbt);
	}

	/** Applies NBT written by {@link #writeSyncNbt} on the client. Mirror any narrowing you did there. */
	default void readSyncNbt(NbtCompound nbt) {
		((BlockEntity) this).readNbt(nbt);
	}

	/**
	 * Called on the client right after {@link #readSyncNbt}. RetroAPI already re-renders the block, so
	 * override this only for extra work - restarting an animation, invalidating a cached model.
	 */
	default void onSynced() {
	}
}
