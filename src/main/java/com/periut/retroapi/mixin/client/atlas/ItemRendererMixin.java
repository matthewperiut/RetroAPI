package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.register.block.RetroTextures;
import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws items off the expanded atlases: which one to bind, and the divisor to normalise against.
 *
 * <p>Both questions come down to beta's {@code id < 256} test, which sorts vanilla blocks from vanilla
 * items and means nothing for a modded id, since all of those are above 256. The answer is
 * {@link RetroTextures#drawsFromTerrainAtlas}: a modded block reads from terrain like a vanilla one,
 * unless it has declared an item icon of its own with {@link RetroTextures#setItemSprite}.
 *
 * <p>Worth knowing when reading {@code renderGuiItem}: that test appears TWICE in it, once to pick a 3D
 * block over a flat sprite and once to pick the atlas, and the constant edit below applies to both. A
 * block sent down the flat path by {@code isSideLit} still meets the second one.
 */
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
		retroapi$currentItemId = itemEntity.stack.itemId;
		retroapi$atlasSize = 256;
	}

	@Inject(
		method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getTextureId()I")
	)
	private void retroapi$setAtlasSizeForRender(ItemEntity itemEntity, double d, double e, double f, float g, float h, CallbackInfo ci) {
		retroapi$atlasSize = RetroTextures.drawsFromTerrainAtlas(itemEntity.stack.itemId) ? AtlasExpander.terrainAtlasSize : AtlasExpander.itemAtlasSize;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		constant = @Constant(intValue = 256)
	)
	private int retroapi$fixBlockCheckInRender(int original) {
		if (RetroTextures.drawsFromTerrainAtlas(retroapi$currentItemId)) {
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
		method = "renderGuiItem(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/texture/TextureManager;IIIII)V",
		at = @At("HEAD")
	)
	private void retroapi$captureItemIdForGui(
		net.minecraft.client.font.TextRenderer textRenderer,
		net.minecraft.client.texture.TextureManager textureManager,
		int item, int metadata, int sprite, int x, int y,
		CallbackInfo ci
	) {
		retroapi$currentItemId = item;
	}


	@Inject(
		method = "renderGuiItem(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/texture/TextureManager;IIIII)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;drawTexture(IIIIII)V")
	)
	private void retroapi$setAtlasSizeForGui(
		net.minecraft.client.font.TextRenderer textRenderer,
		net.minecraft.client.texture.TextureManager textureManager,
		int item, int metadata, int sprite, int x, int y,
		CallbackInfo ci
	) {
		retroapi$atlasSize = RetroTextures.drawsFromTerrainAtlas(item) ? AtlasExpander.terrainAtlasSize : AtlasExpander.itemAtlasSize;
	}

	@ModifyConstant(
		method = "renderGuiItem(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/texture/TextureManager;IIIII)V",
		constant = @Constant(intValue = 256)
	)
	private int retroapi$fixBlockCheckInGui(int original) {
		if (RetroTextures.drawsFromTerrainAtlas(retroapi$currentItemId)) {
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

