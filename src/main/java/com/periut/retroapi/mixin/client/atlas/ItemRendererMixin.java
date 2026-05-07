package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
@Environment(EnvType.CLIENT)
public class ItemRendererMixin {

	@Unique
	private int retroapi$atlasSize = 256;

	@Unique
	private int retroapi$currentItemId = 0;

	// --- render() for dropped items ---

	@Inject(
		method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		at = @At("HEAD")
	)
	private void retroapi$captureItemIdForRender(ItemEntity itemEntity, double d, double e, double f, float g, float h, CallbackInfo ci) {
		retroapi$currentItemId = itemEntity.item.id;
		retroapi$atlasSize = 256;
	}

	@Inject(
		method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getSprite()I")
	)
	private void retroapi$setAtlasSizeForRender(ItemEntity itemEntity, double d, double e, double f, float g, float h, CallbackInfo ci) {
		retroapi$atlasSize = RetroAPI.isBlock(itemEntity.item.id) ? AtlasExpander.terrainAtlasSize : AtlasExpander.itemAtlasSize;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		constant = @Constant(intValue = 256)
	)
	private int retroapi$fixBlockCheckInRender(int original) {
		if (RetroAPI.isBlock(retroapi$currentItemId)) {
			return retroapi$currentItemId + 1;
		}
		return original;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		constant = @Constant(floatValue = 256.0F)
	)
	private float retroapi$fixRenderDivisor(float original) {
		return (float) retroapi$atlasSize;
	}

	// --- renderGuiItem: b1.5+ takes (TextRenderer, TextureManager, int, int, int, int, int) ---

	@Inject(
		method = "renderGuiItem",
		at = @At("HEAD")
	)
	private void retroapi$captureItemIdForGui(
		net.minecraft.client.render.TextRenderer textRenderer,
		net.minecraft.client.render.texture.TextureManager textureManager,
		int item, int metadata, int sprite, int x, int y,
		CallbackInfo ci
	) {
		retroapi$currentItemId = item;
	}

	@Inject(
		method = "renderGuiItem",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/ItemRenderer;drawTexture(IIIIII)V")
	)
	private void retroapi$setAtlasSizeForGui(
		net.minecraft.client.render.TextRenderer textRenderer,
		net.minecraft.client.render.texture.TextureManager textureManager,
		int item, int metadata, int sprite, int x, int y,
		CallbackInfo ci
	) {
		retroapi$atlasSize = RetroAPI.isBlock(item) ? AtlasExpander.terrainAtlasSize : AtlasExpander.itemAtlasSize;
	}

	@ModifyConstant(
		method = "renderGuiItem",
		constant = @Constant(intValue = 256)
	)
	private int retroapi$fixBlockCheckInGui(int original) {
		if (RetroAPI.isBlock(retroapi$currentItemId)) {
			return retroapi$currentItemId + 1;
		}
		return original;
	}

	@ModifyConstant(
		method = "drawTexture",
		constant = @Constant(floatValue = 0.00390625F)
	)
	private float retroapi$fixDrawTextureScale(float original) {
		if (retroapi$atlasSize != 256) {
			return 1.0F / retroapi$atlasSize;
		}
		return original;
	}
}

