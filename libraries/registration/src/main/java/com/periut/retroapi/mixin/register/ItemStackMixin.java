package com.periut.retroapi.mixin.register;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

	@Shadow public int id;
	@Shadow public int size;

	@Shadow public int metadata;

	@Inject(method = "writeNbt", at = @At("RETURN"))
	private void retroapi$writeNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
		String stringId = resolveStringId(this.id);
		if (stringId != null) {
			nbt.putString("retroapi:id", stringId);
			// Save original values so the sidecar can read them after clamping
			nbt.putByte("retroapi:count", (byte) this.size);
			nbt.putShort("retroapi:damage", (short) this.metadata);
			// Clamp to empty so vanilla sees nothing (avoids stone appearing in inventories)
			nbt.putShort("id", (short) 0);
			nbt.putByte("Count", (byte) 0);
		}
	}

	@Inject(method = "readNbt", at = @At("RETURN"))
	private void retroapi$readNbt(NbtCompound nbt, CallbackInfo ci) {
		// Try retroapi:id first, then fall back to stationapi:id (for worlds un-converted from StationAPI)
		String stringId = null;
		if (nbt.contains("retroapi:id")) {
			stringId = nbt.getString("retroapi:id");
		} else if (nbt.contains("stationapi:id")) {
			stringId = nbt.getString("stationapi:id");
		}

		if (stringId == null || stringId.isEmpty()) return;

		String[] parts = stringId.split(":", 2);
		if (parts.length != 2) return;

		NamespacedIdentifier retroId = NamespacedIdentifiers.from(parts[0], parts[1]);

		BlockRegistration blockReg = RetroRegistry.getBlockById(retroId);
		if (blockReg != null) {
			this.id = blockReg.getBlock().id;
		} else {
			ItemRegistration itemReg = RetroRegistry.getItemById(retroId);
			if (itemReg != null) {
				this.id = itemReg.getItem().id;
			} else {
				// Not a RetroAPI item (could be vanilla stationapi:id like "minecraft:stone") - leave as-is
				return;
			}
		}

		// Restore count and damage from retroapi tags (vanilla readNbt set them to clamped values)
		if (nbt.contains("retroapi:count")) {
			this.size = nbt.getByte("retroapi:count") & 0xFF;
		}
		if (nbt.contains("retroapi:damage")) {
			this.metadata = nbt.getShort("retroapi:damage");
		}
	}

	private static String resolveStringId(int numericId) {
		if (numericId >= 0 && numericId < Block.BY_ID.length) {
			Block block = Block.BY_ID[numericId];
			if (block != null) {
				BlockRegistration reg = RetroRegistry.getBlockRegistration(block);
				if (reg != null) return reg.getId().toString();
			}
		}
		if (numericId >= 0 && numericId < Item.BY_ID.length) {
			Item item = Item.BY_ID[numericId];
			if (item != null) {
				ItemRegistration reg = RetroRegistry.getItemRegistration(item);
				if (reg != null) return reg.getId().toString();
			}
		}
		return null;
	}
}
