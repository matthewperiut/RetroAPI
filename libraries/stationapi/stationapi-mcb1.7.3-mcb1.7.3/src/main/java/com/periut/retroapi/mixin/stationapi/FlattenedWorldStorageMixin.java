package com.periut.retroapi.mixin.stationapi;

import com.periut.retroapi.compat.WorldConversionHelper;
import com.periut.retroapi.compat.WorldConversionProcessor;
import com.periut.retroapi.mixin.storage.RegionWorldStorageSourceAccessor;
import com.periut.retroapi.storage.BackupManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ProgressListener;
import net.modificationstation.stationapi.impl.world.storage.FlattenedWorldStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileInputStream;
import java.util.function.BiFunction;

@Mixin(FlattenedWorldStorage.class)
public abstract class FlattenedWorldStorageMixin {

	@Unique
	private boolean retroapi$isUnConversion = false;

	@Inject(method = "convertWorld", at = @At("HEAD"), remap = false)
	private void retroapi$beforeConvertWorld(String worldFolder, BiFunction<?, ?, ?> convertFunction, ProgressListener progress, CallbackInfoReturnable<Boolean> cir) {
		File savesDir = ((RegionWorldStorageSourceAccessor) this).retroapi$getDir();
		File worldDir = new File(savesDir, worldFolder);

		retroapi$isUnConversion = retroapi$isStationAPIWorld(worldDir);

		BackupManager.backupRetroApiData(worldDir);
		WorldConversionHelper.injectRetroApiMappings(worldDir);

		if (retroapi$isUnConversion) {
			// Scan flattened sections for RetroAPI blocks and create block sidecars
			// BEFORE the damager converts sections to byte[]
			WorldConversionProcessor.reconstructBlockSidecarsFromFlattened(worldDir);
		}
	}

	@Inject(method = "convertWorld", at = @At("RETURN"), remap = false)
	private void retroapi$afterConvertWorld(String worldFolder, BiFunction<?, ?, ?> convertFunction, ProgressListener progress, CallbackInfoReturnable<Boolean> cir) {
		File savesDir = ((RegionWorldStorageSourceAccessor) this).retroapi$getDir();
		File worldDir = new File(savesDir, worldFolder);

		if (retroapi$isUnConversion) {
			// After damager: strip modded content from vanilla chunks into inventory sidecars
			WorldConversionProcessor.reconstructInventorySidecarsFromVanilla(worldDir);
			WorldConversionProcessor.fixLevelDatForRetroApi(worldDir);
		} else {
			// Forward conversion: inject sidecars into StationAPI format
			WorldConversionProcessor.processSidecars(worldDir);
			WorldConversionProcessor.fixLevelDat(worldDir);
		}
	}

	@Unique
	private static boolean retroapi$isStationAPIWorld(File worldDir) {
		File levelDat = new File(worldDir, "level.dat");
		if (!levelDat.exists()) return false;
		try {
			NbtCompound root = NbtIo.readCompressed(new FileInputStream(levelDat));
			NbtCompound data = root.getCompound("Data");
			if (data == null) return false;
			// StationAPI stores data versions under "stationapi:data_versions" (Identifier format)
			if (!data.contains("stationapi:data_versions")) return false;
			NbtCompound versions = data.getCompound("stationapi:data_versions");
			return versions.contains("stationapi");
		} catch (Exception e) {
			return false;
		}
	}
}
