package com.periut.retroapi.mixin.client.sound;

import com.periut.retroapi.client.sound.ClientSoundDedup;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records every sound the client plays locally - {@code WorldRenderer} is the client world's
 * {@code GameEventListener}, so all {@code world.playSound(...)} calls (block place/break
 * prediction, mob hurt via entity status, ambient, everything) funnel through here. The
 * world-sound bridge listener then drops its copy of any sound the client already played
 * ({@link ClientSoundDedup}), instead of double-playing it.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererSoundMixin {

	@Inject(method = "playSound", at = @At("HEAD"))
	private void retroapi$recordLocalSound(String sound, double x, double y, double z, float volume, float pitch, CallbackInfo ci) {
		ClientSoundDedup.recordLocalPlay(sound, x, y, z);
	}
}
