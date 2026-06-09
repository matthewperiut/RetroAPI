package com.periut.retroapi.sound.api;

import net.minecraft.client.Minecraft;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

/**
 * Plays a specific track when the player enters a world, the "theme on load" pattern.
 *
 * <p>Two channels are involved and worth distinguishing. The MUSIC channel
 * ({@code assets/{mod}/sounds/music/}) feeds vanilla's background music ticker, which
 * picks a random track at random intervals; you do not control WHICH or WHEN. To force
 * a SPECIFIC track at a known moment (world load), the track goes in the STREAMING
 * channel ({@code assets/{mod}/sounds/streaming/}) and is played with
 * {@code SoundManager.playStreaming}, the same call vanilla uses for jukebox records.</p>
 *
 * <p>Client-only. Register once from your client entrypoint:
 * {@code RetroMusic.playOnWorldLoad("example_mod:aether1");}</p>
 */
public final class RetroMusic {

	private RetroMusic() {
	}

	/**
	 * Plays {@code streamingEventId} once each time a world finishes loading on this client.
	 * The id is a STREAMING-channel event id (file at {@code sounds/streaming/<name>.ogg},
	 * id {@code "<mod>:<name>"}).
	 */
	public static void playOnWorldLoad(String streamingEventId) {
		MinecraftClientEvents.READY_WORLD.register(minecraft -> play(minecraft, streamingEventId));
	}

	/** Plays a streaming track immediately, at the player if there is one, else non-positional. */
	public static void play(Minecraft minecraft, String streamingEventId) {
		if (minecraft == null || minecraft.soundManager == null) {
			return;
		}
		float x = 0.0F, y = 0.0F, z = 0.0F;
		if (minecraft.player != null) {
			x = (float) minecraft.player.x;
			y = (float) minecraft.player.y;
			z = (float) minecraft.player.z;
		}
		minecraft.soundManager.playStreaming(streamingEventId, x, y, z, 1.0F, 1.0F);
	}
}
