package com.periut.retroapi.mixin.client.sound;

import com.periut.retroapi.sound.api.CustomSoundMap;
import com.periut.retroapi.sound.impl.SoundAutoLoader;
import com.periut.retroapi.sound.impl.SoundChannelAccess;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.SoundEntry;
import net.minecraft.client.sound.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs the {@link SoundAutoLoader} at the TAIL of {@code SoundManager.loadSounds(GameOptions)} - the
 * per-startup init that builds the paulscode sound system. After it returns, the three per-channel
 * {@code SoundEntry} holders exist and are ready to receive entries. This is StationAPI's proven hook
 * point (its {@code SoundManagerMixin} injects the TAIL of the same {@code loadSounds(GameOptions)}).
 *
 * <p>The active b1.7.3 mappings (biny build 57cc158) name these members exactly as StationAPI/Yarn do.
 * The three {@code SoundEntry} fields are {@code sounds} ("sound" channel, {@code isRandom=true}),
 * {@code streamingSounds} ("streaming" channel, {@code isRandom} forced false in {@code loadSounds}),
 * and {@code music}.</p>
 *
 * <p>Disabled by {@code RetroAPIMixinPlugin} when StationAPI is present (its
 * {@code station-audio-loader-v0} owns this).</p>
 */
@Mixin(SoundManager.class)
public abstract class SoundEngineMixin {
	@Shadow
	private SoundEntry sounds;

	@Shadow
	private SoundEntry streamingSounds;

	@Shadow
	private SoundEntry music;

	@Inject(method = "loadSounds", at = @At("TAIL"))
	private void retroapi$autoloadSounds(GameOptions options, CallbackInfo ci) {
		CustomSoundMap soundCh = (CustomSoundMap) (Object) this.sounds;
		CustomSoundMap streamingCh = (CustomSoundMap) (Object) this.streamingSounds;
		CustomSoundMap musicCh = (CustomSoundMap) (Object) this.music;
		SoundChannelAccess.set(soundCh, streamingCh, musicCh);
		SoundAutoLoader.loadAll(soundCh, streamingCh, musicCh);
	}
}
