package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.minecraft.block.Block;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Random;
import java.util.Set;

@Mixin(World.class)
public abstract class WorldMixin {

	@Shadow
	public Random random;

	@Shadow
	private Set<ChunkPos> activeChunks;

	@Shadow
	public abstract Chunk getChunk(int chunkX, int chunkZ);


	// Vanilla masks the block id with 255 before indexing Block.UPDATE_CLIENTS in setBlockMeta;
	// widen so extended ids (up to RetroIds.BLOCK_ID_CAPACITY - 1 = 31999) aren't truncated. The
	// indexed array is grown to BLOCK_ID_CAPACITY by BlockArrayExpandMixin, and valid ids stay
	// below 0xFFFF so this mask never truncates a real id nor over-indexes the array.
	@ModifyConstant(method = "setBlockMeta(IIII)V", constant = @Constant(intValue = 255))
	private int retroapi$widenUpdateClientsIndex(int original) {
		return 0xFFFF;
	}

	@Inject(method = "manageChunkUpdatesAndEvents", at = @At("TAIL"))
	private void retroapi$tickExtendedBlocks(CallbackInfo ci) {
		// Vanilla random-ticks 80 positions per chunk from 32768 total (16*16*128).
		// For each extended block with TICKS_RANDOMLY, give it the same probability: 80/32768.
		//
		// Two-phase: collect the positions that win the roll first, THEN tick them. onTick
		// handlers mutate the extended-block maps (grass spread, sapling growth, torch state),
		// including across chunk borders, so iterating the live maps while ticking throws
		// ConcurrentModificationException.
		java.util.ArrayList<int[]> due = null;
		for (ChunkPos pos : activeChunks) {
			Chunk chunk = getChunk(pos.x, pos.z);
			if (chunk == null) continue;

			ChunkExtendedBlocks ext = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
			if (ext.isEmpty()) continue;

			int worldX = pos.x * 16;
			int worldZ = pos.z * 16;

			for (Map.Entry<Integer, Integer> entry : ext.getBlockIds().entrySet()) {
				int blockId = entry.getValue();
				if (blockId <= 0 || blockId >= Block.BLOCKS_RANDOM_TICK.length) continue;
				if (!Block.BLOCKS_RANDOM_TICK[blockId]) continue;

				// 80/32768 ~ 0.00244 chance per tick, matching vanilla random tick rate
				if (random.nextInt(32768) >= 80) continue;

				int index = entry.getKey();
				if (due == null) due = new java.util.ArrayList<>();
				due.add(new int[]{
					worldX + ChunkExtendedBlocks.indexToX(index),
					ChunkExtendedBlocks.indexToY(index),
					worldZ + ChunkExtendedBlocks.indexToZ(index)
				});
			}
		}

		if (due != null) {
			World self = (World) (Object) this;
			for (int[] p : due) {
				// Re-resolve: an earlier tick in this batch may have replaced the block.
				int blockId = self.getBlockId(p[0], p[1], p[2]);
				if (blockId <= 0 || blockId >= Block.BLOCKS_RANDOM_TICK.length) continue;
				if (!Block.BLOCKS_RANDOM_TICK[blockId]) continue;
				Block block = Block.BLOCKS[blockId];
				if (block != null) {
					block.onTick(self, p[0], p[1], p[2], random);
				}
			}
		}
	}
}

