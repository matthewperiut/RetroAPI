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
 * <p>The break sound is world event 2001, and WHEN it arrives depends on who sent it. Playing alone, the
 * interaction manager fires it immediately before {@code setBlock(x, y, z, 0)}, so the block and its
 * disguise are both still standing. From a server it is a packet, and the client may well have taken the
 * block out by the time it lands. So the position is asked first and the record of what left is only the
 * fallback: reading the record alone found nothing in singleplayer, and the sound stayed wooden.
 *
 * <p>This is the fallback path, and usually does nothing.
 * {@link com.periut.retroapi.mixin.client.WorldRendererMixin} replaces the whole of case 2001 at the head
 * of the same method, in order to widen vanilla's eight bit block id, and cancels; when it applies, the
 * call wrapped here is never reached and the disguise is handled there. This one covers the case where it
 * does not apply, since that injection is optional.
 *
 * <p>Both of those are worth stating plainly, because both cost real time to find. The method is
 * {@code worldEvent}: this targeted {@code processWorldEvent} for a while, which does not exist, and
 * because the injection was optional it simply never ran. Made required, it then applied cleanly and still
 * never ran, because the code around it had been cancelled. An injection applying and an injection
 * running are two different claims, and only the first of them is ever checked for you.
 */
@Mixin(WorldRenderer.class)
@Environment(EnvType.CLIENT)
public class DisguisedBreakSoundMixin {

	@org.spongepowered.asm.mixin.Shadow private net.minecraft.world.World world;

	@WrapOperation(method = "worldEvent",
		at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/sound/SoundManager;playSound(Ljava/lang/String;FFFFF)V"),
		require = 1)
	private void retroapi$disguisedBreakSound(SoundManager sounds, String sound, float x, float y, float z,
			float volume, float pitch, Operation<Void> original,
			net.minecraft.entity.player.PlayerEntity player, int event, int px, int py, int pz, int data) {
		Block worn = RetroDisguises.liveOrRemembered(this.world, px, py, pz);
		if (worn == null) {
			original.call(sounds, sound, x, y, z, volume, pitch);
			return;
		}
		// The same arithmetic vanilla applies to a break sound, on the group it should have used.
		original.call(sounds, worn.soundGroup.getBreakSound(), x, y, z,
			(worn.soundGroup.getVolume() + 1.0F) / 2.0F, worn.soundGroup.getPitch() * 0.8F);
	}
}
