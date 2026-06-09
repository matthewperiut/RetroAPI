package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(Chunk.class)
public abstract class WorldChunkMixin implements ExtendedBlocksAccess {

	@Shadow public byte[] blocks;
	@Shadow public ChunkNibbleArray meta;
	@Shadow public World world;
	@Shadow public int x;
	@Shadow public int z;
	@Shadow public boolean dirty;
	@Shadow public Map<BlockPos, BlockEntity> blockEntities;
	@Shadow public byte[] heightmap;
	@Shadow public int minHeightmapValue;
	@Shadow public ChunkNibbleArray skyLight;

	@Shadow public abstract int getHeight(int x, int z);
	@Shadow private void lightGaps(int localX, int localZ) {}
	@Shadow private void lightGap(int x, int z, int height) {}

	@Unique
	private final ChunkExtendedBlocks retroapi$extendedBlocks = new ChunkExtendedBlocks();

	@Unique
	private boolean retroapi$handlingExtended = false;

	@Override
	public ChunkExtendedBlocks retroapi$getExtendedBlocks() {
		return retroapi$extendedBlocks;
	}

	@Inject(method = "getBlockId", at = @At("RETURN"), cancellable = true)
	private void retroapi$getBlockAt(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		int index = ChunkExtendedBlocks.toIndex(x, y, z);
		if (retroapi$extendedBlocks.hasEntry(index)) {
			cir.setReturnValue(retroapi$extendedBlocks.getBlockId(index));
		}
	}

	@Inject(method = "setBlock(IIII)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleSetBlock(int x, int y, int z, int rawId, CallbackInfoReturnable<Boolean> cir) {
		if (retroapi$handlingExtended) return;

		int index = ChunkExtendedBlocks.toIndex(x, y, z);

		if (rawId >= 256) {
			// Handle removal of old extended block at this position
			if (retroapi$extendedBlocks.hasEntry(index)) {
				int oldExtId = retroapi$extendedBlocks.getBlockId(index);
				retroapi$extendedBlocks.remove(index);
				Block oldBlock = (oldExtId >= 0 && oldExtId < Block.BLOCKS.length) ? Block.BLOCKS[oldExtId] : null;
				if (oldBlock != null && world != null) {
					oldBlock.onBreak(world, this.x * 16 + x, y, this.z * 16 + z);
				}
			}

			// Let vanilla handle clearing the byte array position (heightmap, lighting, vanilla block removal)
			retroapi$handlingExtended = true;
			((Chunk) (Object) this).setBlock(x, y, z, 0);
			retroapi$handlingExtended = false;

			// Store the extended block
			retroapi$extendedBlocks.set(index, rawId, 0);
			this.meta.set(x, y, z, 0);

			// Update heightmap and lighting to match vanilla's setBlockAt flow
			retroapi$updateHeightMapForColumn(x, z);

			int worldX = this.x * 16 + x;
			int worldZ = this.z * 16 + z;
			world.queueLightUpdate(LightType.SKY, worldX, y, worldZ, worldX, y, worldZ);
			world.queueLightUpdate(LightType.BLOCK, worldX, y, worldZ, worldX, y, worldZ);
			lightGaps(x, z);

			// Call onAdded for the new block
			Block newBlock = Block.BLOCKS[rawId];
			if (newBlock != null && world != null && !world.isRemote) {
				newBlock.onPlaced(world, this.x * 16 + x, y, this.z * 16 + z);
			}

			dirty = true;
			cir.setReturnValue(true);
		} else {
			// Vanilla block being placed - clear any extended entry
			if (retroapi$extendedBlocks.hasEntry(index)) {
				int oldExtId = retroapi$extendedBlocks.getBlockId(index);
				retroapi$extendedBlocks.remove(index);
				Block oldBlock = (oldExtId >= 0 && oldExtId < Block.BLOCKS.length) ? Block.BLOCKS[oldExtId] : null;
				if (oldBlock != null && world != null) {
					oldBlock.onBreak(world, this.x * 16 + x, y, this.z * 16 + z);
				}
				// Extended blocks are stored as 0 in the vanilla byte array.
				// Vanilla's setBlockAt checks old == new and skips all updates if equal.
				// Set to 1 (stone) temporarily so vanilla sees a real change (1 -> rawId).
				int byteIndex = x << 11 | z << 7 | y;
				blocks[byteIndex] = 1;
			}
			// Let vanilla handle the rest
		}
	}

