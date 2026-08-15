package com.periut.retrotweaks.feature.input;

import com.periut.retrotweaks.RetroTweaks;
import com.periut.retrotweaks.config.Config;

import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads mouse movement straight from the device instead of from the desktop cursor. From UniTweaks.
 *
 * <p>The cursor the window sees has already been through the OS: pointer acceleration, DPI scaling
 * and any desktop smoothing are baked in, and the value is clamped to whole screen pixels. Reading
 * the device through JInput gives the raw counts, so aim is linear and sub-pixel movement is not
 * thrown away.
 *
 * <p>A polling thread is used because JInput has no callback - the device has to be asked. It sleeps
 * a millisecond between polls when active and a full second when not, so the cost when the option is
 * off is nothing worth measuring.
 *
 * <p>Unlike UniTweaks this never writes to chat. Everything it has to say goes to the log; a mouse
 * setting is not worth interrupting the game for, which is exactly what was asked for.
 */
public final class RawInput {

	private RawInput() {}

	private static final List<Mouse> MICE = new ArrayList<>();

	/** Accumulated raw counts since the last poll, drained by {@link RawMouse}. */
	static volatile int deltaX;
	static volatile int deltaY;

	private static Thread pollThread;
	private static volatile boolean running;
	private static boolean active;

	public static boolean isActive() {
		return active;
	}

	/**
	 * Finds the mice and starts polling. Returns false if JInput found none - which is the normal
	 * outcome when its native library is missing - so the caller can stay on the vanilla path.
	 */
	public static boolean start() {
		if (!Config.SYSTEM.rawInput) return false;
		if (!findMice()) {
			RetroTweaks.LOGGER.info("Raw input is on but no mouse device was found; using the normal cursor.");
			return false;
		}
		active = true;
		startPolling();
		return true;
	}

	public static void stop() {
		active = false;
		running = false;
		deltaX = 0;
		deltaY = 0;
	}

	/** Zeroes the accumulated movement, so grabbing the cursor does not fling the camera. */
	public static void resetDelta() {
		deltaX = 0;
		deltaY = 0;
	}

	private static boolean findMice() {
		MICE.clear();
		try {
			for (Controller controller : ControllerEnvironment.getDefaultEnvironment().getControllers()) {
				if (controller instanceof Mouse mouse) MICE.add(mouse);
			}
		} catch (Throwable t) {
			// JInput throws UnsatisfiedLinkError when its native library is absent, which is the
			// usual case on modern Linux. Not an error worth a stack trace - just no raw input.
			RetroTweaks.LOGGER.info("Raw input is unavailable on this system ({})", t.getClass().getSimpleName());
			return false;
		}
		RetroTweaks.LOGGER.info("Raw input found {} mouse device(s)", MICE.size());
		return !MICE.isEmpty();
	}

	private static void startPolling() {
		if (pollThread != null && pollThread.isAlive()) return;
		running = true;
		pollThread = new Thread(() -> {
			while (running) {
				try {
					for (Mouse mouse : MICE) {
						mouse.poll();
						deltaX += (int) mouse.getX().getPollData();
						deltaY += (int) mouse.getY().getPollData();
					}
					Thread.sleep(1);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				} catch (Throwable t) {
					RetroTweaks.LOGGER.warn("Raw input polling stopped", t);
					running = false;
					active = false;
					return;
				}
			}
		}, "RetroTweaks raw input");
		pollThread.setDaemon(true);
		pollThread.start();
	}
}
