package com.periut.retroapi.mixin.gamemode.client;

import com.periut.retroapi.client.screen.RetroCreativeScreen;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A creative player's inventory key opens the creative screen instead of the survival one.
 *
 * <p>Swapped on its way into {@code setScreen}, the same trick the chat screen uses, so every route
 * into the inventory - the key, a mod, a block that opens it - ends up in the right place.
 */
@Mixin(Minecraft.class)
public class CreativeInventoryScreenMixin {

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen retroapi$creativeInventory(Screen screen) {
		if (screen == null || screen.getClass() != InventoryScreen.class) {
			return screen;
		}
		Minecraft minecraft = (Minecraft) (Object) this;
		if (minecraft.player == null
			|| RetroGameModes.get(minecraft.player.name) != RetroGameMode.CREATIVE) {
			return screen;
		}
		return new RetroCreativeScreen(minecraft.player);
	}
}
