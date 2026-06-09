package com.periut.retroapi.mixin.client.sound;

import com.periut.retroapi.sound.api.CustomSoundMap;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implements {@link CustomSoundMap} on vanilla {@code net.minecraft.client.sound.SoundEntry} (the
 * per-channel sound map holder) so the autoloader can insert a sound from a classpath {@link URL} (mod
 * sounds live inside jars and are not reachable as {@link java.io.File}s).
 *
 * <p>Near-verbatim port of StationAPI's {@code SoundMapMixin}, which mirrors vanilla
 * {@code SoundEntry.loadStatic(String, File)} (verified by decompile of the active b1.7.3 mappings):
 * same extension-strip at the first dot, same trailing-digit collapse gated on {@code isRandom}, same
 * {@code /}->{@code .} replace, same map/list/count writes. The only change is the source: URL not File.</p>
 *
 * <p>The active b1.7.3 mappings (biny build 57cc158) name these members exactly as StationAPI/Yarn do:
 * {@code isRandom}, {@code weightedSoundSet}, {@code loadedSounds}, {@code loadedSoundCount}.</p>
 */
@Mixin(SoundEntry.class)
public abstract class SoundsMixin implements CustomSoundMap {
	@Shadow
	public boolean isRandom;

	@Shadow
	private Map<String, List<Sound>> weightedSoundSet;

	@Shadow
	private List<Sound> loadedSounds;

	@Shadow
	public int loadedSoundCount;

	@Override
	@Unique
	public Sound retroapi$putSound(String id, URL url) {
		id = id.toLowerCase();
		// Require exactly one dot (the extension): filenames may not contain extra dots or spaces.
		if (id.length() - id.replace(".", "").length() != 1) {
			throw new RuntimeException(
				"RetroAPI sound files must have exactly one extension and no extra dots/spaces. "
					+ "e.g. \"wolf_bark.ogg\" is fine; \"wolf_bark\", \"wolf.bark.ogg\", "
					+ "\"wolf bark.ogg\" are not. File: \"" + id + "\"");
		}
		String filename = id;
		String event = id.split("\\.")[0];
		event = event.replace('/', '.');
		if (this.isRandom) {
			while (Character.isDigit(event.charAt(event.length() - 1))) {
				event = event.substring(0, event.length() - 1);
			}
		}
		if (!this.weightedSoundSet.containsKey(event)) {
			this.weightedSoundSet.put(event, new ArrayList<>());
		}
		Sound sound = new Sound(filename, url);
		this.weightedSoundSet.get(event).add(sound);
		this.loadedSounds.add(sound);
		++this.loadedSoundCount;
		return sound;
	}
}
