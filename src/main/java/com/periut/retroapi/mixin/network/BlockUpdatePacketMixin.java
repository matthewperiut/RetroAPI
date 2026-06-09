package com.periut.retroapi.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;

@Mixin(BlockUpdateS2CPacket.class)
public class BlockUpdatePacketMixin {

	@Shadow public int x;
	@Shadow public int y;
	@Shadow public int z;
	@Shadow public int blockRawId;
	@Shadow public int blockMetadata;

	@Inject(method = "write(Ljava/io/DataOutputStream;)V", at = @At("HEAD"), cancellable = true)
	private void retroapi$write(DataOutputStream output, CallbackInfo ci) throws IOException {
		output.writeInt(x);
		output.write(y);
		output.writeInt(z);
		output.writeShort(blockRawId);
		output.write(blockMetadata);
		ci.cancel();
	}

	@Inject(method = "read(Ljava/io/DataInputStream;)V", at = @At("HEAD"), cancellable = true)
	private void retroapi$read(DataInputStream input, CallbackInfo ci) throws IOException {
		x = input.readInt();
		y = input.read();
		z = input.readInt();
		blockRawId = input.readShort() & 0xFFFF;
		blockMetadata = input.read();
		ci.cancel();
	}

	@Inject(method = "size", at = @At("HEAD"), cancellable = true)
	private void retroapi$getSize(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(12); // was 11: 4+1+4+1+1, now 12: 4+1+4+2+1
	}
}
