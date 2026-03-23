package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.register.block.RetroBlockAccess;
import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleStorage;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Map;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements ExtendedBlocksAccess {

	@Shadow public byte[] blocks;
	@Shadow public World world;
	@Shadow public int chunkX;
	@Shadow public int chunkZ;
	@Shadow public boolean dirty;
	@Shadow public Map<BlockPos, BlockEntity> blockEntities;
	@Shadow public byte[] heightMap;
	@Shadow public int lowestHeight;
	@Shadow public ChunkNibbleStorage skyLight;

	@Shadow public abstract int getHeight(int x, int z);
#if MC_VER >= 180
	@Shadow private void lightGaps() {}
#else
	@Shadow private void lightGaps(int localX, int localZ) {}
#endif
	@Shadow private void lightGap(int x, int z, int height) {}

	@Unique
	private final ChunkExtendedBlocks retroapi$extendedBlocks = new ChunkExtendedBlocks();

	@Unique
	private boolean retroapi$handlingExtended = false;

	@Override
	public ChunkExtendedBlocks retroapi$getExtendedBlocks() {
		return retroapi$extendedBlocks;
	}

	@Inject(method = "getBlockAt", at = @At("RETURN"), cancellable = true)
	private void retroapi$getBlockAt(int x, int y, int z, CallbackInfoReturnable<Integer> cir) {
		int index = ChunkExtendedBlocks.toIndex(x, y, z);
		if (retroapi$extendedBlocks.hasEntry(index)) {
			cir.setReturnValue(retroapi$extendedBlocks.getBlockId(index));
		}
	}

	@Inject(method = "setBlockAt(IIII)Z", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleSetBlock(int x, int y, int z, int rawId, CallbackInfoReturnable<Boolean> cir) {
		if (retroapi$handlingExtended) return;

		int index = ChunkExtendedBlocks.toIndex(x, y, z);

		if (rawId >= 256) {
			// Handle removal of old extended block at this position
			if (retroapi$extendedBlocks.hasEntry(index)) {
				int oldExtId = retroapi$extendedBlocks.getBlockId(index);
				retroapi$extendedBlocks.remove(index);
				Block oldBlock = (oldExtId >= 0 && oldExtId < Block.BY_ID.length) ? Block.BY_ID[oldExtId] : null;
				if (oldBlock != null && world != null) {
					oldBlock.onRemoved(world, chunkX * 16 + x, y, chunkZ * 16 + z);
				}
			}

			// Let vanilla handle clearing the byte array position (heightmap, lighting, vanilla block removal)
			retroapi$handlingExtended = true;
			((WorldChunk) (Object) this).setBlockAt(x, y, z, 0);
			retroapi$handlingExtended = false;

			// Store the extended block
			retroapi$extendedBlocks.set(index, rawId, 0);

			// Update heightmap and lighting to match vanilla's setBlockAt flow
			retroapi$updateHeightMapForColumn(x, z);

			int worldX = chunkX * 16 + x;
			int worldZ = chunkZ * 16 + z;
			world.updateLight(LightType.SKY, worldX, y, worldZ, worldX, y, worldZ);
			world.updateLight(LightType.BLOCK, worldX, y, worldZ, worldX, y, worldZ);
			#if MC_VER >= 180
			lightGaps();
#else
			lightGaps(x, z);
#endif

			// Call onAdded for the new block
			Block newBlock = Block.BY_ID[rawId];
			if (newBlock != null && world != null && !world.isMultiplayer) {
				newBlock.onAdded(world, chunkX * 16 + x, y, chunkZ * 16 + z);
			}

			dirty = true;
			cir.setReturnValue(true);
		} else {
			// Vanilla block being placed - clear any extended entry
			if (retroapi$extendedBlocks.hasEntry(index)) {
				int oldExtId = retroapi$extendedBlocks.getBlockId(index);
				retroapi$extendedBlocks.remove(index);
				Block oldBlock = (oldExtId >= 0 && oldExtId < Block.BY_ID.length) ? Block.BY_ID[oldExtId] : null;
				if (oldBlock != null && world != null) {
					oldBlock.onRemoved(world, chunkX * 16 + x, y, chunkZ * 16 + z);
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

	@Inject(method = "setBlockWithMetadataAt", at = @At("HEAD"), cancellable = true)
	private void retroapi$handleSetBlockWithMeta(int x, int y, int z, int rawId, int meta, CallbackInfoReturnable<Boolean> cir) {
		if (retroapi$handlingExtended) return;

		int index = ChunkExtendedBlocks.toIndex(x, y, z);

		if (rawId >= 256) {
			// Handle removal of old extended block at this position
			if (retroapi$extendedBlocks.hasEntry(index)) {
				int oldExtId = retroapi$extendedBlocks.getBlockId(index);
				retroapi$extendedBlocks.remove(index);
				Block oldBlock = (oldExtId >= 0 && oldExtId < Block.BY_ID.length) ? Block.BY_ID[oldExtId] : null;
				if (oldBlock != null && world != null && !world.isMultiplayer) {
					oldBlock.onRemoved(world, chunkX * 16 + x, y, chunkZ * 16 + z);
				}
			}

			// Let vanilla clear the byte array position
			retroapi$handlingExtended = true;
			((WorldChunk) (Object) this).setBlockWithMetadataAt(x, y, z, 0, 0);
			retroapi$handlingExtended = false;

			// Store the extended block with metadata
			retroapi$extendedBlocks.set(index, rawId, meta);

			// Update heightmap and lighting to match vanilla's setBlockWithMetadataAt flow
			retroapi$updateHeightMapForColumn(x, z);

			int worldX = chunkX * 16 + x;
			int worldZ = chunkZ * 16 + z;
			world.updateLight(LightType.SKY, worldX, y, worldZ, worldX, y, worldZ);
			world.updateLight(LightType.BLOCK, worldX, y, worldZ, worldX, y, worldZ);
			#if MC_VER >= 180
			lightGaps();
#else
			lightGaps(x, z);
#endif

			// Call onAdded for the new block
			Block newBlock = Block.BY_ID[rawId];
			if (newBlock != null && world != null) {
				newBlock.onAdded(world, chunkX * 16 + x, y, chunkZ * 16 + z);
			}

			dirty = true;
			cir.setReturnValue(true);
		} else {
			// Vanilla block being placed - clear any extended entry
			if (retroapi$extendedBlocks.hasEntry(index)) {
				int oldExtId = retroapi$extendedBlocks.getBlockId(index);
				retroapi$extendedBlocks.remove(index);
				Block oldBlock = (oldExtId >= 0 && oldExtId < Block.BY_ID.length) ? Block.BY_ID[oldExtId] : null;
				if (oldBlock != null && world != null && !world.isMultiplayer) {
					oldBlock.onRemoved(world, chunkX * 16 + x, y, chunkZ * 16 + z);
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

	@Inject(method = "setBlockEntityAt", at = @At("HEAD"), cancellable = true)
	private void retroapi$setBlockEntityAt(int x, int y, int z, BlockEntity be, CallbackInfo ci) {
		int blockId = ((WorldChunk) (Object) this).getBlockAt(x, y, z);
		if (blockId <= 0 || blockId >= Block.BY_ID.length) return;
		Block block = Block.BY_ID[blockId];
		if (block == null) return;

		// For non-BlockWithBlockEntity blocks that are RetroAPI blocks with block entities,
		// bypass the vanilla instanceof check and store the BE directly
		if (!(block instanceof BlockWithBlockEntity)
			&& (Block.HAS_BLOCK_ENTITY[blockId] || RetroRegistry.getBlockRegistration(block) != null)) {
			be.world = world;
			be.x = chunkX * 16 + x;
			be.y = y;
			be.z = chunkZ * 16 + z;
#if MC_VER >= 170
			be.cancelRemoval();
#endif
			blockEntities.put(new BlockPos(x, y, z), be);
			ci.cancel();
		}
	}

	@Inject(method = "getBlockEntityAt", at = @At("HEAD"), cancellable = true)
	private void retroapi$getBlockEntityAt(int x, int y, int z, CallbackInfoReturnable<BlockEntity> cir) {
		BlockPos pos = new BlockPos(x, y, z);
		BlockEntity existing = blockEntities.get(pos);
		if (existing != null) {
#if MC_VER >= 170
			if (existing.isRemoved()) {
				blockEntities.remove(pos);
				cir.setReturnValue(null);
			} else {
				cir.setReturnValue(existing);
			}
#else
			cir.setReturnValue(existing);
#endif
			return;
		}

		int blockId = ((WorldChunk) (Object) this).getBlockAt(x, y, z);
		if (blockId <= 0 || blockId >= Block.BY_ID.length) return;
		if (!Block.HAS_BLOCK_ENTITY[blockId]) return;
		Block block = Block.BY_ID[blockId];
		if (block == null) return;

		// Only intercept non-vanilla block entity blocks to avoid the ClassCastException
		if (!(block instanceof BlockWithBlockEntity)) {
			// RetroAPI block entity — create via our onAdded which sets the BE
			block.onAdded(world, chunkX * 16 + x, y, chunkZ * 16 + z);
			existing = blockEntities.get(pos);
			cir.setReturnValue(existing);
		}
	}

	@Unique
	private static boolean retroapi$isRetroApiBlock(int blockId) {
		if (blockId <= 0 || blockId >= Block.BY_ID.length) return false;
		Block block = Block.BY_ID[blockId];
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

		// Track which columns need sky light recalculation
		boolean[] affectedColumns = new boolean[256];

		for (Map.Entry<Integer, Integer> entry : retroapi$extendedBlocks.getBlockIds().entrySet()) {
			int blockId = entry.getValue();
			if (blockId <= 0 || blockId >= Block.OPACITIES.length) continue;
			if (Block.OPACITIES[blockId] == 0) continue;

			int index = entry.getKey();
			int localX = ChunkExtendedBlocks.indexToX(index);
			int y = ChunkExtendedBlocks.indexToY(index);
			int localZ = ChunkExtendedBlocks.indexToZ(index);

			int hmIndex = localZ << 4 | localX;
			int currentHeight = heightMap[hmIndex] & 255;

			// The extended block is opaque and above the current heightmap value
			if (y + 1 > currentHeight) {
				heightMap[hmIndex] = (byte) (y + 1);
				if (y + 1 < lowestHeight) {
					lowestHeight = y + 1;
				}
			}

			affectedColumns[hmIndex] = true;
		}

		// Recalculate sky light and propagate to neighbors for affected columns
		for (int hmIndex = 0; hmIndex < 256; hmIndex++) {
			if (affectedColumns[hmIndex]) {
				int localX = hmIndex & 15;
				int localZ = hmIndex >> 4;
				retroapi$recalculateColumnSkyLight(localX, localZ);
#if MC_VER >= 180
				lightGaps();
#else
				lightGaps(localX, localZ);
#endif
			}
		}
	}

	@Unique
	private void retroapi$recalculateColumnSkyLight(int localX, int localZ) {
		if (world != null && world.dimension.noSky) return;

		int hmIndex = localZ << 4 | localX;
		int hmHeight = heightMap[hmIndex] & 255;
		int baseOffset = localX << 11 | localZ << 7;

		// Above heightmap: full sky light
		for (int y = 127; y >= hmHeight; y--) {
			skyLight.set(localX, y, localZ, 15);
		}

		// Below heightmap: attenuate downward
		int light = 15;
		for (int y = hmHeight - 1; y >= 0; y--) {
			int extIndex = baseOffset | y;
			int blockId;
			if (retroapi$extendedBlocks.hasEntry(extIndex)) {
				blockId = retroapi$extendedBlocks.getBlockId(extIndex);
			} else {
				blockId = blocks[extIndex] & 255;
			}

			int opacity = 0;
			if (blockId > 0 && blockId < Block.OPACITIES.length) {
				opacity = Block.OPACITIES[blockId];
			}
			// Vanilla uses minimum attenuation of 1 below the heightmap
			if (opacity == 0) {
				opacity = 1;
			}

			light -= opacity;
			if (light < 0) light = 0;
			skyLight.set(localX, y, localZ, light);
		}
	}

	@Unique
	private void retroapi$updateHeightMapForColumn(int localX, int localZ) {
		int hmIndex = localZ << 4 | localX;
		int currentHeight = heightMap[hmIndex] & 255;

		// Scan from top of world down to find the highest opaque block (vanilla + extended)
		int baseOffset = localX << 11 | localZ << 7;
		int newHeight = 0;
		for (int y = 127; y >= 0; y--) {
			int extIndex = baseOffset | y;
			int blockId;
			if (retroapi$extendedBlocks.hasEntry(extIndex)) {
				blockId = retroapi$extendedBlocks.getBlockId(extIndex);
			} else {
				blockId = blocks[extIndex] & 255;
			}
			if (blockId > 0 && blockId < Block.OPACITIES.length && Block.OPACITIES[blockId] != 0) {
				newHeight = y + 1;
				break;
			}
		}

		if (newHeight != currentHeight) {
			// Notify world of heightmap change (matches vanilla updateHeightMap)
			world.onHeightMapChanged(localX, localZ, newHeight, currentHeight);

			heightMap[hmIndex] = (byte) newHeight;

			// Recalculate lowestHeight
			if (newHeight < lowestHeight) {
				lowestHeight = newHeight;
			} else {
				int minH = 127;
				for (int i = 0; i < 256; i++) {
					int h = heightMap[i] & 255;
					if (h < minH) minH = h;
				}
				lowestHeight = minH;
			}
		}
	}
}
