package com.periut.retroapi.mixin.storage;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.storage.ExtendedBlocksAccess;
import net.minecraft.block.Block;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
#if MC_HAS_UPDATE_CLIENTS
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
#endif
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
	private Set<ChunkPos> tickingChunks;

	@Shadow
	public abstract WorldChunk getChunkAt(int chunkX, int chunkZ);

#if MC_HAS_UPDATE_CLIENTS
	@ModifyConstant(method = "setBlockMetadata(IIII)V", constant = @Constant(intValue = 255))
	private int retroapi$widenUpdateClientsIndex(int original) {
		return 0xFFF;
	}
#endif

	@Inject(method = "tickChunks", at = @At("TAIL"))
	private void retroapi$tickExtendedBlocks(CallbackInfo ci) {
		// Vanilla random-ticks 80 positions per chunk from 32768 total (16*16*128).
		// For each extended block with TICKS_RANDOMLY, give it the same probability: 80/32768.
		for (ChunkPos pos : tickingChunks) {
			WorldChunk chunk = getChunkAt(pos.x, pos.z);
			if (chunk == null) continue;

			ChunkExtendedBlocks ext = ((ExtendedBlocksAccess) chunk).retroapi$getExtendedBlocks();
			if (ext.isEmpty()) continue;

			int worldX = pos.x * 16;
			int worldZ = pos.z * 16;

			for (Map.Entry<Integer, Integer> entry : ext.getBlockIds().entrySet()) {
				int blockId = entry.getValue();
				if (blockId <= 0 || blockId >= Block.TICKS_RANDOMLY.length) continue;
				if (!Block.TICKS_RANDOMLY[blockId]) continue;

				// 80/32768 ~ 0.00244 chance per tick, matching vanilla random tick rate
				if (random.nextInt(32768) >= 80) continue;

				int index = entry.getKey();
				int localX = ChunkExtendedBlocks.indexToX(index);
				int y = ChunkExtendedBlocks.indexToY(index);
				int localZ = ChunkExtendedBlocks.indexToZ(index);

				Block block = Block.BY_ID[blockId];
				if (block != null) {
					block.tick((World) (Object) this, worldX + localX, y, worldZ + localZ, random);
				}
			}
		}
	}
}
