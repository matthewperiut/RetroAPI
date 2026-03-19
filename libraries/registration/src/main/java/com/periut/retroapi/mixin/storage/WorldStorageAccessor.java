package com.periut.retroapi.mixin.storage;

import net.minecraft.world.storage.AlphaWorldStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(AlphaWorldStorage.class)
public interface WorldStorageAccessor {
	@Accessor("dir")
	File retroapi$getDir();
}
