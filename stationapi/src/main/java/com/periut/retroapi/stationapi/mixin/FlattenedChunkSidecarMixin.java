package com.periut.retroapi.stationapi.mixin;

import com.periut.retroapi.storage.ExtendedBlocksAccess;
import com.periut.retroapi.storage.RegionSidecar;
import com.periut.retroapi.storage.SidecarManager;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.modificationstation.stationapi.impl.world.chunk.FlattenedWorldChunkLoader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Carries a chunk's RetroAPI sidecar data through a StationAPI world's save and load.
 *
 * <p>RetroAPI's own hooks are on {@code AlphaChunkStorage}, and StationAPI does not use it: its
 * flattening module swaps the world's chunk storage for this loader, which reads and writes its own
 * palettised sections. So neither hook ever ran, and everything the sidecar holds - {@code RetroBlockData},
 * the state bits that do not fit the vanilla nibble, cubic biome cells - was dropped on the way out of the
 * world and read back as absent on the way in.
 *
 * <p>Data only, never block ids: those are StationAPI's here, saved in its own sections, and a flattened
 * chunk has no vanilla id array for the restore to write into.
 */
@Mixin(FlattenedWorldChunkLoader.class)
public class FlattenedChunkSidecarMixin {

	@Inject(method = "loadChunk", at = @At("RETURN"))
	private void retroapi$loadSidecar(World world, int chunkX, int chunkZ, CallbackInfoReturnable<Chunk> cir) {
		Chunk chunk = cir.getReturnValue();
		if (chunk == null || world == null) {
			return;
		}
		RegionSidecar sidecar = SidecarManager.getRegion(world.dimension.id, chunk.x, chunk.z);
		if (sidecar != null) {
			sidecar.loadChunkAuxData(chunk, ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks());
		}
	}

	@Inject(method = "saveChunk", at = @At("RETURN"))
	private void retroapi$saveSidecar(World world, Chunk chunk, CallbackInfo ci) {
		if (chunk == null || world == null) {
			return;
		}
		RegionSidecar sidecar = SidecarManager.getRegion(world.dimension.id, chunk.x, chunk.z);
		if (sidecar != null) {
			sidecar.saveChunkData(chunk.x, chunk.z, ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks());
		}
	}
}
