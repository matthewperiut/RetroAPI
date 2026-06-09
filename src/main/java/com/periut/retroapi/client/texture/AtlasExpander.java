package com.periut.retroapi.client.texture;

import com.periut.retroapi.register.block.RetroTexture;
import com.periut.retroapi.register.block.RetroTextures;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AtlasExpander {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/AtlasExpander");
	private static final int ATLAS_COLUMNS = 16;

	/** Current terrain atlas size (width = height, always square, power of 2). */
	public static int terrainAtlasSize = 256;
	/** Current item atlas size (width = height, always square, power of 2). */
	public static int itemAtlasSize = 256;

	/** Sprite size of the composited terrain/item atlas (16 for standard packs, larger for HD). */
	public static int terrainSpriteSize = 16;
	public static int itemSpriteSize = 16;

	/** Live animators, rebuilt on every composite (initial load, reload, pack switch). */
	public static final List<RetroAnimatedTexture> terrainAnimators = new ArrayList<>();
	public static final List<RetroAnimatedTexture> itemAnimators = new ArrayList<>();

	private static int nextPowerOfTwo(int value) {
		if (value <= 256) return 256;
		return Integer.highestOneBit(value - 1) << 1;
	}

	public static BufferedImage expandTerrainAtlas(BufferedImage original) {
		return expandAtlas(original, RetroTextures.getTerrainTextures(), "block", true);
	}

	public static BufferedImage expandItemAtlas(BufferedImage original) {
		return expandAtlas(original, RetroTextures.getItemTextures(), "item", false);
	}

	private static BufferedImage expandAtlas(BufferedImage original, List<RetroTexture> textures,
			String folder, boolean isTerrain) {
		List<RetroAnimatedTexture> animators = isTerrain ? terrainAnimators : itemAnimators;
		animators.clear();
		if (textures.isEmpty()) {
			return original;
		}

		int spriteSize = original.getWidth() / ATLAS_COLUMNS;
		int originalRows = original.getHeight() / spriteSize;

		int maxSlot = textures.stream().mapToInt(t -> t.id).max().orElse(0);
		int neededRows = Math.max(originalRows, (maxSlot / ATLAS_COLUMNS) + 1);

		int neededWidth = ATLAS_COLUMNS * spriteSize;
		int neededHeight = neededRows * spriteSize;
		int atlasSize = nextPowerOfTwo(Math.max(neededWidth, neededHeight));
		if (isTerrain) {
			terrainAtlasSize = atlasSize;
			terrainSpriteSize = spriteSize;
		} else {
			itemAtlasSize = atlasSize;
			itemSpriteSize = spriteSize;
		}

		LOGGER.info("Expanding {} atlas to {}x{} (vanilla: {}x{})",
			isTerrain ? "terrain" : "item", atlasSize, atlasSize, original.getWidth(), original.getHeight());

		BufferedImage atlas = new BufferedImage(atlasSize, atlasSize, BufferedImage.TYPE_INT_ARGB);
		atlas.getGraphics().drawImage(original, 0, 0, null);

		for (RetroTexture tex : textures) {
			// Derived item slot whose base is a vanilla sprite (e.g. apple + overlay): copy
			// the vanilla base in from this very atlas, then stack the overlays. There is no
			// PNG to load for these, so handle them before the file path below.
			if (!isTerrain) {
				Integer vanillaBase = com.periut.retroapi.client.model.ItemModelLoader.vanillaBaseSlot(tex);
				if (vanillaBase != null) {
					int col = tex.id % ATLAS_COLUMNS;
					int row = tex.id / ATLAS_COLUMNS;
					blitAtlasSlot(atlas, vanillaBase, col, row, spriteSize);
					applyItemOverlays(atlas, tex, col, row, spriteSize);
					continue;
				}
			}

			String texturePath = folder + "/" + tex.getIdentifier().identifier();
			BufferedImage texture = loadTexture(tex.getIdentifier().namespace(), texturePath);
			if (texture == null) {
				continue;
			}

			AnimationMetadata animation = resolveAnimation(tex, texturePath, texture);
			int col = tex.id % ATLAS_COLUMNS;
			int row = tex.id / ATLAS_COLUMNS;

			if (animation != null && texture.getHeight() >= texture.getWidth() * 2) {
				// Vertical strip of square frames: composite frame 0, queue the animator.
				int frameCount = texture.getHeight() / texture.getWidth();
				byte[][] frames = new byte[frameCount][];
				for (int f = 0; f < frameCount; f++) {
					BufferedImage frame = texture.getSubimage(0, f * texture.getWidth(),
						texture.getWidth(), texture.getWidth());
					frames[f] = toRgba(scale(frame, spriteSize));
				}
				atlas.getGraphics().drawImage(scale(texture.getSubimage(0, 0, texture.getWidth(), texture.getWidth()), spriteSize),
					col * spriteSize, row * spriteSize, null);
				animators.add(new RetroAnimatedTexture(tex.id, spriteSize, frames, animation));
				LOGGER.debug("Animated {} texture {} at slot {} ({} frames)",
					folder, tex.getIdentifier(), tex.id, frameCount);
			} else {
				if (animation != null) {
					LOGGER.warn("Texture {} has animation metadata but is not a vertical frame strip; rendering statically",
						tex.getIdentifier());
				}
				atlas.getGraphics().drawImage(texture, col * spriteSize, row * spriteSize, spriteSize, spriteSize, null);
				LOGGER.debug("Composited {} texture {} at slot {}", folder, tex.getIdentifier(), tex.id);
			}

			// Item layer models (layer1+): flatten the extra layers onto the same slot.
			if (!isTerrain) {
				applyItemOverlays(atlas, tex, col, row, spriteSize);
			}
		}

		return atlas;
	}

	/** Copies one square atlas slot into another, used to seed a vanilla base into a derived slot. */
	private static void blitAtlasSlot(BufferedImage atlas, int srcSlot, int destCol, int destRow, int spriteSize) {
		int sx = (srcSlot % ATLAS_COLUMNS) * spriteSize;
		int sy = (srcSlot / ATLAS_COLUMNS) * spriteSize;
		int dx = destCol * spriteSize, dy = destRow * spriteSize;
		atlas.getGraphics().drawImage(atlas,
			dx, dy, dx + spriteSize, dy + spriteSize,
			sx, sy, sx + spriteSize, sy + spriteSize, null);
	}

	/** Composites an item's overlay layers (layer1+) onto its slot: vanilla layers blit from the atlas, modded layers load their PNG. */
	private static void applyItemOverlays(BufferedImage atlas, RetroTexture tex, int col, int row, int spriteSize) {
		java.util.List<net.ornithemc.osl.core.api.util.NamespacedIdentifier> overlays =
			com.periut.retroapi.client.model.ItemModelLoader.overlaysFor(tex);
		if (overlays == null) {
			return;
		}
		for (net.ornithemc.osl.core.api.util.NamespacedIdentifier overlayId : overlays) {
			// Vanilla overlay layers ("minecraft:item/apple") blit straight from the
			// vanilla region of this very atlas (slot positions are unchanged).
			Integer vanillaSlot = "minecraft".equals(overlayId.namespace())
				? com.periut.retroapi.client.model.ItemModelLoader.vanillaSprite(overlayId.identifier())
				: null;
			if (vanillaSlot != null) {
				blitAtlasSlot(atlas, vanillaSlot, col, row, spriteSize);
				continue;
			}
			BufferedImage overlay = loadTexture(overlayId.namespace(), "item/" + overlayId.identifier());
			if (overlay != null) {
				atlas.getGraphics().drawImage(overlay, col * spriteSize, row * spriteSize, spriteSize, spriteSize, null);
			}
		}
	}

	/** Animation source priority: code-driven registration, then a .png.mcmeta sidecar, then bare vertical strips. */
	private static AnimationMetadata resolveAnimation(RetroTexture tex, String texturePath, BufferedImage texture) {
		AnimationMetadata code = RetroTextures.getCodeAnimation(tex);
		if (code != null) {
			return code;
		}
		AnimationMetadata mcmeta = loadMcmeta(tex.getIdentifier().namespace(), texturePath);
		if (mcmeta != null) {
			return mcmeta;
		}
		// A square-frame vertical strip with no metadata still animates (all frames, 1 tick each),
		// so a texture is never silently squashed into one slot.
		if (texture.getHeight() >= texture.getWidth() * 2 && texture.getHeight() % texture.getWidth() == 0) {
			return AnimationMetadata.uniform(1);
		}
		return null;
	}

	private static BufferedImage scale(BufferedImage image, int size) {
		if (image.getWidth() == size && image.getHeight() == size) {
			return image;
		}
		BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaled.createGraphics();
		g.drawImage(image, 0, 0, size, size, null);
		g.dispose();
		return scaled;
	}

	private static byte[] toRgba(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
		byte[] rgba = new byte[w * h * 4];
		for (int i = 0; i < argb.length; i++) {
			int pixel = argb[i];
			rgba[i * 4] = (byte) ((pixel >> 16) & 0xFF);
			rgba[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);
			rgba[i * 4 + 2] = (byte) (pixel & 0xFF);
			rgba[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF);
		}
		return rgba;
	}

	private static AnimationMetadata loadMcmeta(String namespace, String texturePath) {
		String nativePath = "/assets/" + namespace + "/textures/" + texturePath + ".png.mcmeta";
		String stationPath = "/assets/" + namespace + "/stationapi/textures/" + texturePath + ".png.mcmeta";
		for (String resourcePath : new String[]{nativePath, stationPath}) {
			try (InputStream is = AtlasExpander.class.getResourceAsStream(resourcePath)) {
				if (is != null) {
					return AnimationMetadata.parse(is);
				}
			} catch (IOException e) {
				LOGGER.error("Failed to read mcmeta: {}", resourcePath, e);
				return null;
			}
		}
		return null;
	}

	private static BufferedImage loadTexture(String namespace, String texturePath) {
		// RetroAPI-native path first, then the StationAPI-compatible layout so assets
		// laid out for StationAPI migrate with zero file moves (same policy as LangLoader).
		String nativePath = "/assets/" + namespace + "/textures/" + texturePath + ".png";
		String stationPath = "/assets/" + namespace + "/stationapi/textures/" + texturePath + ".png";
		for (String resourcePath : new String[]{nativePath, stationPath}) {
			try (InputStream is = AtlasExpander.class.getResourceAsStream(resourcePath)) {
				if (is != null) {
					return ImageIO.read(is);
				}
			} catch (IOException e) {
				LOGGER.error("Failed to load texture: {}", resourcePath, e);
				return null;
			}
		}
		LOGGER.warn("Texture not found: {} (also tried {})", nativePath, stationPath);
		return null;
	}
}
