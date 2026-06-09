package com.periut.retroapi.testmod.conv;

import com.mojang.serialization.Dynamic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.storage.WorldStorageSource;
import net.modificationstation.stationapi.api.StationAPI;
import net.modificationstation.stationapi.api.datafixer.DataFixers;
import net.modificationstation.stationapi.api.nbt.NbtHelper;
import net.modificationstation.stationapi.api.nbt.NbtOps;
import net.modificationstation.stationapi.impl.vanillafix.datafixer.VanillaDataFixerImpl;
import net.modificationstation.stationapi.impl.world.storage.FlattenedWorldStorage;

/**
 * Drives StationAPI's world conversion directly from the client storage source - the same API the
 * world-select and Edit-World screens use, just without the GUI. Loaded only when StationAPI is
 * present (every reference here is a StationAPI / DataFixerUpper class).
 *
 * <ul>
 *   <li><b>forward</b> - vanilla {@code WorldStorageSource.convert}; with StationAPI installed the
 *       storage source is {@link FlattenedWorldStorage}, so this flattens the world (and RetroAPI's
 *       {@code FlattenedWorldStorageMixin} injects the sidecar content).</li>
 *   <li><b>reverse</b> - replicates StationAPI's "Convert to McRegion" button: runs
 *       {@code FlattenedWorldStorage.convertWorld} with {@code VanillaDataFixerImpl.DATA_DAMAGER}, which
 *       un-flattens chunks/items back to vanilla numeric form (RetroAPI's mixin reconstructs the
 *       sidecars around it).</li>
 * </ul>
 */
public final class StationApiConvert {
	private StationApiConvert() {}

	/** A LoadingDisplay that ignores progress callbacks - the conversion runs headless. */
	private static final LoadingDisplay NOOP = new LoadingDisplay() {
		@Override public void progressStartNoAbort(String message) {}
		@Override public void progressStage(String stage) {}
		@Override public void progressStagePercentage(int percentage) {}
	};

	public static boolean needsConversion(Minecraft mc, String folder) {
		return mc.getWorldStorageSource().needsConversion(folder);
	}

	/** Forward: McRegion + RetroAPI sidecar -> StationAPI flattened. */
	public static void forward(Minecraft mc, String folder) {
		WorldStorageSource source = mc.getWorldStorageSource();
		source.convert(folder, NOOP);
	}

	/** Reverse: StationAPI flattened -> McRegion + RetroAPI sidecar (the Edit-World damager path). */
	public static void reverse(Minecraft mc, String folder) throws Exception {
		FlattenedWorldStorage storage = (FlattenedWorldStorage) mc.getWorldStorageSource();
		storage.convertWorld(folder, (type, compound) ->
			(NbtCompound) VanillaDataFixerImpl.DATA_DAMAGER.get().update(
				type,
				new Dynamic<>(NbtOps.INSTANCE, compound).remove(DataFixers.DATA_VERSIONS),
				VanillaDataFixerImpl.HIGHEST_VERSION
					- NbtHelper.getDataVersions(compound).getInt(StationAPI.NAMESPACE.toString()),
				VanillaDataFixerImpl.VANILLA_VERSION)
			.getValue(),
			NOOP);
	}
}