	@Inject(method = "setBlock(IIIII)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleSetBlockWithMeta(int x, int y, int z, int rawId, int meta, CallbackInfoReturnable<Boolean> cir) {
		if (retroapi$handlingExtended) return;

		int index = ChunkExtendedBlocks.toIndex(x, y, z);

		if (rawId >= 256) {
			// Handle removal of old extended block at this position
			if (retroapi$extendedBlocks.hasEntry(index)) {
				int oldExtId = retroapi$extendedBlocks.getBlockId(index);
				retroapi$extendedBlocks.remove(index);
				Block oldBlock = (oldExtId >= 0 && oldExtId < Block.BLOCKS.length) ? Block.BLOCKS[oldExtId] : null;
				if (oldBlock != null && world != null && !world.isRemote) {
					oldBlock.onBreak(world, this.x * 16 + x, y, this.z * 16 + z);
				}
			}

			// Let vanilla clear the byte array position
			retroapi$handlingExtended = true;
			((Chunk) (Object) this).setBlock(x, y, z, 0, 0);
			retroapi$handlingExtended = false;

			// Store the extended block with metadata
			retroapi$extendedBlocks.set(index, rawId, meta);
			// The vanilla META NIBBLE has no 256-id limitation, so it stays the source of truth for
			// extended-block meta too: getBlockMeta and the vanilla "Data" NBT round-trip work
			// natively (the vanilla-clear above either zeroed it or skipped it; write explicitly).
			this.meta.set(x, y, z, meta);

			// Update heightmap and lighting to match vanilla's setBlockWithMetadataAt flow
			retroapi$updateHeightMapForColumn(x, z);

			int worldX = this.x * 16 + x;
			int worldZ = this.z * 16 + z;
			world.queueLightUpdate(LightType.SKY, worldX, y, worldZ, worldX, y, worldZ);
			world.queueLightUpdate(LightType.BLOCK, worldX, y, worldZ, worldX, y, worldZ);
			lightGaps(x, z);

			// Call onAdded for the new block
			Block newBlock = Block.BLOCKS[rawId];
			if (newBlock != null && world != null) {
				newBlock.onPlaced(world, this.x * 16 + x, y, this.z * 16 + z);
			}

			dirty = true;
			cir.setReturnValue(true);
		} else {
			// Vanilla block being placed - clear any extended entry
			if (retroapi$extendedBlocks.hasEntry(index)) {
				int oldExtId = retroapi$extendedBlocks.getBlockId(index);
				retroapi$extendedBlocks.remove(index);
				Block oldBlock = (oldExtId >= 0 && oldExtId < Block.BLOCKS.length) ? Block.BLOCKS[oldExtId] : null;
				if (oldBlock != null && world != null && !world.isRemote) {
					oldBlock.onBreak(world, this.x * 16 + x, y, this.z * 16 + z);
				}
				// Extended blocks are stored as 0 in the vanilla byte array.
				// Vanilla's setBlockWithMetadataAt checks old == new and skips updates if equal.
				// Set to 1 (stone) temporarily so vanilla sees a real change (1 -> rawId).
				int byteIndex = x << 11 | z << 7 | y;
				blocks[byteIndex] = 1;
			}
			// Let vanilla handle the rest
		}
	}

