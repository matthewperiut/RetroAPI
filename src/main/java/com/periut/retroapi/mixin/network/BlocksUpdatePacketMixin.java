package com.periut.retroapi.mixin.network;

import com.periut.retroapi.network.BlocksUpdatePacketAccess;
import net.minecraft.network.packet.BlocksUpdatePacket;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Mixin(BlocksUpdatePacket.class)
public class BlocksUpdatePacketMixin implements BlocksUpdatePacketAccess {

	@Shadow public int chunkX;
	@Shadow public int chunkZ;
	@Shadow public short[] positions;
	@Shadow public byte[] blockIds;
	@Shadow public byte[] blockMetadata;
	@Shadow public int blockChangeCount;

	@Unique
	private short[] retroapi$fullBlockIds;

	@Override
	public short[] retroapi$getFullBlockIds() {
		return retroapi$fullBlockIds;
	}

	@Override
	public void retroapi$populateFullIds(WorldChunk chunk) {
		retroapi$fullBlockIds = new short[blockChangeCount];
		if (chunk == null) return;
		for (int i = 0; i < blockChangeCount; i++) {
			short pos = positions[i];
			int localX = pos >> 12 & 15;
			int localZ = pos >> 8 & 15;
			int y = pos & 255;
			retroapi$fullBlockIds[i] = (short) chunk.getBlockAt(localX, y, localZ);
		}
	}

	@Inject(method = "write", at = @At("HEAD"), cancellable = true)
	private void retroapi$write(DataOutputStream output, CallbackInfo ci) throws IOException {
		output.writeInt(chunkX);
		output.writeInt(chunkZ);
		output.writeShort(blockChangeCount);
		for (int i = 0; i < blockChangeCount; i++) {
			output.writeShort(positions[i]);
		}
		for (int i = 0; i < blockChangeCount; i++) {
			if (retroapi$fullBlockIds != null) {
				output.writeShort(retroapi$fullBlockIds[i]);
			} else {
				output.writeShort(blockIds[i] & 0xFF);
			}
		}
		for (int i = 0; i < blockChangeCount; i++) {
			output.write(blockMetadata[i]);
		}
		ci.cancel();
	}

	@Inject(method = "read", at = @At("HEAD"), cancellable = true)
	private void retroapi$read(DataInputStream input, CallbackInfo ci) throws IOException {
		chunkX = input.readInt();
		chunkZ = input.readInt();
		blockChangeCount = input.readShort() & 0xFFFF;
		positions = new short[blockChangeCount];
		blockIds = new byte[blockChangeCount];
		blockMetadata = new byte[blockChangeCount];
		retroapi$fullBlockIds = new short[blockChangeCount];
		for (int i = 0; i < blockChangeCount; i++) {
			positions[i] = input.readShort();
		}
		for (int i = 0; i < blockChangeCount; i++) {
			retroapi$fullBlockIds[i] = input.readShort();
			blockIds[i] = (byte) retroapi$fullBlockIds[i];
		}
		for (int i = 0; i < blockChangeCount; i++) {
			blockMetadata[i] = (byte) input.read();
		}
		ci.cancel();
	}

	@Inject(method = "getSize", at = @At("HEAD"), cancellable = true)
	private void retroapi$getSize(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(10 + blockChangeCount * 5);
	}
}
