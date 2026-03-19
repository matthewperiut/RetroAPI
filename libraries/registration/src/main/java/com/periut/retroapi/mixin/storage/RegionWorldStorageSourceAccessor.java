package com.periut.retroapi.mixin.storage;

import net.minecraft.world.storage.AlphaWorldStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(AlphaWorldStorageSource.class)
public interface RegionWorldStorageSourceAccessor {
	@Accessor("dir")
	File retroapi$getDir();
}
