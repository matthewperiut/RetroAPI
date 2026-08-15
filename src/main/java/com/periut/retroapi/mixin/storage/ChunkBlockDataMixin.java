package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.storage.RetroBlockData;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drops a position's {@link com.periut.retroapi.storage.RetroBlockData} when the block there changes.
 *
 * <p>Without this, auxiliary data outlives the block it described and is inherited by whatever is placed
 * at that position next - break a clad stair, put a plain one back, and it comes up wearing the old
 * block's cladding. The data is keyed by position, so the position changing hands is exactly the moment
 * it stops meaning anything.
 *
 * <p>Only a change of block <em>id</em> counts. Metadata and state changes (a door opening, a crop
 * growing) go through {@code setBlockMeta}, never through here, and must keep their data.
 *
 * <p>Separate from {@code WorldChunkMixin} deliberately: that class owns the extended-id bookkeeping and
 * re-enters {@code setBlock} while doing it. This one only ever reads the id that is there now, so it is
 * correct on both passes of that re-entry.
 */
@Mixin(Chunk.class)
public abstract class ChunkBlockDataMixin {

	@Inject(method = "setBlock(IIII)Z", at = @At("HEAD"))
	private void retroapi$clearBlockData(int x, int y, int z, int rawId, CallbackInfoReturnable<Boolean> cir) {
		retroapi$clearIfBlockChanged(x, y, z, rawId);
	}

	@Inject(method = "setBlock(IIIII)Z", at = @At("HEAD"))
	private void retroapi$clearBlockDataWithMeta(int x, int y, int z, int rawId, int meta,
			CallbackInfoReturnable<Boolean> cir) {
		retroapi$clearIfBlockChanged(x, y, z, rawId);
	}

	private void retroapi$clearIfBlockChanged(int x, int y, int z, int rawId) {
		RetroBlockData.onBlockChanged((Chunk) (Object) this, x, y, z, rawId);
	}
}
