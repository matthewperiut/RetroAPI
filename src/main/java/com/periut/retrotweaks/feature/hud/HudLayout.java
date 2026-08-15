package com.periut.retrotweaks.feature.hud;

/**
 * Shared state for the HUD layout.
 *
 * <p>{@link #configUpdated} tells the HUD to recompute its offsets on the next frame. The layout is
 * only recalculated when the window size or the config changes, because working it out every frame
 * for every element would be pure waste - but that means something has to say when the config moved.
 *
 * <p>The fields below are published by {@code InGameHudMixin.retrotweaks$render} every time that
 * recompute happens - a copy of exactly what it just worked out for this frame's real HUD draw, not
 * a second opinion. The HUD-position editor ({@code ConfigScreen}'s drag-to-move mode, via
 * {@link HudEditorMath}) reads them to hit-test and outline the real on-screen element without
 * duplicating any of that maths itself. They are only meaningful once a world is loaded and
 * {@code InGameHud.render()} has run at least one frame; every field defaults to the same "nothing
 * hidden, nothing offset" state the mixin itself starts from.
 */
public final class HudLayout {

	private HudLayout() {}

	/** Set when a HUD option changes; cleared by the HUD once it has recalculated. */
	public static volatile boolean configUpdated = true;

	// ---- published alongside the recompute above - see InGameHudMixin.retrotweaks$render ----
	public static volatile int scaledWidth;
	public static volatile int scaledHeight;
	public static volatile boolean hotbarVisible = true;
	public static volatile boolean heartsVisible = true;
	public static volatile boolean armorVisible = true;
	public static volatile boolean oxygenVisible = true;
	public static volatile boolean overlayVisible = true;
	public static volatile int xOffsetHotbar, yOffsetHotbar;
	public static volatile int xOffsetHearts, yOffsetHearts;
	public static volatile int xOffsetArmor, yOffsetArmor;
	public static volatile int xOffsetOxygen, yOffsetOxygen;
	public static volatile int xOffsetOverlay, yOffsetOverlay;

	/** True while {@code kind} is currently drawn at all - a hidden element has nothing to hit-test. */
	public static boolean visible(HudEditorMath.Kind kind) {
		return switch (kind) {
			case HOTBAR -> hotbarVisible;
			case HEARTS -> heartsVisible;
			case ARMOR -> armorVisible;
			case OXYGEN -> oxygenVisible;
			case OVERLAY -> overlayVisible;
		};
	}

	public static int xOffset(HudEditorMath.Kind kind) {
		return switch (kind) {
			case HOTBAR -> xOffsetHotbar;
			case HEARTS -> xOffsetHearts;
			case ARMOR -> xOffsetArmor;
			case OXYGEN -> xOffsetOxygen;
			case OVERLAY -> xOffsetOverlay;
		};
	}

	public static int yOffset(HudEditorMath.Kind kind) {
		return switch (kind) {
			case HOTBAR -> yOffsetHotbar;
			case HEARTS -> yOffsetHearts;
			case ARMOR -> yOffsetArmor;
			case OXYGEN -> yOffsetOxygen;
			case OVERLAY -> yOffsetOverlay;
		};
	}
}