	/**
	 * Post-placement meta changes (crop growth, machine state...) go through setBlockMeta and write
	 * the vanilla nibble (which extended blocks now share) - mirror the new value into the extended
	 * map so the sidecar and the MP chunk_ext packet stay in sync.
	 */
	@Inject(method = "setBlockMeta", at = @At("TAIL"))
	private void retroapi$syncExtendedMeta(int x, int y, int z, int newMeta, CallbackInfo ci) {
		int index = ChunkExtendedBlocks.toIndex(x, y, z);
		if (retroapi$extendedBlocks.hasEntry(index)) {
			retroapi$extendedBlocks.set(index, retroapi$extendedBlocks.getBlockId(index), newMeta);
		}
	}

	@Inject(method = "setBlockEntity", at = @At("HEAD"), cancellable = true)
	private void retroapi$setBlockEntityAt(int x, int y, int z, BlockEntity be, CallbackInfo ci) {
		int blockId = ((Chunk) (Object) this).getBlockId(x, y, z);
		if (blockId <= 0 || blockId >= Block.BLOCKS.length) return;
		Block block = Block.BLOCKS[blockId];
		if (block == null) return;

		// For non-BlockWithBlockEntity blocks that are RetroAPI blocks with block entities,
		// bypass the vanilla instanceof check and store the BE directly
		if (!(block instanceof BlockWithEntity)
			&& (Block.BLOCKS_WITH_ENTITY[blockId] || RetroRegistry.getBlockRegistration(block) != null)) {
			be.world = world;
			be.x = this.x * 16 + x;
			be.y = y;
			be.z = this.z * 16 + z;
			be.cancelRemoval();
			blockEntities.put(new BlockPos(x, y, z), be);
			ci.cancel();
		}
	}

	@Inject(method = "getBlockEntity", at = @At("HEAD"), cancellable = true)
	private void retroapi$getBlockEntityAt(int x, int y, int z, CallbackInfoReturnable<BlockEntity> cir) {
		BlockPos pos = new BlockPos(x, y, z);
		BlockEntity existing = blockEntities.get(pos);
		if (existing != null) {
			if (existing.isRemoved()) {
				blockEntities.remove(pos);
				cir.setReturnValue(null);
			} else {
				cir.setReturnValue(existing);
			}
			return;
		}

		int blockId = ((Chunk) (Object) this).getBlockId(x, y, z);
		if (blockId <= 0 || blockId >= Block.BLOCKS.length) return;
		if (!Block.BLOCKS_WITH_ENTITY[blockId]) return;
		Block block = Block.BLOCKS[blockId];
		if (block == null) return;

		// Only intercept non-vanilla block entity blocks to avoid the ClassCastException
		if (!(block instanceof BlockWithEntity)) {
			// RetroAPI block entity - create via our onAdded which sets the BE
			block.onPlaced(world, this.x * 16 + x, y, this.z * 16 + z);
			existing = blockEntities.get(pos);
			cir.setReturnValue(existing);
		}
	}

	@Unique
	private static boolean retroapi$isRetroApiBlock(int blockId) {
		if (blockId <= 0 || blockId >= Block.BLOCKS.length) return false;
		Block block = Block.BLOCKS[blockId];
		if (block == null) return false;
		return RetroRegistry.getBlockRegistration(block) != null;
	}

	// --- Heightmap fixes for extended blocks ---

	@Inject(method = "populateHeightMapOnly", at = @At("RETURN"), require = 0)
	private void retroapi$fixHeightMapOnly(CallbackInfo ci) {
		retroapi$adjustHeightMapForExtendedBlocks();
	}

	@Inject(method = "populateHeightMap", at = @At("RETURN"))
	private void retroapi$fixHeightMap(CallbackInfo ci) {
		retroapi$adjustHeightMapForExtendedBlocks();
	}

