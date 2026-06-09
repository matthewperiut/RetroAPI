package com.periut.retroapi.sound.api;

import com.periut.retroapi.sound.impl.SoundChannelAccess;
import net.minecraft.client.sound.Sound;

import java.net.URL;

/**
 * Public, optional API for RetroAPI-native mods that want to register a sound programmatically rather
 * than relying purely on the autoloader. The autoloader covers 100% of Aether's needs with zero code;
 * this is convenience sugar.
 *
 * <p>Client-only. The three channels map to vanilla's {@code SoundManager} per-channel
 * {@code SoundEntry} holders. Registration is only possible after the {@code SoundManager} has been
 * initialised (i.e. once the game's sound system has loaded); calling before then is a no-op and
 * returns {@code null}.</p>
 */
public final class RetroSounds {
	private RetroSounds() {
	}

	/** Vanilla sound channels. {@link #SOUND} and {@link #MUSIC} collapse random variants; {@link #STREAMING} does not. */
	public enum SoundChannel {
		/** Effect sounds (random-variant collapse enabled). */
		SOUND,
		/** Streaming records (no random-variant collapse). */
		STREAMING,
		/** Music tracks. */
		MUSIC
	}

	/**
	 * Register one sound from a classpath URL under an explicit path-style id (with extension), e.g.
	 * {@code register(SoundChannel.SOUND, "mymod:mob/foo/bar.ogg", url)}. The event id is derived the
	 * StationAPI way (extension stripped, slashes -> dots, trailing digits collapsed when the channel is
	 * random), so the example registers event {@code "mymod:mob.foo.bar"}.
	 *
	 * @return the inserted {@link Sound}, or {@code null} if the sound engine is not yet available
	 */
	public static Sound register(SoundChannel channel, String pathId, URL ogg) {
		CustomSoundMap map = SoundChannelAccess.get(channel);
		if (map == null) {
			return null;
		}
		return map.retroapi$putSound(pathId, ogg);
	}

	/**
	 * Derive the event id from a resource path the StationAPI way (without inserting anything):
	 * extension stripped, {@code /} -> {@code .}, lowercased. Trailing-digit collapse is NOT applied
	 * here (it depends on the target channel's random flag at insert time).
	 *
	 * @param modId        the mod namespace
	 * @param relativePath path under the channel folder, e.g. {@code "mob/foo/bar.ogg"}
	 * @return the event id, e.g. {@code "mymod:mob.foo.bar"}
	 */
	public static String eventIdFor(String modId, String relativePath) {
		String rel = relativePath.replace('\\', '/');
		int dot = rel.indexOf('.');
		if (dot >= 0) {
			rel = rel.substring(0, dot);
		}
		rel = rel.replace('/', '.');
		return (modId + ":" + rel).toLowerCase();
	}
}
