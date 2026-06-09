package com.periut.retroapi.mixin.component;

import com.periut.retroapi.component.ComponentWire;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Syncs components for a whole-inventory update (the packet that fills a window or the
 * player's inventory on open/refresh). One blob per slot, in array order, appended after
 * all the stacks (which are the packet's last fields).
 */
@Mixin(InventoryS2CPacket.class)
public class InventoryComponentMixin {

	@Shadow
	public ItemStack[] contents;

	@Inject(method = "write", at = @At("TAIL"))
	private void retroapi$writeComponents(DataOutputStream out, CallbackInfo ci) {
		if (this.contents != null) {
			for (ItemStack stack : this.contents) {
				ComponentWire.write(out, stack);
			}
		}
	}

	@Inject(method = "read", at = @At("TAIL"))
	private void retroapi$readComponents(DataInputStream in, CallbackInfo ci) {
		if (this.contents != null) {
			for (ItemStack stack : this.contents) {
				ComponentWire.read(in, stack);
			}
		}
	}
}
