package com.periut.retroapi.mixin.client;

import com.periut.retroapi.tag.RetroBreakTarget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MultiplayerInteractionManager;
import net.minecraft.client.SingleplayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records the block being broken on the classes that actually break blocks.
 *
 * <p>{@code BreakTargetClientMixin} marks the target on {@code InteractionManager}, which declares
 * {@code processBlockBreakingAction} but is not the class that runs it: both subclasses override it and
 * neither calls {@code super}. So while a player was holding the button down, nothing was recording where
 * they were pointing, and everything downstream of that record, {@link
 * com.periut.retroapi.tag.RetroToolTier.Positional} and the per-position tool of a disguised block, was
 * asking a question nobody had written the answer to. It failed silently, by falling back to the
 * block-wide answer, which is the same answer for most blocks and so looked correct nearly everywhere.
 */
@Environment(EnvType.CLIENT)
@Mixin({SingleplayerInteractionManager.class, MultiplayerInteractionManager.class})
public class BreakTargetSubclassMixin {

	@Inject(method = "processBlockBreakingAction", at = @At("HEAD"), require = 1)
	private void retroapi$markProgress(int x, int y, int z, int side, CallbackInfo ci) {
		Minecraft minecraft = ((InteractionManagerAccessor) this).retroapi$minecraft();
		if (minecraft != null && minecraft.player != null && minecraft.world != null) {
			RetroBreakTarget.set(minecraft.player, minecraft.world, x, y, z);
		}
	}

	@Unique
	private static void retroapi$unused() {}
}
