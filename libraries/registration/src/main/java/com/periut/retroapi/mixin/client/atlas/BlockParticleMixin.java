package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.entity.particle.BlockParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlockParticle.class)
@Environment(EnvType.CLIENT)
public class BlockParticleMixin {

	@ModifyConstant(
		method = "render",
		constant = @Constant(floatValue = 16.0F)
	)
	private float retroapi$fixColumnCount(float original) {
		return AtlasExpander.terrainAtlasSize / 16.0F;
	}

	@ModifyConstant(
		method = "render",
		constant = @Constant(floatValue = 0.015609375F)
	)
	private float retroapi$fixSubSpriteSize(float original) {
		return 0.015609375F * 256.0F / AtlasExpander.terrainAtlasSize;
	}
}
