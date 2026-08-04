package com.periut.retroapi.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.periut.retroapi.register.block.RetroDisguises;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes a disguised block break with the sound of what it was wearing. See
 * {@link com.periut.retroapi.register.block.RetroBlockDisguise}.
 *
 * <p>The break sound is world event 2001, which arrives carrying a block id and a position and is handled
 * after the block itself has gone, so the disguise cannot be read from the world any more. It is read
 * from what {@code RetroDisguises} recorded on the way out instead.
 *
 * <p>The method is {@code worldEvent}. This targeted {@code processWorldEvent} for a while, which does not
 * exist, and because the injection was optional it simply never ran: the sound stayed wooden and nothing
 * anywhere said so. Optional injections hide exactly this, which is why these are required now.
 */
@Mixin(WorldRenderer.class)
@Environment(EnvType.CLIENT)
public class DisguisedBreakSoundMixin {

	@WrapOperation(method = "worldEvent",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/sound/SoundManager;playSound(Ljava/lang/String;FFFFF)V"),
		require = 1)
	private void retroapi$disguisedBreakSound(SoundManager sounds, String sound, float x, float y, float z,
			float volume, float pitch, Operation<Void> original,
			net.minecraft.entity.player.PlayerEntity player, int event, int px, int py, int pz, int data) {
		Block worn = RetroDisguises.remembered(px, py, pz);
		if (worn == null) {
			original.call(sounds, sound, x, y, z, volume, pitch);
			return;
		}
		// The same arithmetic vanilla applies to a break sound, on the group it should have used.
		original.call(sounds, worn.soundGroup.getBreakSound(), x, y, z,
			(worn.soundGroup.getVolume() + 1.0F) / 2.0F, worn.soundGroup.getPitch() * 0.8F);
	}
}
