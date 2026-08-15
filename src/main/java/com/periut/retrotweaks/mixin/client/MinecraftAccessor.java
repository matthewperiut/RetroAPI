package com.periut.retrotweaks.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * b1.7.3 keeps the running {@code Minecraft} in a private static field with no getter, so anything
 * outside the client's own call chain - a mixin on a shared class, say - has no way to reach it.
 */
@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public interface MinecraftAccessor {

	@Accessor("INSTANCE")
	static Minecraft retrotweaks$getInstance() {
		throw new AssertionError("Replaced by Mixin at runtime");
	}
}
