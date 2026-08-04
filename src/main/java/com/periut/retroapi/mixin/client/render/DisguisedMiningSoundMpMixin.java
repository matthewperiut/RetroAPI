package com.periut.retroapi.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.periut.retroapi.register.block.RetroDisguises;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MultiplayerInteractionManager;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The tapping sound while a block is being mined, taken from the disguise. See
 * {@link com.periut.retroapi.register.block.RetroBlockDisguise}.
 *
 * <p>This is a separate sound from the one when the block finally gives way, played from a separate place
 * every four ticks of mining, and it is the one you hear most of: breaking a block by hand is a second of
 * tapping and a single crunch at the end. Fixing only the crunch leaves a stone-clad frame sounding like
 * wood for the entire time you are actually hitting it.
 *
 * <p>The multiplayer half of the same hook: the two interaction managers do not share a supertype that
 * carries this method, so the fix is stated twice rather than made to fit one that does not exist.
 */
@Mixin(MultiplayerInteractionManager.class)
@Environment(EnvType.CLIENT)
public class DisguisedMiningSoundMpMixin {

	@WrapOperation(method = "processBlockBreakingAction",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/sound/SoundManager;playSound(Ljava/lang/String;FFFFF)V"),
		require = 0)
	private void retroapi$disguisedMiningSound(SoundManager sounds, String sound, float sx, float sy, float sz,
			float volume, float pitch, Operation<Void> original, int x, int y, int z, int side) {
		// The world comes through the accessor, not from a break record: the record is written by a
		// different mixin on a different class, and depending on one hook to have run before another is
		// how this ended up playing wooden sounds for a stone-clad block in the first place.
		net.minecraft.client.Minecraft minecraft =
			((com.periut.retroapi.mixin.client.InteractionManagerAccessor) this).retroapi$minecraft();
		Block worn = minecraft == null || minecraft.world == null
			? null
			: RetroDisguises.at(minecraft.world, x, y, z);
		if (worn == null) {
			original.call(sounds, sound, sx, sy, sz, volume, pitch);
			return;
		}
		// Vanilla's own arithmetic for this sound, on the group it should have come from.
		original.call(sounds, worn.soundGroup.getSound(), sx, sy, sz,
			(worn.soundGroup.getVolume() + 1.0F) / 8.0F, worn.soundGroup.getPitch() * 0.5F);
	}
}
