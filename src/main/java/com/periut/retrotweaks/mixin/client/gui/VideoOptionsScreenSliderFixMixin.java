package com.periut.retrotweaks.mixin.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;

import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes vanilla's {@code VideoOptionsScreen.buttonClicked} rebuilding every widget on the screen
 * after every click, including the initial click that starts a slider drag. From UniTweaks
 * (bugfixes.videosettingssliderfix).
 *
 * <p>{@code Screen.mouseClicked} calls {@code isMouseOver} on the clicked widget (which applies the
 * click position and sets {@code SliderWidget.dragging = true}), then unconditionally calls
 * {@code buttonClicked}, which vanilla ends with a full {@code init(...)} - replacing every widget,
 * including the one just marked as dragging, with a fresh instance whose {@code dragging} is false.
 * {@code Screen.selectedButton} still points at the discarded instance, so the mouse-release event
 * lands on a widget nobody renders any more. Net effect: the slider jumps to wherever the click
 * landed and then never tracks the drag - exactly the "label moves but the interface doesn't resize"
 * report for the GUI scale slider (finding #50), and the same reason none of vanilla's other
 * video-options sliders (render distance, FPS limit, brightness, fog density, cloud height) drag
 * either.
 *
 * <p>Cancelling the {@code init(...)} call while the mouse is still down leaves the just-clicked
 * widget in place for the rest of the drag; {@code SliderWidget.renderBackground} keeps re-applying
 * the value every frame regardless, and the screen still gets laid out fresh the next time
 * {@code buttonClicked} runs on release.
 *
 * <p>Vanilla's own GUI Scale button (id 12, the four-step cycle) is excluded: it is a single
 * discrete click, not a drag, and skipping its relayout would leave the other buttons stale if that
 * click changed the scale.
 */
@Environment(EnvType.CLIENT)
@Mixin(VideoOptionsScreen.class)
public class VideoOptionsScreenSliderFixMixin {

	@Inject(method = "buttonClicked",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/option/VideoOptionsScreen;init(Lnet/minecraft/client/Minecraft;II)V",
			shift = At.Shift.BEFORE),
		cancellable = true, require = 1)
	private void retrotweaks$cancelInit(ButtonWidget button, CallbackInfo ci) {
		if (button.id != 12 && Mouse.isButtonDown(0)) {
			ci.cancel();
		}
	}
}
