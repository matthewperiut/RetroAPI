package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.client.texture.AtlasExpander;
import net.minecraft.client.render.texture.TextureManager;
import net.minecraft.client.resource.pack.TexturePack;
import net.minecraft.client.resource.pack.TexturePacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

@Mixin(TextureManager.class)
public abstract class TextureManagerMixin {
	@Shadow
	private TexturePacks texturePacks;

	@Shadow
	public abstract void load(BufferedImage image, int id);

	@Unique
	private int retroapi$terrainTextureId = -1;
	@Unique
	private int retroapi$itemsTextureId = -1;

	@Inject(method = "load(Ljava/lang/String;)I", at = @At("RETURN"))
	private void retroapi$onLoadTexture(String path, CallbackInfoReturnable<Integer> cir) {
		int textureId = cir.getReturnValueI();

		if ("/terrain.png".equals(path) && retroapi$terrainTextureId == -1) {
			retroapi$terrainTextureId = textureId;
			retroapi$compositeAtlas("/terrain.png", textureId, true);
		} else if ("/gui/items.png".equals(path) && retroapi$itemsTextureId == -1) {
			retroapi$itemsTextureId = textureId;
			retroapi$compositeAtlas("/gui/items.png", textureId, false);
		}
	}

	@Inject(method = "reload", at = @At("RETURN"))
	private void retroapi$onReload(CallbackInfo ci) {
		if (retroapi$terrainTextureId != -1) {
			retroapi$compositeAtlas("/terrain.png", retroapi$terrainTextureId, true);
		}
		if (retroapi$itemsTextureId != -1) {
			retroapi$compositeAtlas("/gui/items.png", retroapi$itemsTextureId, false);
		}
	}

	@Unique
	private void retroapi$compositeAtlas(String path, int textureId, boolean isTerrain) {
		try {
			TexturePack pack = texturePacks.selected;
			InputStream is = pack.getResource(path);
			if (is != null) {
				BufferedImage original = ImageIO.read(is);
				is.close();
				BufferedImage modified = isTerrain
					? AtlasExpander.expandTerrainAtlas(original)
					: AtlasExpander.expandItemAtlas(original);
				load(modified, textureId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
