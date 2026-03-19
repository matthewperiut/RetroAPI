package com.periut.retroapi.mixin.network;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.network.WorldChunkPacketAccess;
import net.minecraft.network.packet.WorldChunkPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

@Mixin(WorldChunkPacket.class)
public class WorldChunkPacketMixin implements WorldChunkPacketAccess {

	@Unique private int retroapi$extCount = 0;
	@Unique private int[] retroapi$extIndices;
	@Unique private int[] retroapi$extBlockIds;
	@Unique private int[] retroapi$extMeta;

	@Override
	public int retroapi$getExtCount() {
		return retroapi$extCount;
	}

	@Override
	public int[] retroapi$getExtIndices() {
		return retroapi$extIndices;
	}

	@Override
	public int[] retroapi$getExtBlockIds() {
		return retroapi$extBlockIds;
	}

	@Override
	public int[] retroapi$getExtMeta() {
		return retroapi$extMeta;
	}

	@Override
	public void retroapi$populateExtended(ChunkExtendedBlocks extended) {
		if (extended == null || extended.isEmpty()) {
			retroapi$extCount = 0;
			return;
		}

		Map<Integer, Integer> blockIds = extended.getBlockIds();
		Map<Integer, Integer> metadataMap = extended.getMetadataMap();

		retroapi$extCount = blockIds.size();
		retroapi$extIndices = new int[retroapi$extCount];
		retroapi$extBlockIds = new int[retroapi$extCount];
		retroapi$extMeta = new int[retroapi$extCount];

		int i = 0;
		for (Map.Entry<Integer, Integer> entry : blockIds.entrySet()) {
			retroapi$extIndices[i] = entry.getKey();
			retroapi$extBlockIds[i] = entry.getValue();
			retroapi$extMeta[i] = metadataMap.getOrDefault(entry.getKey(), 0);
			i++;
		}
	}

	@Inject(method = "write", at = @At("RETURN"))
	private void retroapi$writeExtended(DataOutputStream output, CallbackInfo ci) throws IOException {
		output.writeShort(retroapi$extCount);
		for (int i = 0; i < retroapi$extCount; i++) {
			output.writeInt(retroapi$extIndices[i]);
			output.writeShort(retroapi$extBlockIds[i]);
			output.write(retroapi$extMeta[i]);
		}
	}

	@Inject(method = "read", at = @At("RETURN"))
	private void retroapi$readExtended(DataInputStream input, CallbackInfo ci) throws IOException {
		retroapi$extCount = input.readShort() & 0xFFFF;
		if (retroapi$extCount > 0) {
			retroapi$extIndices = new int[retroapi$extCount];
			retroapi$extBlockIds = new int[retroapi$extCount];
			retroapi$extMeta = new int[retroapi$extCount];
			for (int i = 0; i < retroapi$extCount; i++) {
				retroapi$extIndices[i] = input.readInt();
				retroapi$extBlockIds[i] = input.readShort() & 0xFFFF;
				retroapi$extMeta[i] = input.read();
			}
		}
	}
}
