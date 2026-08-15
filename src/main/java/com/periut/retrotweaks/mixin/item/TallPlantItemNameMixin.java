package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.flora.TallPlantVariants;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Names the three tall-plant variants apart: Dead Shrub, Short Grass and Fern.
 *
 * <p>{@code BlockItem.getTranslationKey(ItemStack)} ignores the stack entirely and answers the block's
 * single name, so all three metas of block 31 share one label. Varying the key by damage is vanilla's
 * own mechanism for exactly this - it is how wool, dye and saplings get a name per colour or species -
 * so no new item, no registry entry and no model is involved. It only became possible once
 * {@code TallPlantBlockMixin.getDroppedItemMeta} stopped flattening every sheared plant to meta 0.
 *
 * <p>"Dead Shrub" rather than "Dead Bush" for meta 0: block 32 is a separate block that draws with the
 * same sprite and is already called Dead Bush, and two identical labels on two different blocks is a
 * worse answer than the one RetroTweaks' own options already use ("Hide Dead Shrubs").
 *
 * <p>The key it returns always resolves, on every setup - {@link
 * com.periut.retrotweaks.mixin.client.TallPlantNameShimMixin} answers it when no lang loader
 * has - so unlike an earlier version of this file there is nothing to fall back to and no need to
 * probe the translation table first.
 */
@Mixin(BlockItem.class)
public class TallPlantItemNameMixin {

	@Inject(method = "getTranslationKey(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;",
		at = @At("HEAD"), cancellable = true)
	private void retrotweaks$tallPlantVariantName(ItemStack stack, CallbackInfoReturnable<String> cir) {
		if (!Config.WORLD.flora.tallGrassItems || stack == null) return;
		if (stack.itemId != Block.GRASS.id) return;
		int meta = stack.getDamage();
		if (!TallPlantVariants.isVariant(meta)) return;
		cir.setReturnValue(TallPlantVariants.TRANSLATION_KEYS[meta]);
	}
}
