package com.periut.retroapi.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.periut.retroapi.register.block.RetroBlockAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Lets a block say how big its dropped form is drawn, instead of beta deciding from
 * {@code isFullCube()}. See {@link RetroBlockAccess#droppedItemScale(float)} for why that decision is
 * wrong for a block with a custom shape.
 *
 * <p>Only the one constant is touched, and only for a block that asked. A block that never calls
 * {@code droppedItemScale} keeps beta's number exactly, so this changes nothing about how vanilla
 * content looks.
 */
@Mixin(ItemRenderer.class)
@Environment(EnvType.CLIENT)
public class DroppedItemScaleMixin {

	@ModifyConstant(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		constant = @Constant(floatValue = 0.5F, ordinal = 0), require = 0)
	private float retroapi$droppedItemScale(float vanilla, @Local ItemStack stack) {
		if (stack == null) {
			return vanilla;
		}
		int id = stack.itemId;
		if (id <= 0 || id >= Block.BLOCKS.length) {
			return vanilla;
		}
		Block block = Block.BLOCKS[id];
		if (block == null) {
			return vanilla;
		}
		float declared = ((RetroBlockAccess) block).getDroppedItemScale();
		return declared > 0.0F ? declared : vanilla;
	}
}
