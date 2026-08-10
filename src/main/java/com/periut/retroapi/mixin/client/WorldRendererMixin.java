package com.periut.retroapi.mixin.client;

import com.periut.retroapi.register.block.RetroDisguises;
import com.periut.retroapi.registry.RetroIds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Shadow
	private Minecraft client;

	@Shadow
	private World world;

	@Inject(method = "worldEvent", at = @At("HEAD"), cancellable = true, require = 0)
	private void retroapi$handleExtendedBlockBreak(PlayerEntity player, int event, int x, int y, int z, int data, CallbackInfo ci) {
		if (event == 2001) {
			// Decode the block-break packing. Must match the encode side (Client/Server
			// PlayerInteractionManager mixins) AND StationAPI's flattening layout: blockId in the low
			// 28 bits, metadata in bits 28+. Using a 12-bit layout here against StationAPI's 28-bit
			// encode silently dropped metadata, breaking particles for metadata-driven blocks (tall
			// grass / ferns / cross plants). See RetroIds.
			int blockId = data & RetroIds.BREAK_EVENT_ID_MASK;
			int metadata = (data >> RetroIds.BREAK_EVENT_META_SHIFT) & 0xF;

			// A disguised block breaks with the sound of what it was wearing. This has to be answered
			// HERE rather than by a hook on vanilla's break sound, because the branch below replaces
			// vanilla's and cancels it: anything bound to the call this one supersedes would apply
			// cleanly and then never run.
			//
			// The position is asked first and the record of what left is the fallback. Playing alone the
			// event is fired just before the block is taken out, so the disguise is still there to read;
			// arriving from a server it may land after.
			Block worn = RetroDisguises.liveOrRemembered(this.world, x, y, z);
			if (worn != null) {
				retroapi$playBreakSound(worn, x, y, z);
			} else if (blockId > 0) {
				Block block = Block.BLOCKS[blockId];
				if (block != null) {
					retroapi$playBreakSound(block, x, y, z);
				}
			}

			client.particleManager.addBlockBreakParticles(x, y, z, blockId, metadata);
			ci.cancel();
		}
	}

	/** Vanilla's own arithmetic for a break sound, on whichever block should be heard. */
	@Unique
	private void retroapi$playBreakSound(Block block, int x, int y, int z) {
		client.soundManager.playSound(
			block.soundGroup.getBreakSound(),
			x + 0.5f, y + 0.5f, z + 0.5f,
			(block.soundGroup.getVolume() + 1.0f) / 2.0f,
			block.soundGroup.getPitch() * 0.8f
		);
	}
}

