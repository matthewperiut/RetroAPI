package com.periut.retrotweaks.mixin.client.render;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.UndeadEntityRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same fix as {@link BowHeldFixPlayerEntityRendererMixin}, for skeletons - the only vanilla mob
 * that holds a bow. {@code renderMore} is declared directly on {@code UndeadEntityRenderer}
 * (overriding the generic {@code LivingEntityRenderer<T>} one), so this mixin targets that class
 * rather than the generic parent. From UniTweaks (mixin/bugfixes/bowheldfix/SkeletonEntityRendererMixin).
 */
@Mixin(UndeadEntityRenderer.class)
public abstract class BowHeldFixSkeletonEntityRendererMixin extends EntityRenderer {

	@Inject(method = "renderMore", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V",
		shift = At.Shift.BEFORE))
	protected void retrotweaks$offsetBowRendering(LivingEntity entity, float f, CallbackInfo ci) {
		if (Config.BUGFIXES.bowHeldFix) {
			ItemStack itemStack = entity.getHeldItem();
			if (itemStack != null && itemStack.getItem() instanceof BowItem) {
				GL11.glRotatef(-5, 1, 0, 0);
				GL11.glTranslatef(0.2F, -0.5F, 0.2F);
			}
		}
	}
}
