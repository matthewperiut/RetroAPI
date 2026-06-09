package com.periut.retroapi.mixin.register;

import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.storage.PlayerItemSidecar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player-inventory leg of the sidecar architecture: modded stacks are stripped OUT of the vanilla
 * {@code Inventory} NBT on save ({@code writeNbt} TAIL - the {@code retroapi:id} stamps that
 * {@code ItemStackMixin} wrote identify them) and injected back INTO the list on load
 * ({@code readNbt} HEAD, before vanilla parses it) - same slot if free, else the next free main
 * slot; items whose mod is missing (or with no free slot) stay in the sidecar for a later load.
 *
 * <p>Same embed-into-loading philosophy as {@code PlayerDimensionMixin}: vanilla's own code paths
 * parse the restored entries, so behavior is exactly as if they had always been in the file. The
 * in-place {@code retroapi:id} stamps alone were not vanilla-proof for player inventories: vanilla
 * play culls count-0 ghost stacks and vanilla saves drop the custom keys, destroying the items.
 * Old saves with in-place stamps still load (ItemStackMixin resolves them) and migrate to the
 * sidecar on their first save here.
 *
 * <p>Disabled under StationAPI (it owns item flattening there).
 */
@Mixin(PlayerEntity.class)
public class PlayerInventorySidecarMixin {
	private static final int MAIN_SIZE = 36;
	private static final int ARMOR_BASE = 100;
	private static final int ARMOR_SIZE = 4;

	@Inject(method = "writeNbt", at = @At("TAIL"))
	private void retroapi$stripModdedItems(NbtCompound nbt, CallbackInfo ci) {
		if (!PlayerItemSidecar.isAvailable()) return;
		PlayerEntity self = (PlayerEntity) (Object) this;
		NbtList inventory = nbt.getList("Inventory");
		if (inventory == null) return;

		NbtList kept = new NbtList();
		NbtList modded = new NbtList();
		for (int i = 0; i < inventory.size(); i++) {
			NbtCompound entry = (NbtCompound) inventory.get(i);
			if (entry.contains("retroapi:id")) {
				NbtCompound m = new NbtCompound();
				m.putByte("Slot", entry.getByte("Slot"));
				m.putString("id", entry.getString("retroapi:id"));
				m.putByte("Count", entry.contains("retroapi:count")
						? entry.getByte("retroapi:count") : entry.getByte("Count"));
				m.putShort("Damage", entry.contains("retroapi:damage")
						? entry.getShort("retroapi:damage") : entry.getShort("Damage"));
				// Carry the item's data components through the sidecar too.
				if (entry.contains("retroapi:components")) {
					m.put("retroapi:components", entry.getCompound("retroapi:components"));
				}
				modded.add(m);
			} else {
				kept.add(entry);
			}
		}

		nbt.put("Inventory", kept);
		// Always record - an empty list clears the player's entry (items were dropped/used up),
		// and recordItems merges in any load-time leftovers so they survive.
		PlayerItemSidecar.recordItems(self.name, modded);
	}

	@Inject(method = "readNbt", at = @At("HEAD"))
	private void retroapi$injectModdedItems(NbtCompound nbt, CallbackInfo ci) {
		PlayerEntity self = (PlayerEntity) (Object) this;
		NbtList recorded = PlayerItemSidecar.getItems(self.name);
		if (recorded == null || recorded.size() == 0) return;

		NbtList inventory = nbt.getList("Inventory");
		if (inventory == null) {
			inventory = new NbtList();
		}

		// Occupancy from the entries already in the vanilla list.
		boolean[] mainTaken = new boolean[MAIN_SIZE];
		boolean[] armorTaken = new boolean[ARMOR_SIZE];
		for (int i = 0; i < inventory.size(); i++) {
			int slot = ((NbtCompound) inventory.get(i)).getByte("Slot") & 255;
			if (slot < MAIN_SIZE) {
				mainTaken[slot] = true;
			} else if (slot >= ARMOR_BASE && slot < ARMOR_BASE + ARMOR_SIZE) {
				armorTaken[slot - ARMOR_BASE] = true;
			}
		}

		NbtList leftover = new NbtList();
		for (int i = 0; i < recorded.size(); i++) {
			NbtCompound m = (NbtCompound) recorded.get(i);
			Item item = RetroRegistry.getItemByStringId(m.getString("id"));
			if (item == null) {
				// Mod currently missing - keep for a future load.
				leftover.add(m);
				continue;
			}

			// Original slot if free, else the next free main slot.
			int desired = m.getByte("Slot") & 255;
			int target = -1;
			if (desired < MAIN_SIZE && !mainTaken[desired]) {
				target = desired;
			} else if (desired >= ARMOR_BASE && desired < ARMOR_BASE + ARMOR_SIZE && !armorTaken[desired - ARMOR_BASE]) {
				target = desired;
			} else {
				for (int s = 0; s < MAIN_SIZE; s++) {
					if (!mainTaken[s]) {
						target = s;
						break;
					}
				}
			}
			if (target == -1) {
				// Inventory full of vanilla items - keep for a future load.
				leftover.add(m);
				continue;
			}
			if (target < MAIN_SIZE) {
				mainTaken[target] = true;
			} else {
				armorTaken[target - ARMOR_BASE] = true;
			}

			NbtCompound entry = new NbtCompound();
			entry.putByte("Slot", (byte) target);
			entry.putShort("id", (short) item.id);
			entry.putByte("Count", m.getByte("Count"));
			entry.putShort("Damage", m.getShort("Damage"));
			// Re-attach components so ItemStack.readNbt restores them on load.
			if (m.contains("retroapi:components")) {
				entry.put("retroapi:components", m.getCompound("retroapi:components"));
			}
			inventory.add(entry);
		}

		nbt.put("Inventory", inventory);
		PlayerItemSidecar.setLeftover(self.name, leftover);
	}
}
