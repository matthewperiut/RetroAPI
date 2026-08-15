package com.periut.retrotweaks.mixin.client.hud;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shows the world seed on the debug screen. From MojangFix.
 *
 * <p>b1.7.3 has no way to see the seed of a world you are in, short of reading level.dat.
 */
@Mixin(InGameHud.class)
public abstract class WorldSeedMixin extends DrawContext {

	@Shadow private Minecraft minecraft;

	@Inject(method = "render",
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glPopMatrix()V", ordinal = 0, remap = false),
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;debugHud:Z")),
		require = 0)
	private void retrotweaks$drawSeed(CallbackInfo ci) {
		if (!Config.HUD.debug.showWorldSeed || this.minecraft.world == null) return;
		this.drawTextWithShadow(this.minecraft.textRenderer,
			"Seed: " + this.minecraft.world.getSeed(), 2, 104, 0xE0E0E0);
	}
}
