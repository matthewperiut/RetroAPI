package com.periut.retrotweaks.feature.hud;

/**
 * Where the HUD-position editor's movable control panel currently sits, and whether it is
 * minimised.
 *
 * <p>Pure UI chrome, not a setting: it is never written to {@code retrotweaks.json} (a fresh install
 * should not have to decide where a debug panel goes) and resets to the default corner every time
 * the game restarts. Kept here, alongside {@link HudLayout}, rather than as instance fields on
 * {@code ConfigScreen} because a new {@code ConfigScreen} instance is built every time the player
 * navigates between the six HUD-element subsections (see {@code ConfigScreen.buttonClicked}) - an
 * instance field would forget the panel's position on every click into or out of a subsection. One
 * position is shared by all six subsections rather than remembered per element: simpler, and you are
 * never editing two of them at once.
 */
public final class HudPanelState {

	private HudPanelState() {}

	/** -1 means "not yet placed"; {@code ConfigScreen} picks a default corner the first time it lays
	 *  the panel out and never sees -1 again for the rest of this game session. */
	public static volatile int x = -1;
	public static volatile int y = -1;
	public static volatile boolean minimized = false;
}