	@Unique
	private void retroapi$adjustHeightMapForExtendedBlocks() {
		if (retroapi$extendedBlocks.isEmpty()) return;

		// On the CLIENT, only the heightmap may be fixed here - never the light arrays. This runs
		// from populateHeightMapOnly on every chunk-data packet and from the chunk_ext receive; the
		// server's authoritative light arrives in those same packets, and rewriting columns with
		// our vertical-only estimate made blocks flash bright in dark spaces before client-side
		// engine updates converged them back down.
		boolean fixLight = world == null || !world.isRemote;

		// Track which columns need sky light recalculation
		boolean[] affectedColumns = new boolean[256];

		for (Map.Entry<Integer, Integer> entry : retroapi$extendedBlocks.getBlockIds().entrySet()) {
			int blockId = entry.getValue();
			if (blockId <= 0 || blockId >= Block.BLOCKS_LIGHT_OPACITY.length) continue;

			int index = entry.getKey();
			int localX = ChunkExtendedBlocks.indexToX(index);
			int y = ChunkExtendedBlocks.indexToY(index);
			int localZ = ChunkExtendedBlocks.indexToZ(index);

			// Luminous extended blocks: vanilla populateLight read the raw byte array (air), so
			// freshly generated chunks have no block light at them - queue an engine update.
			if (fixLight && Block.BLOCKS_LIGHT_LUMINANCE[blockId] > 0 && world != null) {
				int wx = this.x * 16 + localX;
				int wz = this.z * 16 + localZ;
				world.queueLightUpdate(LightType.BLOCK, wx, y, wz, wx, y, wz);
			}

			if (Block.BLOCKS_LIGHT_OPACITY[blockId] == 0) continue;

			int hmIndex = localZ << 4 | localX;
			int currentHeight = heightmap[hmIndex] & 255;

			// The extended block is opaque and above the current heightmap value
			if (y + 1 > currentHeight) {
				heightmap[hmIndex] = (byte) (y + 1);
				if (y + 1 < minHeightmapValue) {
					minHeightmapValue = y + 1;
				}
			}

			affectedColumns[hmIndex] = true;
		}

		// Recalculate sky light and propagate to neighbors for affected columns (server/gen only -
		// see fixLight above).
		if (fixLight) {
			for (int hmIndex = 0; hmIndex < 256; hmIndex++) {
				if (affectedColumns[hmIndex]) {
					int localX = hmIndex & 15;
					int localZ = hmIndex >> 4;
					retroapi$recalculateColumnSkyLight(localX, localZ);
					lightGaps(localX, localZ);
				}
			}
		}
	}

	@Unique
	private void retroapi$recalculateColumnSkyLight(int localX, int localZ) {
		if (world != null && world.dimension.hasCeiling) return;

		int hmIndex = localZ << 4 | localX;
		int hmHeight = heightmap[hmIndex] & 255;

		// Above heightmap: full sky light
		for (int y = 127; y >= hmHeight; y--) {
			skyLight.set(localX, y, localZ, 15);
		}

		// Below heightmap: seed with vertical attenuation, then hand the span to the vanilla light
		// engine - the engine reads getBlockId (extended-aware via our inject), so it repropagates
		// HORIZONTAL light correctly. The vertical-only seed alone left covered air pockets
		// (portal interiors, overhangs, caves through modded terrain) pitch black.
		int light = 15;
		int lowest = hmHeight;
		for (int y = hmHeight - 1; y >= 0; y--) {
			int opacity = retroapi$opacityAt(localX, y, localZ);
			// Vanilla uses minimum attenuation of 1 below the heightmap
			if (opacity == 0) {
				opacity = 1;
			}

			light -= opacity;
			if (light < 0) light = 0;
			skyLight.set(localX, y, localZ, light);
			lowest = y;
			if (light == 0) break;
		}
		if (world != null && hmHeight > 0) {
			int wx = this.x * 16 + localX;
			int wz = this.z * 16 + localZ;
			world.queueLightUpdate(LightType.SKY, wx, Math.max(lowest - 1, 0), wz, wx, hmHeight, wz);
		}
	}

