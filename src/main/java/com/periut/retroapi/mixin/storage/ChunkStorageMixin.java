package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import com.periut.retroapi.storage.InventorySidecar;
import com.periut.retroapi.storage.RegionSidecar;
import com.periut.retroapi.storage.SidecarManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AlphaChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AlphaChunkStorage.class)
public class ChunkStorageMixin {

	@Inject(method = "loadChunkFromNbt", at = @At("RETURN"))
	private static void retroapi$onLoadChunk(World world, NbtCompound nbt, CallbackInfoReturnable<Chunk> cir) {
		Chunk chunk = cir.getReturnValue();
		if (chunk == null) return;

		int dimensionId = world.dimension.id;

		// Restore extended blocks from sidecar. loadChunkData owns the full restore contract:
		// block-match + vanilla-displacement checks (vanilla-placed blocks win; displaced modded
		// entries stay deferred in the sidecar), and writes the sidecar's authoritative meta into
		// the vanilla nibble only for the positions it actually restored.
		RegionSidecar sidecar = SidecarManager.getRegion(dimensionId, chunk.x, chunk.z);
		if (sidecar != null) {
			ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
			sidecar.loadChunkData(chunk, extended);
		}

		// Restore modded block entities, inventory items, and item entities from sidecar
		InventorySidecar invSidecar = SidecarManager.getInventoryRegion(dimensionId, chunk.x, chunk.z);
		if (invSidecar != null) {
			invSidecar.restoreChunkContent(chunk, world);
		}
	}

	@Inject(method = "saveChunkToNbt", at = @At("HEAD"))
	private static void retroapi$sanitizeBeforeSave(Chunk chunk, World world, NbtCompound nbt, CallbackInfo ci) {
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
	private static void retroapi$onSaveChunk(Chunk chunk, World world, NbtCompound nbt, CallbackInfo ci) {
		ChunkExtendedBlocks extended = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
		int dimensionId = world.dimension.id;

		// Save extended blocks to block sidecar
		RegionSidecar sidecar = SidecarManager.getRegion(dimensionId, chunk.x, chunk.z);
		if (sidecar != null) {
			sidecar.saveChunkData(chunk.x, chunk.z, extended);
		}

		// Filter the NBT to remove all modded content and save to inventory sidecar
		InventorySidecar invSidecar = SidecarManager.getInventoryRegion(dimensionId, chunk.x, chunk.z);
		if (invSidecar != null) {
			invSidecar.filterAndSave(chunk.x, chunk.z, nbt, extended);
		}
	}
}
