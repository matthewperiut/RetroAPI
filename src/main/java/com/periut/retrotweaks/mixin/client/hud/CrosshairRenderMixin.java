package com.periut.retrotweaks.mixin.client.hud;

import com.periut.retrotweaks.compat.Mods;
import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The crosshair draw ({@code drawTexture} ordinal 2 in {@code InGameHud.render}), split out of
 * {@link InGameHudMixin} because it must sit at a DIFFERENT mixin priority than the rest of the HUD
 * work to survive UniTweaks.
 *
 * <p><b>The bug this exists for:</b> UniTweaks 0.29.0's
 * {@code tweaks.frontviewthirdperson.InGameHudMixin} wraps this same draw, and its handler has a
 * fall-through: when its {@code frontViewThirdPerson} mode is {@code DISABLED} it calls the draw once
 * for the DISABLED branch and then AGAIN because front view is not active - two draws. The crosshair
 * is drawn with inverting blend ({@code GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR}), so the
 * second draw inverts the first back out: net effect, no crosshair. UniTweaks alone defaults the mode
 * to {@code NORMAL} (single draw), which is why only its own users who pick DISABLED ever saw it - but
 * RetroTweaks defaults front view OFF and {@code UniTweaksBridge} pushes that {@code DISABLED} into
 * UniTweaks' live config, so the combination hit the bug out of the box: crosshair missing whenever
 * both mods are installed.
 *
 * <p><b>The fix:</b> priority 1100 makes this wrap apply after UniTweaks' (1000), i.e. OUTERMOST in
 * MixinExtras' chain. When UniTweaks is installed and the front-view mode RetroTweaks pushed is
 * {@code DISABLED} - exactly the case whose UniTweaks branch is broken, and a case where UniTweaks'
 * feature semantics are simply "draw the crosshair normally" - this calls the real
 * {@code drawTexture} directly instead of {@code original}, skipping UniTweaks' handler and drawing
 * exactly once. Every other case (front view enabled, or no UniTweaks) defers to the chain, so
 * UniTweaks' hide-crosshair-in-front-view behaviour keeps working. If UniTweaks fixes the
 * double-draw upstream, the bypass still draws exactly once and nothing regresses.
 */
@Environment(EnvType.CLIENT)
@Mixin(value = InGameHud.class, priority = 1100)
public abstract class CrosshairRenderMixin {

	@WrapOperation(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
					ordinal = 2
			), require = 0)
	private void retrotweaks$renderCrosshair(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
		if (Config.HUD.disableCrosshair) {
			original.call(instance, 0, 0, 0, 0, 0, 0);
			return;
		}
		if (Mods.HAS_UNITWEAKS && Config.INTERFACE.frontViewThirdPerson == Enums.FrontView.DISABLED) {
			instance.drawTexture(x, y, u, v, width, height);
			return;
		}
		original.call(instance, x, y, u, v, width, height);
	}
}
