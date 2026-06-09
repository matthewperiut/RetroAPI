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

/**
 * The numeric-id flattening half of the ItemStack mixin: persists a RetroAPI item/block stack as a string
 * id ({@code retroapi:id} + original count/damage) and clamps the vanilla {@code id}/{@code Count} to empty
 * so a vanilla save never sees a bogus stack, resolving it back on read. This is the half that CLASHES with
 * StationAPI (which owns id flattening via {@code stationapi:id}), so it is in
 * {@code RetroAPIMixinPlugin}'s StationAPI-disabled set. Data components were split out to
 * {@link com.periut.retroapi.mixin.component.ItemStackComponentMixin}, which stays active under StationAPI.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

	@Shadow public int itemId;
	@Shadow public int count;
	@Shadow public int damage;

	@Inject(method = "writeNbt", at = @At("RETURN"))
	private void retroapi$writeNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
		String stringId = resolveStringId(this.itemId);
		if (stringId != null) {
			nbt.putString("retroapi:id", stringId);
			// Save original values so the sidecar can read them after clamping
			nbt.putByte("retroapi:count", (byte) this.count);
			nbt.putShort("retroapi:damage", (short) this.damage);
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
			this.itemId = blockReg.getBlock().id;
		} else {
			ItemRegistration itemReg = RetroRegistry.getItemById(retroId);
			if (itemReg != null) {
				this.itemId = itemReg.getItem().id;
			} else {
				// Not a RetroAPI item (could be vanilla stationapi:id like "minecraft:stone") - leave as-is
				return;
			}
		}

		// Restore count and damage from retroapi tags (vanilla readNbt set them to clamped values)
		if (nbt.contains("retroapi:count")) {
			this.count = nbt.getByte("retroapi:count") & 0xFF;
		}
		if (nbt.contains("retroapi:damage")) {
			this.damage = nbt.getShort("retroapi:damage");
		}
	}

	private static String resolveStringId(int numericId) {
		if (numericId >= 0 && numericId < Block.BLOCKS.length) {
			Block block = Block.BLOCKS[numericId];
			if (block != null) {
				BlockRegistration reg = RetroRegistry.getBlockRegistration(block);
				if (reg != null) return reg.getId().toString();
			}
		}
		if (numericId >= 0 && numericId < Item.ITEMS.length) {
			Item item = Item.ITEMS[numericId];
			if (item != null) {
				ItemRegistration reg = RetroRegistry.getItemRegistration(item);
				if (reg != null) return reg.getId().toString();
			}
		}
		return null;
	}
}
