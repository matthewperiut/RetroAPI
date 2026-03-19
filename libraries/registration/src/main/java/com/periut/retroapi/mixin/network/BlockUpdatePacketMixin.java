package com.periut.retroapi.mixin.network;

import net.minecraft.network.packet.BlockUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Mixin(BlockUpdatePacket.class)
public class BlockUpdatePacketMixin {

	@Shadow public int x;
	@Shadow public int y;
	@Shadow public int z;
	@Shadow public int block;
	@Shadow public int metadata;

	@Inject(method = "write", at = @At("HEAD"), cancellable = true)
	private void retroapi$write(DataOutputStream output, CallbackInfo ci) throws IOException {
		output.writeInt(x);
		output.write(y);
		output.writeInt(z);
		output.writeShort(block);
		output.write(metadata);
		ci.cancel();
	}

	@Inject(method = "read", at = @At("HEAD"), cancellable = true)
	private void retroapi$read(DataInputStream input, CallbackInfo ci) throws IOException {
		x = input.readInt();
		y = input.read();
		z = input.readInt();
		block = input.readShort() & 0xFFFF;
		metadata = input.read();
		ci.cancel();
	}

	@Inject(method = "getSize", at = @At("HEAD"), cancellable = true)
	private void retroapi$getSize(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(12); // was 11: 4+1+4+1+1, now 12: 4+1+4+2+1
	}
}
