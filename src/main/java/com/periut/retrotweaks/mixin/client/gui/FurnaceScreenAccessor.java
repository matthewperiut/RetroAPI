package com.periut.retrotweaks.mixin.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reaches the furnace behind a furnace screen, so shift-click knows which slots to aim at. */
@Environment(EnvType.CLIENT)
@Mixin(FurnaceScreen.class)
public interface FurnaceScreenAccessor {

	@Accessor("furnaceBlockEntity")
	FurnaceBlockEntity retrotweaks$getFurnaceBlockEntity();
}
