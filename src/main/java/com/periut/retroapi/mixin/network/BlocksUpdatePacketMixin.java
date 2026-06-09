package com.periut.retroapi.mixin.network;

import com.periut.retroapi.network.BlocksUpdatePacketAccess;
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
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.world.chunk.Chunk;

@Mixin(ChunkDeltaUpdateS2CPacket.class)
public class BlocksUpdatePacketMixin implements BlocksUpdatePacketAccess {

	@Shadow public int x;
	@Shadow public int z;
	@Shadow public short[] positions;
	@Shadow public byte[] blockRawIds;
	@Shadow public byte[] blockMetadata;
	@Shadow public int size;

	@Unique
	private short[] retroapi$fullBlockIds;

	@Override
	public short[] retroapi$getFullBlockIds() {
		return retroapi$fullBlockIds;
	}

	@Override
	public void retroapi$populateFullIds(Chunk chunk) {
		retroapi$fullBlockIds = new short[size];
		if (chunk == null) return;
		for (int i = 0; i < size; i++) {
			short pos = positions[i];
			int localX = pos >> 12 & 15;
			int localZ = pos >> 8 & 15;
			int y = pos & 255;
			retroapi$fullBlockIds[i] = (short) chunk.getBlockId(localX, y, localZ);
		}
	}

	@Inject(method = "write(Ljava/io/DataOutputStream;)V", at = @At("HEAD"), cancellable = true)
	private void retroapi$write(DataOutputStream output, CallbackInfo ci) throws IOException {
		output.writeInt(x);
		output.writeInt(z);
		output.writeShort(size);
		for (int i = 0; i < size; i++) {
			output.writeShort(positions[i]);
		}
		for (int i = 0; i < size; i++) {
			if (retroapi$fullBlockIds != null) {
				output.writeShort(retroapi$fullBlockIds[i]);
			} else {
				output.writeShort(blockRawIds[i] & 0xFF);
			}
		}
		for (int i = 0; i < size; i++) {
			output.write(blockMetadata[i]);
		}
		ci.cancel();
	}

	@Inject(method = "read(Ljava/io/DataInputStream;)V", at = @At("HEAD"), cancellable = true)
	private void retroapi$read(DataInputStream input, CallbackInfo ci) throws IOException {
		x = input.readInt();
		z = input.readInt();
		size = input.readShort() & 0xFFFF;
		positions = new short[size];
		blockRawIds = new byte[size];
		blockMetadata = new byte[size];
		retroapi$fullBlockIds = new short[size];
		for (int i = 0; i < size; i++) {
			positions[i] = input.readShort();
		}
		for (int i = 0; i < size; i++) {
			retroapi$fullBlockIds[i] = input.readShort();
			blockRawIds[i] = (byte) retroapi$fullBlockIds[i];
		}
		for (int i = 0; i < size; i++) {
			blockMetadata[i] = (byte) input.read();
		}
		ci.cancel();
	}

	@Inject(method = "size", at = @At("HEAD"), cancellable = true)
	private void retroapi$getSize(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(10 + size * 5);
	}
}
