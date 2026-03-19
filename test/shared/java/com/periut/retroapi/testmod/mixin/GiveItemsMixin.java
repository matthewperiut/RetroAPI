package com.periut.retroapi.testmod.mixin;

import com.periut.retroapi.testmod.TestMod;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class GiveItemsMixin {
	@Shadow
	public PlayerInventory inventory;

	@Unique
	private boolean retroapi_test$givenItems = false;

	@Unique
	private static void retroapi_test$safeAddItem(PlayerInventory inv, ItemStack stack) {
		if (stack.id >= 0 && stack.id < Item.BY_ID.length && Item.BY_ID[stack.id] != null) {
			inv.addItem(stack);
		} else {
			TestMod.LOGGER.error("Cannot add item with id {} - no Item.BY_ID entry!", stack.id);
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void retroapi_test$giveTestItems(CallbackInfo ci) {
		if (!retroapi_test$givenItems) {
			retroapi_test$givenItems = true;

			PlayerEntity self = (PlayerEntity) (Object) this;
			World world = self.world;
			if (world == null || world.isMultiplayer) return;

			if (TestMod.TEST_BLOCK != null) {
				retroapi_test$safeAddItem(inventory, new ItemStack(TestMod.TEST_BLOCK, 64));
				retroapi_test$safeAddItem(inventory, new ItemStack(TestMod.COLOR_BLOCK, 64));
				retroapi_test$safeAddItem(inventory, new ItemStack(TestMod.PIPE_BLOCK, 64));
			}
			if (TestMod.CRATE_BLOCK != null) {
				retroapi_test$safeAddItem(inventory, new ItemStack(TestMod.CRATE_BLOCK, 64));
			}
			if (TestMod.FREEZER_BLOCK != null) {
				retroapi_test$safeAddItem(inventory, new ItemStack(TestMod.FREEZER_BLOCK, 64));
				retroapi_test$safeAddItem(inventory, new ItemStack(Item.WATER_BUCKET, 1));
				retroapi_test$safeAddItem(inventory, new ItemStack(Item.COAL, 64));
			}
			if (TestMod.TEST_ITEM != null) {
				retroapi_test$safeAddItem(inventory, new ItemStack(TestMod.TEST_ITEM, 64));
			}

			retroapi_test$safeAddItem(inventory, new ItemStack(Item.DIAMOND_PICKAXE, 1));

			// Spawn chests filled with the 200 numbered blocks
			int px = (int) self.x;
			int py = (int) self.y;
			int pz = (int) self.z;
			int blockIndex = 0;
			int blockChestCount = (TestMod.BLOCK_COUNT + 26) / 27; // 27 slots per chest
			for (int c = 0; c < blockChestCount; c++) {
				int cx = px + 2 + (c % 8);
				int cz = pz + 2 + (c / 8);
				int cy = py;

				world.setBlock(cx, cy, cz, Block.CHEST.id);
				if (world.getBlockEntity(cx, cy, cz) instanceof ChestBlockEntity chest) {
					for (int slot = 0; slot < 27 && blockIndex < TestMod.BLOCK_COUNT; slot++, blockIndex++) {
						Block block = TestMod.BLOCKS[blockIndex];
						if (block != null && block.id >= 0 && block.id < Item.BY_ID.length && Item.BY_ID[block.id] != null) {
							chest.setItem(slot, new ItemStack(block, 64));
						} else {
							TestMod.LOGGER.error("Block {} (index {}) has id {} with no Item.BY_ID entry!",
								block, blockIndex, block != null ? block.id : "null");
						}
					}
				}
			}

			// Spawn chests filled with the 200 numbered items (offset row by 2 blocks)
			int itemIndex = 0;
			int itemChestCount = (TestMod.ITEM_COUNT + 26) / 27;
			for (int c = 0; c < itemChestCount; c++) {
				int cx = px + 2 + (c % 8);
				int cz = pz + 2 + ((blockChestCount + 7) / 8) + 1 + (c / 8);
				int cy = py;

				world.setBlock(cx, cy, cz, Block.CHEST.id);
				if (world.getBlockEntity(cx, cy, cz) instanceof ChestBlockEntity chest) {
					for (int slot = 0; slot < 27 && itemIndex < TestMod.ITEM_COUNT; slot++, itemIndex++) {
						Item item = TestMod.ITEMS[itemIndex];
						if (item != null && item.id >= 0 && item.id < Item.BY_ID.length && Item.BY_ID[item.id] != null) {
							chest.setItem(slot, new ItemStack(item, 64));
						} else {
							TestMod.LOGGER.error("Item {} (index {}) has id {} with no Item.BY_ID entry!",
								item, itemIndex, item != null ? item.id : "null");
						}
					}
				}
			}

			TestMod.LOGGER.info("Gave test items and spawned {} block chests + {} item chests", blockChestCount, itemChestCount);
		}
	}
}
