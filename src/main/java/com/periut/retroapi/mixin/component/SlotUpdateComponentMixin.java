package com.periut.retroapi.mixin.component;

import com.periut.retroapi.component.ComponentWire;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Syncs the components of a single-slot update (the packet the server sends when a held or
 * inventory item changes). The stack is the last field, so the blob appends cleanly.
 */
@Mixin(ScreenHandlerSlotUpdateS2CPacket.class)
public class SlotUpdateComponentMixin {

	@Shadow
	public ItemStack stack;

	@Inject(method = "write", at = @At("TAIL"))
	private void retroapi$writeComponents(DataOutputStream out, CallbackInfo ci) {
		ComponentWire.write(out, this.stack);
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void retroapi$readComponents(DataInputStream in, CallbackInfo ci) {
		ComponentWire.read(in, this.stack);
	}
}
