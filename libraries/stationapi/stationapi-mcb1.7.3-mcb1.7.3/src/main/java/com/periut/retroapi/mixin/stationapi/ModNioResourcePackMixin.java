package com.periut.retroapi.mixin.stationapi;

import net.modificationstation.stationapi.api.resource.InputSupplier;
import net.modificationstation.stationapi.api.resource.ResourcePack;
import net.modificationstation.stationapi.api.resource.ResourceType;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.util.Namespace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

/**
 * Remaps StationAPI resource lookups from stationapi/textures/ to textures/
 * so that textures placed at assets/{namespace}/textures/ are found by
 * StationAPI's atlas system without requiring duplicate files.
 */
@Mixin(targets = "net.modificationstation.stationapi.impl.resource.ModNioResourcePack")
public abstract class ModNioResourcePackMixin {

	private static final String STATIONAPI_PREFIX = "stationapi/textures/";
	private static final String RETROAPI_PREFIX = "textures/";

	@Unique
	private boolean retroapi$redirectingOpen = false;
	@Unique
	private boolean retroapi$redirectingFind = false;

	/**
	 * When StationAPI looks for a texture at stationapi/textures/ and doesn't find it,
	 * try looking at textures/ instead.
	 */
	@Inject(method = "open", at = @At("RETURN"), cancellable = true)
	private void retroapi$openRetroTexture(ResourceType type, Identifier id,
										   CallbackInfoReturnable<InputSupplier<InputStream>> cir) {
		if (retroapi$redirectingOpen) return;
		if (cir.getReturnValue() != null) return;
		if (!id.path.startsWith(STATIONAPI_PREFIX)) return;

		String retroPath = RETROAPI_PREFIX + id.path.substring(STATIONAPI_PREFIX.length());
		Identifier retroId = Identifier.of(id.namespace, retroPath);

		retroapi$redirectingOpen = true;
		try {
			InputSupplier<InputStream> result = ((ResourcePack) (Object) this).open(type, retroId);
			if (result != null) {
				cir.setReturnValue(result);
			}
		} finally {
			retroapi$redirectingOpen = false;
		}
	}

	/**
	 * When StationAPI scans stationapi/textures/ directories,
	 * also scan textures/ and present results as stationapi/textures/.
	 */
	@Inject(method = "findResources", at = @At("TAIL"))
	private void retroapi$findRetroTextures(ResourceType type, Namespace namespace, String path,
											ResourcePack.ResultConsumer visitor, CallbackInfo ci) {
		if (retroapi$redirectingFind) return;
		if (!path.startsWith(STATIONAPI_PREFIX)) return;

		String retroPath = RETROAPI_PREFIX + path.substring(STATIONAPI_PREFIX.length());

		retroapi$redirectingFind = true;
		try {
			((ResourcePack) (Object) this).findResources(type, namespace, retroPath, (id, supplier) -> {
				if (id.path.startsWith(RETROAPI_PREFIX)) {
					String remappedPath = STATIONAPI_PREFIX + id.path.substring(RETROAPI_PREFIX.length());
					Identifier remapped = Identifier.of(id.namespace, remappedPath);
					visitor.accept(remapped, supplier);
				}
			});
		} finally {
			retroapi$redirectingFind = false;
		}
	}
}
