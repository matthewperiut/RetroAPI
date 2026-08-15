package com.periut.retroapi.register.blockentity;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.world.World;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Block-entity state that arrived before the block entity did.
 *
 * <p>A server sends a chunk and then, in the same breath, the state of the block entities inside it.
 * The client can still be a step behind at that moment - the chunk is not in the world yet, or the
 * block entity has not been made from it - and a sync that lands then used to be dropped on the floor.
 * Nothing ever sent it again, so the block kept whatever it was constructed with: a mob spawner joined
 * showing a pig no matter what it really spawned, right up until someone changed it and the change
 * packet arrived with the chunk long since loaded.
 *
 * <p>So the state waits here instead, and is tried again each tick for a few seconds before being given
 * up on. Applying it is the same call the listener would have made, and applying it twice is harmless -
 * it is a description of the block entity, not an instruction to change it.
 */
public final class PendingBlockEntitySync {

	/** Two and a half seconds. Long enough for a chunk to arrive, short enough not to hoard. */
	private static final int MAX_AGE_TICKS = 50;

	private record Pending(int x, int y, int z, byte[] data, int tick) {
	}

	private static final List<Pending> WAITING = new ArrayList<>();
	private static int ticks;
	private static boolean registered;

	private PendingBlockEntitySync() {}

	/** Keeps state whose block entity is not there yet, to be applied when it turns up. */
	public static void stash(int x, int y, int z, byte[] data) {
		ensureRegistered();
		WAITING.removeIf(pending -> pending.x() == x && pending.y() == y && pending.z() == z);
		WAITING.add(new Pending(x, y, z, data, ticks));
	}

	/** Dropped on disconnect: a position means nothing in the next world. */
	public static void clear() {
		WAITING.clear();
	}

	private static void ensureRegistered() {
		if (registered) {
			return;
		}
		registered = true;
		MinecraftClientEvents.TICK_END.register(minecraft -> retry(minecraft.world));
	}

	private static void retry(World world) {
		ticks++;
		if (WAITING.isEmpty()) {
			return;
		}
		if (world == null) {
			WAITING.clear();
			return;
		}

		WAITING.removeIf(pending -> {
			final BlockEntity blockEntity = world.getBlockEntity(pending.x(), pending.y(), pending.z());
			if (blockEntity != null) {
				BlockEntitySyncCodec.decode(blockEntity, pending.data());
				world.setBlocksDirty(pending.x(), pending.y(), pending.z(), pending.x(), pending.y(), pending.z());
				return true;
			}
			return ticks - pending.tick() > MAX_AGE_TICKS;
		});
	}
}
