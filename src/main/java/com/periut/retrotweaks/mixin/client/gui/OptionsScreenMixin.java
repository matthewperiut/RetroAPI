package com.periut.retrotweaks.mixin.client.gui;

import com.periut.retrotweaks.client.gui.ConfigScreen;
import com.periut.retrotweaks.client.gui.ExtraButton;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Adds the "Configs..." button to the vanilla options screen.
 *
 * <p>Named for what it opens rather than for one mod: the page behind it carries every config any mod
 * has registered through {@code RetroConfigs}, reached from the "Mods" button at the top of it.
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {

	@Inject(method = "init", at = @At("TAIL"))
	private void retrotweaks$addConfigButton(CallbackInfo ci) {
		// Vanilla's five options fill two columns and leave the right half of the third row empty
		// (Difficulty sits alone on the left). Dropping the button there means nothing vanilla has
		// to move, so the screen still looks like the screen people know.
		List<ButtonWidget> buttons = this.buttons;
		buttons.add(new ExtraButton(
			this.width / 2 - 155 + 160,
			this.height / 6 + 48,
			150, 20,
			"Configs...",
			button -> this.minecraft.setScreen(new ConfigScreen((OptionsScreen) (Object) this))
		));
	}
}
