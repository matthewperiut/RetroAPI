package com.periut.retrotweaks.feature.input;

import net.minecraft.client.Mouse;

import java.awt.Component;

/**
 * The mouse handler the game uses while raw input is on: it reports the counts
 * {@link RawInput} collected from the device rather than the cursor's screen movement.
 */
public class RawMouse extends Mouse {

	public RawMouse(Component parent) {
		super(parent);
	}

	@Override
	public void poll() {
		this.deltaX = RawInput.deltaX;
		// The device's Y axis grows downwards and the camera's grows upwards.
		this.deltaY = -RawInput.deltaY;
		RawInput.resetDelta();
	}

	@Override
	public void lockCursor() {
		super.lockCursor();
		// Anything that accumulated while the cursor was free would arrive as one huge jerk.
		RawInput.resetDelta();
	}
}
