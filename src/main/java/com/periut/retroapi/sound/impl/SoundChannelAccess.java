package com.periut.retroapi.sound.impl;

import com.periut.retroapi.sound.api.CustomSoundMap;
import com.periut.retroapi.sound.api.RetroSounds;

/**
 * Holds references to the live per-channel {@code SoundEntry} maps (as {@link CustomSoundMap}) captured
 * by {@code SoundEngineMixin} during sound-manager init, so the public {@link RetroSounds} API can
 * register sounds programmatically after startup. Client-only, single-threaded (sound engine thread).
 */
public final class SoundChannelAccess {
	private static CustomSoundMap sound;
	private static CustomSoundMap streaming;
	private static CustomSoundMap music;

	private SoundChannelAccess() {
	}

	/** Called from {@code SoundEngineMixin} on {@code SoundManager.loadSounds(GameOptions)} TAIL. */
	public static void set(CustomSoundMap soundChannel, CustomSoundMap streamingChannel,
	                       CustomSoundMap musicChannel) {
		sound = soundChannel;
		streaming = streamingChannel;
		music = musicChannel;
	}

	public static CustomSoundMap get(RetroSounds.SoundChannel channel) {
		switch (channel) {
			case SOUND:
				return sound;
			case STREAMING:
				return streaming;
			case MUSIC:
				return music;
			default:
				return null;
		}
	}
}
