package com.periut.retrotweaks.mixin.item;

import com.periut.retrotweaks.config.Config;

import net.minecraft.item.BowItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla renders bows as a non-handheld item, which holds them at the wrong angle in the
 * player's hand. Forcing {@code isHandheld()} true fixes the pose; the render-position offset
 * that compensates for the resulting shift lives in {@link
 * com.periut.retrotweaks.mixin.client.render.BowHeldFixPlayerEntityRendererMixin} and
 * {@link com.periut.retrotweaks.mixin.client.render.BowHeldFixSkeletonEntityRendererMixin}.
 * From UniTweaks (mixin/bugfixes/bowheldfix/BowItemMixin).
 */
@Mixin(BowItem.class)
public class BowHeldFixBowItemMixin extends BowHeldFixItemMixin {

	@Override
	protected void retrotweaks$bowHeldFixIsHandheld(CallbackInfoReturnable<Boolean> cir) {
		if (Config.BUGFIXES.bowHeldFix) {
			cir.setReturnValue(true);
		}
	}
}
