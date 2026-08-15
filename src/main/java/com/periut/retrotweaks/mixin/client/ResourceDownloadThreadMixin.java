package com.periut.retrotweaks.mixin.client;

import com.periut.retrotweaks.config.Config;

import net.minecraft.client.resource.ResourceDownloadThread;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Points sound and music downloads at a mirror. From MojangFix and UniTweaks.
 *
 * <p>The URL vanilla ships has been dead for years, so without this the game hangs on startup
 * waiting for a host that will never answer, and no sounds ever arrive.
 *
 * <p>High priority so this wins over other mods redirecting the same constant, and
 * {@code require = 0} so it is not an error when one of them got there first.
 */
@Mixin(value = ResourceDownloadThread.class, priority = 1500)
public class ResourceDownloadThreadMixin {

	@ModifyConstant(method = "run", constant = @Constant(stringValue = "http://s3.amazonaws.com/MinecraftResources/"), remap = false, require = 0)
	private String retrotweaks$resourceUrl(String vanilla) {
		if (!Config.MULTIPLAYER.useResourcesDownloadUrl) return vanilla;
		String url = Config.MULTIPLAYER.useAlternateResourcesUrl
			? Config.MULTIPLAYER.alternateResourcesDownloadUrl
			: Config.MULTIPLAYER.resourcesDownloadUrl;
		return url == null || url.isEmpty() ? vanilla : url;
	}
}
