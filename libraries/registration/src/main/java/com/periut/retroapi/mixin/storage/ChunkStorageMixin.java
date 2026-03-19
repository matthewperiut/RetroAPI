package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import com.periut.retroapi.storage.InventorySidecar;
import com.periut.retroapi.storage.RegionSidecar;
import com.periut.retroapi.storage.SidecarManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AlphaChunkStorage.class)
public class ChunkStorageMixin {

	@Inject(method = "loadChunkFromNbt", at = @At("RETURN"))
	private static void retroapi$onLoadChunk(World world, NbtCompound nbt, CallbackInfoReturnable<WorldChunk> cir) {
		WorldChunk chunk = cir.getReturnValue();
		if (chunk == null) return;

		// Restore extended blocks from sidecar
		RegionSidecar sidecar = SidecarManager.getRegion(chunk.chunkX, chunk.chunkZ);
		if (sidecar != null) {
			ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
			sidecar.loadChunkData(chunk.chunkX, chunk.chunkZ, extended);
		}

		// Restore modded block entities, inventory items, and item entities from sidecar
		InventorySidecar invSidecar = SidecarManager.getInventoryRegion(chunk.chunkX, chunk.chunkZ);
		if (invSidecar != null) {
			invSidecar.restoreChunkContent(chunk, world);
		}
	}

	@Inject(method = "saveChunkToNbt", at = @At("HEAD"))
	private static void retroapi$sanitizeBeforeSave(WorldChunk chunk, World world, NbtCompound nbt, CallbackInfo ci) {
		// Set extended block positions to air in the vanilla byte array
		ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
		for (int index : extended.getBlockIds().keySet()) {
			int blockId = extended.getBlockId(index);
			if (blockId >= 256 && index >= 0 && index < chunk.blocks.length) {
				chunk.blocks[index] = 0;
			}
		}
	}

	@Inject(method = "saveChunkToNbt", at = @At("RETURN"))
	private static void retroapi$onSaveChunk(WorldChunk chunk, World world, NbtCompound nbt, CallbackInfo ci) {
		ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();

		// Save extended blocks to block sidecar
		RegionSidecar sidecar = SidecarManager.getRegion(chunk.chunkX, chunk.chunkZ);
		if (sidecar != null) {
			sidecar.saveChunkData(chunk.chunkX, chunk.chunkZ, extended);
		}

		// Filter the NBT to remove all modded content and save to inventory sidecar
		InventorySidecar invSidecar = SidecarManager.getInventoryRegion(chunk.chunkX, chunk.chunkZ);
		if (invSidecar != null) {
			invSidecar.filterAndSave(chunk.chunkX, chunk.chunkZ, nbt, extended);
		}
	}
}
