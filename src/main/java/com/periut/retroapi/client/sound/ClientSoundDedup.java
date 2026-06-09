package com.periut.retroapi.client.sound;

import java.util.ArrayDeque;
import java.util.Iterator;

/**
 * Client-side dedup for the server->client world-sound bridge.
 * <p>
 * The client already plays many sounds locally before (or independently of) the server's bridged
 * copy arriving: block place/break/door prediction, mob hurt sounds via entity status 2, etc.
 * Local plays win - they have zero latency. Every locally played sound is recorded here
 * ({@code WorldRendererSoundMixin}), and when a bridged sound arrives the listener first asks
 * {@link #consumeIfRecentlyPlayed}: a matching recent local play (same name, within
 * {@value #MAX_DISTANCE} blocks, within {@value #WINDOW_MS} ms) means the bridged copy is a
 * duplicate and is dropped. Sounds the client did NOT predict (custom mob sounds, other players'
 * actions out of vanilla's delivery paths) find no match and play normally - slightly delayed by
 * the network, but better than silence.
 * <p>
 * Matches are consumed one-for-one so N simultaneous identical sounds dedup N bridged copies,
 * not all of them. Bridge-initiated plays are not recorded ({@link #applyingBridgedSound}),
 * otherwise a repeating server-only sound would dedup its own next repetition.
 * <p>
 * Main-thread only (both the recording hook and the OSL listener run on the client main thread).
 */
public final class ClientSoundDedup {
	private ClientSoundDedup() {}

	private static final long WINDOW_MS = 500;
	private static final double MAX_DISTANCE = 4.0; // entity interpolation can offset positions a bit
	private static final double MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;
	private static final int MAX_ENTRIES = 64;

	/** Set while the bridge listener feeds a received sound into world.playSound. */
	public static boolean applyingBridgedSound = false;

	private record Entry(String sound, double x, double y, double z, long time) {}

	private static final ArrayDeque<Entry> recent = new ArrayDeque<>();

	/** Record a sound the client played on its own (called from the WorldRenderer hook). */
	public static void recordLocalPlay(String sound, double x, double y, double z) {
		if (applyingBridgedSound || sound == null || sound.isEmpty()) return;
		long now = System.currentTimeMillis();
		prune(now);
		if (recent.size() >= MAX_ENTRIES) {
			recent.pollFirst();
		}
		recent.addLast(new Entry(sound, x, y, z, now));
	}

	/** True (and consumes the record) if the client already played this sound just now, nearby. */
	public static boolean consumeIfRecentlyPlayed(String sound, double x, double y, double z) {
		prune(System.currentTimeMillis());
		for (Iterator<Entry> it = recent.iterator(); it.hasNext(); ) {
			Entry e = it.next();
			if (!e.sound.equals(sound)) continue;
			double dx = e.x - x, dy = e.y - y, dz = e.z - z;
			if (dx * dx + dy * dy + dz * dz <= MAX_DISTANCE_SQ) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	private static void prune(long now) {
		while (!recent.isEmpty() && now - recent.peekFirst().time > WINDOW_MS) {
			recent.pollFirst();
		}
	}
}