	/**
	 * Extended-aware port of vanilla {@code Chunk.updateHeightMap}. Vanilla's version scans the raw
	 * byte array (extended cells read as air), so it never sees extended terrain - but everything
	 * else it does is ESSENTIAL and was missing here before: filling/zeroing the changed span,
	 * queueing the span to the sky-light engine, the walk-down attenuation seed, and the 3x3
	 * column engine update over the attenuation zone. Without those queued updates the light
	 * below changed extended terrain never converges - dark pockets inside portals / under
	 * modded terrain ("entity goes black, view dims" while standing in a non-opaque block).
	 */
	@Unique
	private void retroapi$updateHeightMapForColumn(int localX, int localZ) {
		int hmIndex = localZ << 4 | localX;
		int oldHeight = heightmap[hmIndex] & 255;

		// Scan from top of world down to find the highest opaque block (vanilla + extended)
		int newHeight = 0;
		for (int y = 127; y >= 0; y--) {
			if (retroapi$opacityAt(localX, y, localZ) != 0) {
				newHeight = y + 1;
				break;
			}
		}

		if (newHeight == oldHeight) return;

		world.setBlocksDirty(localX, localZ, newHeight, oldHeight);
		heightmap[hmIndex] = (byte) newHeight;

		// Recalculate lowestHeight (vanilla pattern)
		if (newHeight < minHeightmapValue) {
			minHeightmapValue = newHeight;
		} else {
			int minH = 127;
			for (int i = 0; i < 256; i++) {
				int h = heightmap[i] & 255;
				if (h < minH) minH = h;
			}
			minHeightmapValue = minH;
		}

		int worldX = this.x * 16 + localX;
		int worldZ = this.z * 16 + localZ;
		if (newHeight < oldHeight) {
			// Height dropped: the uncovered cells see the sky again.
			for (int y = newHeight; y < oldHeight; y++) {
				skyLight.set(localX, y, localZ, 15);
			}
		} else {
			// Height rose: zero the covered span and let the engine repropagate it.
			world.queueLightUpdate(LightType.SKY, worldX, oldHeight, worldZ, worldX, newHeight, worldZ);
			for (int y = oldHeight; y < newHeight; y++) {
				skyLight.set(localX, y, localZ, 0);
			}
		}

		// Walk down from the new height seeding vertical attenuation (extended-aware), then hand
		// the whole attenuation zone (3x3 columns, like vanilla) to the light engine so horizontal
		// propagation is recomputed properly.
		int light = 15;
		int y = newHeight;
		int attenuationTop = y;
		while (y > 0 && light > 0) {
			int opacity = retroapi$opacityAt(localX, --y, localZ);
			if (opacity == 0) opacity = 1;
			light -= opacity;
			if (light < 0) light = 0;
			skyLight.set(localX, y, localZ, light);
		}
		while (y > 0 && retroapi$opacityAt(localX, y - 1, localZ) == 0) {
			y--;
		}
		if (y != attenuationTop) {
			world.queueLightUpdate(LightType.SKY, worldX - 1, y, worldZ - 1, worldX + 1, attenuationTop, worldZ + 1);
		}

		dirty = true;
	}

	/** Light opacity at a cell, reading the extended overlay first (vanilla arrays read air there). */
	@Unique
	private int retroapi$opacityAt(int localX, int y, int localZ) {
		int extIndex = localX << 11 | localZ << 7 | y;
		int blockId;
		if (retroapi$extendedBlocks.hasEntry(extIndex)) {
			blockId = retroapi$extendedBlocks.getBlockId(extIndex);
		} else {
			blockId = blocks[extIndex] & 255;
		}
		if (blockId <= 0 || blockId >= Block.BLOCKS_LIGHT_OPACITY.length) return 0;
		return Block.BLOCKS_LIGHT_OPACITY[blockId];
	}
}

