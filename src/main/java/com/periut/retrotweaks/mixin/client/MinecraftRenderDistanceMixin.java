package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.feature.options.ModOptions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Option;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Redirects the vanilla render-distance keybinding ("F", {@code GameOptions.fogKey}) from vanilla's
 * four fixed tiers to RetroTweaks' own slider stops while the render distance option is enabled. From
 * UniTweaks' {@code mixin.tweaks.renderdistance.MinecraftMixin}.
 *
 * <p>{@code Minecraft.tick()} handles that key with the only call to {@code GameOptions.setInt} inside
 * it: {@code this.options.setInt(Option.RENDER_DISTANCE, ±1)}, which only ever cycles the four values
 * {@code WorldRenderer.reload()} understands on its own ({@code GameOptions.viewDistance}, 0..3) - see
 * {@code WorldRendererMixin} for how those stop mattering once this option is on. Wrapping that call
 * site steps {@link ModOptions#renderDistance} through its own stops instead
 * ({@link ModOptions#cycleRenderDistance()}), and calls {@code save()} itself: skipping vanilla's
 * {@code setInt} also skips the {@code save()} call at the end of it, which is what would otherwise
 * have persisted the change (through {@code GameOptionsMixin}'s own hook on that same method).
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftRenderDistanceMixin {

	@WrapOperation(method = "tick", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/option/GameOptions;setInt(Lnet/minecraft/client/option/Option;I)V"), require = 1)
	private void retrotweaks$cycleRenderDistance(GameOptions instance, Option option, int value, Operation<Void> original) {
		if (option == Option.RENDER_DISTANCE && ModOptions.enabled(ModOptions.renderDistanceOption)) {
			ModOptions.cycleRenderDistance();
			instance.save();
			return;
		}
		original.call(instance, option, value);
	}
}
