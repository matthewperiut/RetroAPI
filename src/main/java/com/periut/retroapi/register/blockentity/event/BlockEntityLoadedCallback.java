package com.periut.retroapi.register.blockentity.event;

import net.minecraft.block.entity.BlockEntity;
import net.ornithemc.osl.core.api.events.Event;

/**
 * Fired once per block entity, the first time it ticks in a world - i.e. after its NBT has been read AND
 * after it has been attached to a world, which is the pass beta simply does not have.
 *
 * <p>{@code readNbt} runs while the chunk is still being deserialized: the block entity has no world yet,
 * its neighbors may not exist, and touching the world from there either NPEs or reads a half-built chunk.
 * The usual workaround was a hand-rolled "did I initialize yet?" boolean in every block entity. This is
 * that boolean, once, for everyone:
 *
 * <pre>
 * BlockEntityLoadedCallback.EVENT.register(be -&gt; {
 *     if (be instanceof MultiblockCore core) core.rebuildStructure();   // safe: world + neighbors exist
 * });
 * </pre>
 *
 * <p>Fires on both sides for block entities that tick. Removed block entities never fire.
 */
public final class BlockEntityLoadedCallback {

	@FunctionalInterface
	public interface Listener {
		void onBlockEntityLoaded(BlockEntity blockEntity);
	}

	public static final Event<Listener> EVENT = Event.of(listeners -> blockEntity -> {
		for (Listener listener : listeners) {
			listener.onBlockEntityLoaded(blockEntity);
		}
	});

	private BlockEntityLoadedCallback() {}
}
