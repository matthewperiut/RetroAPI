package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.ItemInHandRenderer;
#if MC_B1_6_OR_LATER
import net.minecraft.entity.mob.MobEntity;
#endif
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
@Environment(EnvType.CLIENT)
public class ItemInHandRendererMixin {

	@Unique
	private int retroapi$atlasSize = 256;

	@Unique
	private int retroapi$currentItemId = 0;

#if MC_B1_6_OR_LATER
	// --- render(): 3D block check + atlas size for held items ---

	@Inject(
		method = "render(Lnet/minecraft/entity/mob/MobEntity;Lnet/minecraft/item/ItemStack;)V",
		at = @At("HEAD")
	)
	private void retroapi$captureItemId(MobEntity mob, ItemStack itemInHand, CallbackInfo ci) {
		retroapi$currentItemId = itemInHand != null ? itemInHand.id : 0;
	}

	@Inject(
		method = "render(Lnet/minecraft/entity/mob/MobEntity;Lnet/minecraft/item/ItemStack;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;getItemSprite(Lnet/minecraft/item/ItemStack;)I")
	)
	private void retroapi$setAtlasSizeForRender(MobEntity mob, ItemStack itemInHand, CallbackInfo ci) {
		retroapi$atlasSize = RetroAPI.isBlock(itemInHand.id) ? AtlasExpander.terrainAtlasSize : AtlasExpander.itemAtlasSize;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/entity/mob/MobEntity;Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(intValue = 256)
	)
	private int retroapi$fixBlockCheck(int original) {
		if (RetroAPI.isBlock(retroapi$currentItemId)) {
			return retroapi$currentItemId + 1;
		}
		return original;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/entity/mob/MobEntity;Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(floatValue = 256.0F)
	)
	private float retroapi$fixRenderDivisor(float original) {
		return (float) retroapi$atlasSize;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/entity/mob/MobEntity;Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(floatValue = 0.001953125F)
	)
	private float retroapi$fixEdgeBias(float original) {
		return 0.5F / retroapi$atlasSize;
	}
#else
	// --- render(ItemStack): b1.4-b1.5 signature (no MobEntity param) ---

	@Inject(
		method = "render(Lnet/minecraft/item/ItemStack;)V",
		at = @At("HEAD")
	)
	private void retroapi$captureItemId(ItemStack itemInHand, CallbackInfo ci) {
		retroapi$currentItemId = itemInHand != null ? itemInHand.id : 0;
		retroapi$atlasSize = RetroAPI.isBlock(retroapi$currentItemId) ? AtlasExpander.terrainAtlasSize : AtlasExpander.itemAtlasSize;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(intValue = 256),
		require = 0
	)
	private int retroapi$fixBlockCheck(int original) {
		if (RetroAPI.isBlock(retroapi$currentItemId)) {
			return retroapi$currentItemId + 1;
		}
		return original;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(floatValue = 256.0F),
		require = 0
	)
	private float retroapi$fixRenderDivisor(float original) {
		return (float) retroapi$atlasSize;
	}

	@ModifyConstant(
		method = "render(Lnet/minecraft/item/ItemStack;)V",
		constant = @Constant(floatValue = 0.001953125F),
		require = 0
	)
	private float retroapi$fixEdgeBias(float original) {
		return 0.5F / retroapi$atlasSize;
	}
#endif

	// --- renderInWallEffect: always terrain atlas ---

	@Inject(method = "renderInWallEffect", at = @At("HEAD"))
	private void retroapi$setTerrainAtlasForWall(float tickDelta, int sprite, CallbackInfo ci) {
		retroapi$atlasSize = AtlasExpander.terrainAtlasSize;
	}

	@ModifyConstant(
		method = "renderInWallEffect",
#if MC_B1_6_OR_LATER
		constant = @Constant(floatValue = 256.0F)
#else
		constant = @Constant(floatValue = 256.0F),
		require = 0
#endif
	)
	private float retroapi$fixWallEffectDivisor(float original) {
		return (float) retroapi$atlasSize;
	}

	// --- renderOnFireEffect: always terrain atlas ---

	@Inject(method = "renderOnFireEffect", at = @At("HEAD"))
	private void retroapi$setTerrainAtlasForFire(float tickDelta, CallbackInfo ci) {
		retroapi$atlasSize = AtlasExpander.terrainAtlasSize;
	}

	@ModifyConstant(
		method = "renderOnFireEffect",
#if MC_B1_6_OR_LATER
		constant = @Constant(intValue = 240)
#else
		constant = @Constant(intValue = 240),
		require = 0
#endif
	)
	private int retroapi$fixFireRowMask(int original) {
		return -16;
	}

	@ModifyConstant(
		method = "renderOnFireEffect",
#if MC_B1_6_OR_LATER
		constant = @Constant(floatValue = 256.0F)
#else
		constant = @Constant(floatValue = 256.0F),
		require = 0
#endif
	)
	private float retroapi$fixFireEffectDivisor(float original) {
		return (float) retroapi$atlasSize;
	}
}
