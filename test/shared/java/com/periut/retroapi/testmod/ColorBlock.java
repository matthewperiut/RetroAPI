package com.periut.retroapi.testmod;

import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;
import com.periut.retroapi.register.block.RetroTexture;
import com.periut.retroapi.register.block.RetroTextures;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class ColorBlock extends Block {
	private final RetroTexture[] faceTextures = new RetroTexture[6];

	public ColorBlock(int id, Material material) {
		super(id, material);
		faceTextures[0] = RetroTextures.addBlockTexture(NamespacedIdentifiers.from("retroapi_test", "color_block_bottom"));
		faceTextures[1] = RetroTextures.addBlockTexture(NamespacedIdentifiers.from("retroapi_test", "color_block_top"));
		faceTextures[2] = RetroTextures.addBlockTexture(NamespacedIdentifiers.from("retroapi_test", "color_block_north"));
		faceTextures[3] = RetroTextures.addBlockTexture(NamespacedIdentifiers.from("retroapi_test", "color_block_south"));
		faceTextures[4] = RetroTextures.addBlockTexture(NamespacedIdentifiers.from("retroapi_test", "color_block_west"));
		faceTextures[5] = RetroTextures.addBlockTexture(NamespacedIdentifiers.from("retroapi_test", "color_block_east"));
		this.sprite = faceTextures[0].id;
		RetroTextures.trackBlock(this, faceTextures[0]);
	}

	@Override
	public int getSprite(int face) {
		if (face >= 0 && face < faceTextures.length) {
			return faceTextures[face].id;
		}
		return super.getSprite(face);
	}
}
