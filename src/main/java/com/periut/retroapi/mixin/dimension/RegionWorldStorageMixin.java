package com.periut.retroapi.mixin.dimension;

import com.periut.retroapi.dimension.RetroDimensionRegistry;
import com.periut.retroapi.mixin.storage.WorldStorageAccessor;
import net.minecraft.world.chunk.storage.ChunkStorage;
import net.minecraft.world.chunk.storage.RegionChunkStorage;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.storage.RegionWorldStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

/**
 * Gives a modded dimension its own {@code DIM<serialId>/} save folder. Vanilla
 * {@code getChunkStorage} hardcodes the folder by {@code instanceof NetherDimension} (-> {@code DIM-1})
 * else the overworld root, so a modded dimension would clobber the overworld's region files. We
 * intercept for registered modded ids and return a {@code RegionChunkStorage} rooted at
 * {@code DIM<id>/}, mirroring vanilla's {@code DIM-1} scheme - so the modded save is inherently
 * additive (vanilla simply ignores unknown {@code DIM<n>/} folders). The base world dir comes from
 * the existing {@link WorldStorageAccessor} ({@code dir} is a private field on the parent
 * {@code AlphaWorldStorage}, so it cannot be shadowed directly here). Disabled under StationAPI.
 */
@Mixin(RegionWorldStorage.class)
public abstract class RegionWorldStorageMixin {
	@Inject(method = "getChunkStorage", at = @At("HEAD"), cancellable = true)
	private void retroapi$moddedDimensionFolder(Dimension dimension, CallbackInfoReturnable<ChunkStorage> cir) {
		if (dimension == null || RetroDimensionRegistry.isVanillaId(dimension.id)) return;
		if (RetroDimensionRegistry.getBySerialId(dimension.id) == null) return;
		File baseDir = ((WorldStorageAccessor) (Object) this).retroapi$getDir();
		File dir = new File(baseDir, "DIM" + dimension.id);
		dir.mkdirs();
		cir.setReturnValue(new RegionChunkStorage(dir));
	}
}
