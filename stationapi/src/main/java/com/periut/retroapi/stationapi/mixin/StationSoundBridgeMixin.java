package com.periut.retroapi.stationapi.mixin;

import com.periut.retroapi.sound.impl.SoundAutoLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.SoundEntry;
import net.minecraft.client.sound.SoundManager;
import net.modificationstation.stationapi.api.client.sound.CustomSoundMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * StationAPI compat bridge for the sound autoloader. Active ONLY when StationAPI is present (gated by
 * {@code RetroAPIMixinPlugin}'s {@code STATIONAPI_ONLY_MIXINS} set - the inverse of the usual
 * disable list).
 *
 * <p>StationAPI's {@code station-audio-loader-v0} ({@code SoundManagerMixin}) already scans every mod
 * for the StationAPI layout {@code assets/<modid>/stationapi/sounds/<channel>/...}. It does <em>not</em>
 * scan the RetroAPI-native {@code assets/<modid>/sounds/<channel>/...} layout. This bridge fills that
 * gap: it injects the same {@code SoundManager.loadSounds(GameOptions)} TAIL and registers only the
 * native-root sounds into StationAPI's per-channel maps, so a RetroAPI mod gets both roots searched
 * - {@code stationapi/sounds/} via StationAPI, {@code sounds/} via RetroAPI - under StationAPI.</p>
 *
 * <p>This class references StationAPI's {@code CustomSoundMap}; gating it OFF when StationAPI is absent
 * keeps that class off the load path (no {@code NoClassDefFoundError}). When StationAPI is absent,
 * {@code SoundEngineMixin} owns autoloading instead and scans both roots itself.</p>
 *
 * <p>The cast target is StationAPI's {@code CustomSoundMap} (its {@code SoundMapMixin} implements it on
 * {@code SoundEntry}); under StationAPI our own {@code SoundsMixin} is disabled, so it is the only
 * {@code putSound} duck on {@code SoundEntry}.</p>
 */
@Mixin(SoundManager.class)
public abstract class StationSoundBridgeMixin {
	@Shadow
	private SoundEntry sounds;

	@Shadow
	private SoundEntry streamingSounds;

	@Shadow
	private SoundEntry music;

	@Inject(method = "loadSounds", at = @At("TAIL"))
	private void retroapi$bridgeNativeSounds(GameOptions options, CallbackInfo ci) {
		retroapi$bridge(this.sounds, "sound");
		retroapi$bridge(this.streamingSounds, "streaming");
		retroapi$bridge(this.music, "music");
	}

	@Unique
	private void retroapi$bridge(SoundEntry channelEntry, String channel) {
		CustomSoundMap target = (CustomSoundMap) (Object) channelEntry;
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			SoundAutoLoader.scanNative(mod.getMetadata().getId(), channel, target::putSound);
		}
	}
}
