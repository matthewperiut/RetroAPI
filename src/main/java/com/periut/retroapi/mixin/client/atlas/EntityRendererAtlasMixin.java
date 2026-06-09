package com.periut.retroapi.mixin.client.atlas;

import com.periut.retroapi.client.texture.AtlasExpander;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Fix the on-fire overlay for entities on an EXPANDED terrain atlas. Vanilla
 * {@code EntityRenderer.renderOnFire} binds {@code /terrain.png} and normalizes the fire sprite's
 * pixel coordinates by a hardcoded {@code 256.0F} - correct only for the vanilla 256x256 atlas.
 * Once modded block textures grow the atlas (512+), those UVs land in the wrong cell (usually an
 * empty, fully transparent slot), so burning entities show NO flames at all on a RetroAPI client
 * while behaving normally otherwise. Same correction the other atlas mixins apply
 * ({@code ItemInHandRendererMixin} already fixes the first-person {@code renderFireOverlay}).
 * Sprite-local pixel offsets (the {@code 16}/{@code 15.99F} constants) are untouched - cells stay
 * 16px; only the divisor scales with the atlas.
 */
@Mixin(EntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class EntityRendererAtlasMixin {
	@ModifyConstant(method = "renderOnFire", constant = @Constant(floatValue = 256.0F))
	private float retroapi$fireAtlasSize(float original) {
		return AtlasExpander.terrainAtlasSize;
	}
}
