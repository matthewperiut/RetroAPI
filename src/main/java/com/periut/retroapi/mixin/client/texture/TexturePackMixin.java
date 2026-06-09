package com.periut.retroapi.mixin.client.texture;

import net.minecraft.client.resource.pack.TexturePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

/**
 * Resolves namespaced texture paths ({@code "mymod:textures/mobs/x.png"}) to the classpath
 * resource {@code /assets/mymod/textures/mobs/x.png}, mirroring StationAPI's resource-loader
 * behavior so mods can keep namespaced strings in {@code entity.texture} fields,
 * {@code TextureManager.getTextureId(...)} calls, and armor/accessory texture paths.
 * Vanilla paths (starting with '/', no namespace) fall through untouched.
 */
@Mixin(TexturePack.class)
public class TexturePackMixin {

	@Inject(method = "getResource", at = @At("HEAD"), cancellable = true)
	private void retroapi$resolveNamespacedPath(String path, CallbackInfoReturnable<InputStream> cir) {
		if (path == null || path.isEmpty() || path.charAt(0) == '/') {
			return;
		}
		int colon = path.indexOf(':');
		if (colon <= 0) {
			return;
		}
		String namespace = path.substring(0, colon);
		String rest = path.substring(colon + 1);
		InputStream stream = TexturePackMixin.class.getResourceAsStream("/assets/" + namespace + "/" + rest);
		if (stream != null) {
			cir.setReturnValue(stream);
		}
	}
}
