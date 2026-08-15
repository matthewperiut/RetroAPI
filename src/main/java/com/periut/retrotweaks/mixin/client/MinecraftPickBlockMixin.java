/*
 * Ported from AnnoyanceFix's mixin.MinecraftMixin (the pick-block table and boat/minecart/painting
 * entity handling) and UniTweaks' mixin.bugfixes.pickblockfix.MinecraftMixin (the id-packing
 * convention). handlePickBlock's own local variable already carries vanilla's grass->dirt,
 * double-slab->slab and bedrock->stone substitutions by the time this runs, so those stay
 * untouched unless Pick Block Behavior asks for the exact block back.
 *
 * <p>The result is packed as (itemId << 4) | (meta & 0xF) for block hits, matching what
 * PlayerInventoryPickBlockMixin (ported separately from UniTweaks' pick-block-from-inventory
 * feature) already expects to unpack - that mixin is untouched here, this one just starts feeding
 * it real data instead of a raw item id.
 */
package com.periut.retrotweaks.mixin.client;

import java.util.HashMap;
import java.util.Map;

import com.periut.retrotweaks.config.Config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;
import net.minecraft.world.World;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftPickBlockMixin {

	@Shadow public HitResult crosshairTarget;
	@Shadow public World world;

	@Unique
	private static final Map<Integer, Integer> retrotweaks$pickBlockLookupMap = new HashMap<>();

	static {
		retrotweaks$pickBlockLookupMap.put(Block.REDSTONE_WIRE.id, Item.REDSTONE.id);
		retrotweaks$pickBlockLookupMap.put(Block.REPEATER.id, Item.REPEATER.id);
		retrotweaks$pickBlockLookupMap.put(Block.POWERED_REPEATER.id, Item.REPEATER.id);
		retrotweaks$pickBlockLookupMap.put(Block.DOOR.id, Item.WOODEN_DOOR.id);
		retrotweaks$pickBlockLookupMap.put(Block.IRON_DOOR.id, Item.IRON_DOOR.id);
		retrotweaks$pickBlockLookupMap.put(Block.SIGN.id, Item.SIGN.id);
		retrotweaks$pickBlockLookupMap.put(Block.WALL_SIGN.id, Item.SIGN.id);
		retrotweaks$pickBlockLookupMap.put(Block.WHEAT.id, Item.SEEDS.id);
		retrotweaks$pickBlockLookupMap.put(Block.BED.id, Item.BED.id);
		retrotweaks$pickBlockLookupMap.put(Block.CAKE.id, Item.CAKE.id);
	}

	/**
	 * Adds functionality for picking blocks that are not in the hotbar. It also adds pick block
	 * functionality to boats, minecarts and paintings. Injected right where vanilla reads
	 * {@code this.player} to call {@code setHeldItem}, so it can swap out the id it is about to be
	 * called with.
	 */
	@ModifyVariable(
			method = "handlePickBlock",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/Minecraft;player:Lnet/minecraft/entity/player/ClientPlayerEntity;",
					opcode = Opcodes.GETFIELD
			),
			index = 1
	)
	private int retrotweaks$pickBlock(int vanillaItemId) {
		if (!Config.GAMEPLAY.pickBlockFixes) {
			return vanillaItemId;
		}

		if (this.crosshairTarget.type == HitResultType.ENTITY) {
			int itemId = retrotweaks$getItemIdFromEntity(this.crosshairTarget.entity);
			if (itemId == 0) {
				return vanillaItemId;
			}
			return itemId;
		}

		if (this.crosshairTarget.type == HitResultType.BLOCK) {
			int tileMeta = this.world.getBlockMeta(this.crosshairTarget.blockX, this.crosshairTarget.blockY, this.crosshairTarget.blockZ);
			int tileId = this.world.getBlockId(this.crosshairTarget.blockX, this.crosshairTarget.blockY, this.crosshairTarget.blockZ);
			int itemId = retrotweaks$getItemIdFromTileId(vanillaItemId, tileId, tileMeta);
			return (itemId << 4) | (tileMeta & 0xF);
		}

		return vanillaItemId;
	}

	/**
	 * Returns a corresponding item id for a given tile. For example, when looking at crops, it
	 * should return the seeds item. Falls back to {@code vanillaItemId} (already carrying vanilla's
	 * own grass/double-slab/bedrock substitutions) when nothing special applies.
	 */
	@Unique
	private int retrotweaks$getItemIdFromTileId(int vanillaItemId, int tileId, int tileMeta) {
		// Special case: pistons store extension state in meta, not in a separate block id.
		if (tileId == Block.PISTON_HEAD.id) {
			// 0-5 damage = normal piston, 8-13 damage = sticky piston
			if (tileMeta < 8) {
				return Block.PISTON.id;
			} else {
				return Block.STICKY_PISTON.id;
			}
		}

		if (retrotweaks$pickBlockLookupMap.containsKey(tileId)) {
			return retrotweaks$pickBlockLookupMap.get(tileId);
		}

		if (Config.GAMEPLAY.pickBlockBehavior.ordinal() > 1 && tileId == Block.GRASS_BLOCK.id) {
			return Item.ITEMS[Block.GRASS_BLOCK.id].id;
		} else if (Config.GAMEPLAY.pickBlockBehavior.ordinal() > 1 && tileId == Block.BEDROCK.id) {
			return Item.ITEMS[Block.BEDROCK.id].id;
		} else if (tileId == Block.LIT_FURNACE.id) {
			return Item.ITEMS[Block.FURNACE.id].id;
		} else if (tileId == Block.REDSTONE_TORCH.id) {
			return Item.ITEMS[Block.LIT_REDSTONE_TORCH.id].id;
		} else {
			return vanillaItemId;
		}
	}

	/**
	 * Returns a corresponding item id for a given entity. For example, when looking at a boat, it
	 * should return a boat. Returns 0 when the entity has no pickable item.
	 */
	@Unique
	private int retrotweaks$getItemIdFromEntity(Entity entity) {
		if (entity instanceof PaintingEntity) {
			return Item.PAINTING.id;
		}

		if (entity instanceof BoatEntity) {
			return Item.BOAT.id;
		}

		if (entity instanceof MinecartEntity) {
			MinecartEntity minecart = (MinecartEntity) entity;
			switch (minecart.type) {
				case 1:
					return Item.CHEST_MINECART.id;
				case 2:
					return Item.FURNACE_MINECART.id;
				default:
					return Item.MINECART.id;
			}
		}

		return 0;
	}
}
