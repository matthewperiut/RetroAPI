package com.periut.retrotweaks.mixin.entity;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.mixin.client.MinecraftAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pick block reaches the whole inventory, not just the hotbar. From UniTweaks.
 *
 * <p>Vanilla gives up unless the block is already on the hotbar, which makes the key close to
 * useless while building. When it is further in, the item is swapped into the selected hotbar slot
 * with the same two clicks a player would make by hand - so the server sees an ordinary inventory
 * move and no new packet is needed.
 *
 * <p>Whether metadata is checked at all - and, if so, whether a meta-less item is still accepted
 * when no exact-meta match exists - is driven by Config.GAMEPLAY.pickBlockBehavior, ported from
 * AnnoyanceFix's PickBlockEnum. Do Not Check Meta-Data matches the first stack with the right item
 * id, ignoring damage entirely (vanilla indexOf's own behaviour). Check Meta-Data and Check More
 * Exact both require an exact damage match and report nothing found rather than falling back to
 * any stack of that item - AnnoyanceFix's own PlayerInventoryMixin makes no distinction between
 * the two here; "more exact" only changes MinecraftPickBlockMixin's grass block/bedrock handling.
 */
@Environment(EnvType.CLIENT)
@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryPickBlockMixin {

	@Shadow public ItemStack[] main;
	@Shadow public int selectedSlot;

	/**
	 * Inventory slot index to the slot id the player screen handler uses. The inventory array runs
	 * hotbar-first; the handler runs hotbar-last, so the two cannot simply be added to.
	 */
	@Unique
	private static final int[] RETROTWEAKS_SLOT_TO_HANDLER = {
		36, 37, 38, 39, 40, 41, 42, 43, 44,
		27, 28, 29, 30, 31, 32, 33, 34, 35,
		18, 19, 20, 21, 22, 23, 24, 25, 26,
		9, 10, 11, 12, 13, 14, 15, 16, 17
	};

	@Inject(method = "setHeldItem", at = @At("HEAD"), cancellable = true)
	private void retrotweaks$pickFromWholeInventory(int packedId, boolean testInteractionManager, CallbackInfo ci) {
		if (!Config.GAMEPLAY.pickBlockFromInventory) return;

		// The id arrives packed: metadata in the low nibble, item id above it.
		int meta = packedId & 0xF;
		int itemId = packedId >>> 4;

		int found;
		if (Config.GAMEPLAY.pickBlockBehavior == Enums.PickBlock.NO_CHECK_META) {
			found = retrotweaks$find(itemId, -1);
		} else {
			found = retrotweaks$find(itemId, meta);
		}
		if (found < 0) {
			ci.cancel();
			return;
		}

		if (found < 9) {
			this.selectedSlot = found;
			ci.cancel();
			return;
		}

		Minecraft minecraft = MinecraftAccessor.retrotweaks$getInstance();
		if (minecraft == null || minecraft.player == null || minecraft.interactionManager == null) return;
		// Pick the stack up, drop it on the selected hotbar slot, put whatever was there back.
		int from = RETROTWEAKS_SLOT_TO_HANDLER[found];
		int to = RETROTWEAKS_SLOT_TO_HANDLER[this.selectedSlot];
		minecraft.interactionManager.clickSlot(0, from, 0, false, minecraft.player);
		minecraft.interactionManager.clickSlot(0, to, 0, false, minecraft.player);
		if (minecraft.player.inventory.getCursorStack() != null) {
			minecraft.interactionManager.clickSlot(0, from, 0, false, minecraft.player);
		}
		ci.cancel();
	}

	@Unique
	private int retrotweaks$find(int itemId, int meta) {
		for (int i = 0; i < this.main.length && i < RETROTWEAKS_SLOT_TO_HANDLER.length; i++) {
			ItemStack stack = this.main[i];
			if (stack == null || stack.itemId != itemId) continue;
			if (meta >= 0 && stack.getDamage() != meta) continue;
			return i;
		}
		return -1;
	}
}
