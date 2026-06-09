package com.periut.retroapi.mixin.network;

import com.periut.retroapi.storage.ChunkExtendedBlocks;
import com.periut.retroapi.network.WorldChunkPacketAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;

@Mixin(ChunkDataS2CPacket.class)
public class WorldChunkPacketMixin implements WorldChunkPacketAccess {

	@Unique private int retroapi$extCount = 0;
	@Unique private int[] retroapi$extIndices;
	@Unique private int[] retroapi$extBlockIds;
	@Unique private int[] retroapi$extMeta;
	@Unique private int[] retroapi$xmetaPositions;
	@Unique private int[] retroapi$xmetaValues;

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
	public int[] retroapi$getXmetaPositions() {
		return retroapi$xmetaPositions;
	}

	@Override
	public int[] retroapi$getXmetaValues() {
		return retroapi$xmetaValues;
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

		// Secondary meta (state index bits 4-11), for modded and vanilla-stored positions alike.
		Map<Integer, Integer> xmeta = extended.getXmetaMap();
		if (!xmeta.isEmpty()) {
			retroapi$xmetaPositions = new int[xmeta.size()];
			retroapi$xmetaValues = new int[xmeta.size()];
			int j = 0;
			for (Map.Entry<Integer, Integer> entry : xmeta.entrySet()) {
				retroapi$xmetaPositions[j] = entry.getKey();
				retroapi$xmetaValues[j] = entry.getValue();
				j++;
			}
		}
	}

	@Inject(method = "write(Ljava/io/DataOutputStream;)V", at = @At("RETURN"))
	private void retroapi$writeExtended(DataOutputStream output, CallbackInfo ci) throws IOException {
		output.writeShort(retroapi$extCount);
		for (int i = 0; i < retroapi$extCount; i++) {
			output.writeInt(retroapi$extIndices[i]);
			output.writeShort(retroapi$extBlockIds[i]);
			output.write(retroapi$extMeta[i]);
		}
		int xCount = retroapi$xmetaPositions != null ? retroapi$xmetaPositions.length : 0;
		output.writeShort(xCount);
		for (int i = 0; i < xCount; i++) {
			output.writeInt(retroapi$xmetaPositions[i]);
			output.write(retroapi$xmetaValues[i]);
		}
	}

	@Inject(method = "read(Ljava/io/DataInputStream;)V", at = @At("RETURN"))
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
		int xCount = input.readShort() & 0xFFFF;
		if (xCount > 0) {
			retroapi$xmetaPositions = new int[xCount];
			retroapi$xmetaValues = new int[xCount];
			for (int i = 0; i < xCount; i++) {
				retroapi$xmetaPositions[i] = input.readInt();
				retroapi$xmetaValues[i] = input.read();
			}
		}
	}
}
