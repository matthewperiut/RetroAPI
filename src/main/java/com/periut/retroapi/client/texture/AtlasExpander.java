package com.periut.retroapi.client.texture;

import com.periut.retroapi.register.block.RetroTexture;
import com.periut.retroapi.register.block.RetroTextures;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AtlasExpander {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/AtlasExpander");
	private static final int ATLAS_COLUMNS = 16;

	/** Current terrain atlas size (width = height, always square, power of 2). */
	public static int terrainAtlasSize = 256;
	/** Current item atlas size (width = height, always square, power of 2). */
	public static int itemAtlasSize = 256;

	private static int nextPowerOfTwo(int value) {
		if (value <= 256) return 256;
		return Integer.highestOneBit(value - 1) << 1;
	}

	public static BufferedImage expandTerrainAtlas(BufferedImage original) {
		List<RetroTexture> textures = RetroTextures.getTerrainTextures();
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
		terrainAtlasSize = atlasSize;

		LOGGER.info("Expanding terrain atlas to {}x{} (vanilla: {}x{})", atlasSize, atlasSize, original.getWidth(), original.getHeight());

		BufferedImage atlas = new BufferedImage(atlasSize, atlasSize, BufferedImage.TYPE_INT_ARGB);
		atlas.getGraphics().drawImage(original, 0, 0, null);

		for (RetroTexture tex : textures) {
			BufferedImage texture = loadTexture(tex.getIdentifier().namespace(), "block/" + tex.getIdentifier().identifier());
			if (texture != null) {
				int col = tex.id % ATLAS_COLUMNS;
				int row = tex.id / ATLAS_COLUMNS;
				atlas.getGraphics().drawImage(texture, col * spriteSize, row * spriteSize, spriteSize, spriteSize, null);
				LOGGER.debug("Composited block texture {} at slot {}", tex.getIdentifier(), tex.id);
			}
		}

		return atlas;
	}

	public static BufferedImage expandItemAtlas(BufferedImage original) {
		List<RetroTexture> textures = RetroTextures.getItemTextures();
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
		itemAtlasSize = atlasSize;

		LOGGER.info("Expanding item atlas to {}x{} (vanilla: {}x{})", atlasSize, atlasSize, original.getWidth(), original.getHeight());

		BufferedImage atlas = new BufferedImage(atlasSize, atlasSize, BufferedImage.TYPE_INT_ARGB);
		atlas.getGraphics().drawImage(original, 0, 0, null);

		for (RetroTexture tex : textures) {
			BufferedImage texture = loadTexture(tex.getIdentifier().namespace(), "item/" + tex.getIdentifier().identifier());
			if (texture != null) {
				int col = tex.id % ATLAS_COLUMNS;
				int row = tex.id / ATLAS_COLUMNS;
				atlas.getGraphics().drawImage(texture, col * spriteSize, row * spriteSize, spriteSize, spriteSize, null);
				LOGGER.debug("Composited item texture {} at slot {}", tex.getIdentifier(), tex.id);
			}
		}

		return atlas;
	}

	private static BufferedImage loadTexture(String namespace, String texturePath) {
		String resourcePath = "/assets/" + namespace + "/textures/" + texturePath + ".png";
		try (InputStream is = AtlasExpander.class.getResourceAsStream(resourcePath)) {
			if (is != null) {
				return ImageIO.read(is);
			}
			LOGGER.warn("Texture not found: {}", resourcePath);
		} catch (IOException e) {
			LOGGER.error("Failed to load texture: {}", resourcePath, e);
		}
		return null;
	}
}
