package com.periut.retroapi.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("gameDir")
	File retroapi$getGameDir();
}
