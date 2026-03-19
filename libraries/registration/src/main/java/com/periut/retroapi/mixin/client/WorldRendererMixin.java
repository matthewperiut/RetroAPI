package com.periut.retroapi.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Shadow
	private Minecraft minecraft;

	@Shadow
	private World world;

	@Inject(method = "doEvent", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$handleExtendedBlockBreak(PlayerEntity player, int event, int x, int y, int z, int data, CallbackInfo ci) {
		if (event == 2001) {
			int blockId = data & 0xFFF;
			int metadata = (data >> 12) & 0xF;

			if (blockId > 0) {
				Block block = Block.BY_ID[blockId];
				if (block != null) {
					minecraft.soundEngine.play(
						block.sounds.getBreaking(),
						x + 0.5f, y + 0.5f, z + 0.5f,
						(block.sounds.getVolume() + 1.0f) / 2.0f,
						block.sounds.getPitch() * 0.8f
					);
				}
			}

#if MC_B1_6_OR_LATER
			minecraft.particleManager.handleBlockBreaking(x, y, z, blockId, metadata);
#else
			minecraft.particleManager.handleBlockBreaking(x, y, z);
#endif
			ci.cancel();
		}
	}
}
