package com.periut.retrotweaks.mixin.item;

import net.minecraft.item.Item;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hook point for {@link BowHeldFixBowItemMixin} to override {@code isHandheld()} on a
 * per-subclass basis. Does nothing on its own - {@code BowItem} overrides the injected callback.
 * From UniTweaks (mixin/bugfixes/bowheldfix/ItemMixin).
 */
@Mixin(Item.class)
public class BowHeldFixItemMixin {

	@SuppressWarnings("CancellableInjectionUsage")
	@Inject(method = "isHandheld", at = @At(value = "HEAD"), cancellable = true)
	protected void retrotweaks$bowHeldFixIsHandheld(CallbackInfoReturnable<Boolean> cir) {

	}
}
