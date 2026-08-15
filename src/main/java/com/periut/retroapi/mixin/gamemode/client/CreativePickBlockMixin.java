package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.gamemode.GameModeNetworking;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import com.periut.retroapi.register.item.ObtainableItems;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Middle click as a block picker, which is what modern made it.
 *
 * <p>Beta's own {@code handlePickBlock} can only ever SELECT: it looks the block up in the hotbar and
 * moves the highlight if it finds it, so pointing at something you are not already carrying does
 * nothing at all. Modern's {@code Inventory.setPickedItem} is three cases and this is the same three:
 * already in the hotbar, select it; elsewhere in the inventory, swap it into the held slot; nowhere,
 * and the player is creative, so make one - into the first empty hotbar slot, or the held one.
 *
 * <p>Survival is left with vanilla's behaviour, as in modern, where picking a block you do not have is
 * a creative-only power.
 */
@Mixin(Minecraft.class)
public class CreativePickBlockMixin {

	@Shadow public ClientPlayerEntity player;
	@Shadow public World world;
	@Shadow public HitResult crosshairTarget;

	@Inject(method = "handlePickBlock", at = @At("HEAD"), cancellable = true)
	private void retroapi$creativePick(CallbackInfo ci) {
		if (player == null || world == null || crosshairTarget == null
			|| RetroGameModes.get(player) != RetroGameMode.CREATIVE) {
			return;
		}

		final int targeted = world.getBlockId(crosshairTarget.blockX, crosshairTarget.blockY, crosshairTarget.blockZ);

		// What that block is HELD as, which is not always the block itself: a lit furnace picks as a
		// furnace, a door block as its door item, redstone wire as redstone dust, a double slab as the
		// slab it is made of. Same table the give command filters with, so what can be picked and what
		// can be given are one answer rather than two that drift.
		//
		// Grass and bedrock are NOT swapped out - beta only did that because survival could not hold
		// them, and creative can.
		final int id = ObtainableItems.pickedItemId(targeted);
		if (id <= 0 || id >= Item.ITEMS.length || Item.ITEMS[id] == null) {
			ci.cancel();
			return;
		}

		// Metadata only where the game itself says it means a variant, so wool comes back the right
		// colour while a leaf block's decay bits do not turn into a damage value nothing can use. A
		// substituted item keeps the meta too, which is what makes a double slab pick as its own kind
		// of slab.
		final int meta = Item.ITEMS[id].hasSubtypes()
			? world.getBlockMeta(crosshairTarget.blockX, crosshairTarget.blockY, crosshairTarget.blockZ)
			: 0;

		retroapi$pick(player.inventory, new ItemStack(id, 1, meta));
		ci.cancel();
	}

	/** Modern's {@code Inventory.setPickedItem}, with beta's arrays. */
	private static void retroapi$pick(PlayerInventory inventory, ItemStack stack) {
		final int found = retroapi$find(inventory, stack);

		if (found >= 0 && found < 9) {
			inventory.selectedSlot = found;
			return;
		}

		if (found >= 9) {
			// In the inventory but not to hand: swap it with whatever is held, as modern's pickSlot does.
			final ItemStack held = inventory.main[inventory.selectedSlot];
			inventory.main[inventory.selectedSlot] = inventory.main[found];
			inventory.main[found] = held;
			retroapi$send(inventory.selectedSlot, inventory.main[inventory.selectedSlot]);
			retroapi$send(found, inventory.main[found]);
			return;
		}

		// Nowhere: creative makes one, preferring an empty slot over throwing away what is held.
		int slot = inventory.selectedSlot;
		for (int candidate = 0; candidate < 9; candidate++) {
			if (inventory.main[candidate] == null) {
				slot = candidate;
				break;
			}
		}

		inventory.main[slot] = stack;
		inventory.selectedSlot = slot;
		retroapi$send(slot, stack);
	}

	/** The first slot holding this exact item, or -1. */
	private static int retroapi$find(PlayerInventory inventory, ItemStack stack) {
		for (int slot = 0; slot < inventory.main.length; slot++) {
			final ItemStack candidate = inventory.main[slot];
			if (candidate != null && candidate.itemId == stack.itemId
				&& candidate.getDamage() == stack.getDamage()) {
				return slot;
			}
		}
		return -1;
	}

	/** Tells the server, which is the same path the creative screen's own slot edits take. */
	private static void retroapi$send(int slot, ItemStack stack) {
		GameModeNetworking.setCreativeSlot(slot, stack);
	}
}
