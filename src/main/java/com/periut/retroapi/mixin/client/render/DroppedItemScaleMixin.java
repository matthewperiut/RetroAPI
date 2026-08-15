package com.periut.retroapi.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.periut.retroapi.client.render.DroppedItemScale;
import com.periut.retroapi.register.block.RetroBlockAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * How big a dropped item is drawn: what the block asked for, else what the config asked for, else
 * beta's own number.
 *
 * <p>Two separate features want this one constant, and a constant only has room for one writer.
 * {@link RetroBlockAccess#droppedItemScale(float)} lets a block state its own size instead of having
 * beta guess it from {@code isFullCube()} - see that method for why the guess is wrong for a custom
 * shape - while the tweaks half's "Dropped Item Size" makes every OTHER item the quarter size that
 * every version after b1.7.3 draws them at. Both used to write it from their own
 * {@code @ModifyConstant}, and mixin resolved that the only way it can: the higher priority won and
 * the other was skipped with a warning, which silently killed the per-block API.
 *
 * <p>So they are one injector, and the precedence is stated rather than left to a priority number: a
 * block that named a size gets that size, because a mod author asking for 0.4 means 0.4 and not "0.4
 * unless a setting says otherwise". Everything else takes the config's answer, and a config that is
 * off leaves beta's 0.5F exactly as it was.
 *
 * <p>Priority 1100 for the same reason the tweaks mixin carried it: it buys ordering against another
 * mod that injects here (UniTweaks' {@code droppeditemfix} rewrites this identical constant). It buys
 * nothing against one that REPLACES the method, which is what StationAPI's arsenic renderer does -
 * there this mixin is disabled outright by {@code RetroAPIMixinPlugin} and
 * {@code ArsenicDroppedItemScaleMixin} carries both rules instead.
 */
@Mixin(value = ItemRenderer.class, priority = 1100)
@Environment(EnvType.CLIENT)
public class DroppedItemScaleMixin {

	@ModifyConstant(method = "render(Lnet/minecraft/entity/ItemEntity;DDDFF)V",
		constant = @Constant(floatValue = 0.5F, ordinal = 0), require = 0)
	private float retroapi$droppedItemScale(float vanilla, @Local ItemStack stack) {
		return DroppedItemScale.of(stack, vanilla);
	}
}
