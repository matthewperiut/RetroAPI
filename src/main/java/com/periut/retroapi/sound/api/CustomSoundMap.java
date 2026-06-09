package com.periut.retroapi.sound.api;

import net.minecraft.client.sound.Sound;

import java.net.URL;

/**
 * Duck interface cast onto vanilla {@code net.minecraft.client.sound.SoundEntry} (the per-channel
 * sound map holder) so the autoloader can insert a sound from a classpath {@link URL} instead of a
 * {@link java.io.File}. Mirrors StationAPI's {@code CustomSoundMap}.
 *
 * <p>Implemented by {@code com.periut.retroapi.mixin.client.sound.SoundsMixin}.</p>
 */
public interface CustomSoundMap {
	/**
	 * Register a single sound under the given path-style id, sourced from a classpath URL.
	 *
	 * @param id  the lowercased path-with-extension id, e.g. {@code "aether:other/dungeontrap/activatetrap.ogg"}
	 * @param url the resource URL pointing at the OGG/WAV inside a mod jar (or exploded classpath dir)
	 * @return the inserted {@link Sound}
	 */
	Sound retroapi$putSound(String id, URL url);
}
