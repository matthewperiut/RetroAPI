package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
@Environment(EnvType.CLIENT)
public class ItemInHandRendererMixin {

	@Unique
	private int retroapi$atlasSize = 256;

	@Unique
	private int retroapi$currentItemId = 0;

	// --- render(): b1.6-b1.8 takes (MobEntity, ItemStack) ---

	@Inject(
		method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
		at = @At("HEAD")
	)
	private void retroapi$captureItemId(LivingEntity mob, ItemStack itemInHand, CallbackInfo ci) {
		retroapi$currentItemId = itemInHand != null ? itemInHand.itemId : 0;
	}

	@Inject(
		method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getItemStackTextureId(Lnet/minecraft/item/ItemStack;)I")
	)
	private void retroapi$setAtlasSizeForRender(LivingEntity mob, ItemStack itemInHand, CallbackInfo ci) {
		retroapi$atlasSize = RetroAPI.isBlock(itemInHand.itemId) ? AtlasExpander.terrainAtlasSize : AtlasExpander.itemAtlasSize;
	}

	@ModifyConstant(
		method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(intValue = 256)
	)
	private int retroapi$fixBlockCheck(int original) {
		if (RetroAPI.isBlock(retroapi$currentItemId)) {
			return retroapi$currentItemId + 1;
		}
		return original;
	}

	@ModifyConstant(
		method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(floatValue = 256.0F)
	)
	private float retroapi$fixRenderDivisor(float original) {
		return (float) retroapi$atlasSize;
	}

	@ModifyConstant(
		method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(floatValue = 0.001953125F)
	)
	private float retroapi$fixEdgeBias(float original) {
		return 0.5F / retroapi$atlasSize;
	}

	// --- renderInWallEffect: always terrain atlas ---

	@Inject(method = "renderTexturedOverlay", at = @At("HEAD"))
	private void retroapi$setTerrainAtlasForWall(float tickDelta, int sprite, CallbackInfo ci) {
		retroapi$atlasSize = AtlasExpander.terrainAtlasSize;
	}

	@ModifyConstant(
		method = "renderTexturedOverlay",
		constant = @Constant(floatValue = 256.0F)
	)
	private float retroapi$fixWallEffectDivisor(float original) {
		return (float) retroapi$atlasSize;
	}

	// --- renderOnFireEffect: always terrain atlas ---

	@Inject(method = "renderFireOverlay", at = @At("HEAD"))
	private void retroapi$setTerrainAtlasForFire(float tickDelta, CallbackInfo ci) {
		retroapi$atlasSize = AtlasExpander.terrainAtlasSize;
	}

	@ModifyConstant(
		method = "renderFireOverlay",
		constant = @Constant(intValue = 240)
	)
	private int retroapi$fixFireRowMask(int original) {
		return -16;
	}

	@ModifyConstant(
		method = "renderFireOverlay",
		constant = @Constant(floatValue = 256.0F)
	)
	private float retroapi$fixFireEffectDivisor(float original) {
		return (float) retroapi$atlasSize;
	}
}

