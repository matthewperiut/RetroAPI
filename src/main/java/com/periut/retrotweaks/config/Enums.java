package com.periut.retrotweaks.config;

/**
 * Every multi-choice config value, gathered in one place. They are nested here rather than given a
 * file each because each is three constants and a label, and the config screen only ever needs
 * {@code values()} and {@code toString()} from them.
 */
public final class Enums {

	private Enums() {}

	/** Label carrier so the config screen can print "Drop Boat" instead of "DROP_BOAT". */
	private interface Labelled {
		String label();
	}

	public enum BoatCollision implements Labelled {
		VANILLA("Vanilla"), DROP_BOAT("Drop Boat"), INVINCIBLE("Invincible");
		private final String label;
		BoatCollision(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum PickBlock implements Labelled {
		NO_CHECK_META("Do Not Check Meta-Data"), CHECK_META("Check Meta-Data"), CHECK_MORE_EXACT("Check More Exact");
		private final String label;
		PickBlock(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum BedBehavior implements Labelled {
		VANILLA("Vanilla"),
		DISABLE_NIGHTMARES("Disable Nightmares"),
		SET_SPAWN_POINT_ONLY("Set Spawn Point Only"),
		DISABLE_ENTIRELY("Disable Entirely");
		private final String label;
		BedBehavior(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum ZombieDrop implements Labelled {
		FEATHER("Feathers"), RED_MUSHROOM("Red Mushrooms"), CYAN_DYE("Cyan Dye"), GREEN_DYE("Green Dye"),
		CLAY("Clay"), PAPER("Paper"), NOTHING("Nothing");
		private final String label;
		ZombieDrop(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum PigmanDrop implements Labelled {
		COOKED_PORKCHOP("Cooked Porkchops"), RAW_PORKCHOP("Raw Porkchops"), BROWN_MUSHROOM("Brown Mushrooms"),
		GOLD_SWORD("Gold Sword"), BONE_MEAL("Bone Meal"), BRICK("Brick"), GLOWSTONE_DUST("Glowstone Dust"),
		NOTHING("Nothing");
		private final String label;
		PigmanDrop(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum ScoreDisplay implements Labelled {
		VANILLA("Vanilla"), COMBINED("Combined"), LISTED("Listed"), CUSTOM("Custom");
		private final String label;
		ScoreDisplay(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum CoordinateDisplay implements Labelled {
		SHOW("Show"), HIDE("Hide"), RANDOMIZE("Randomize");
		private final String label;
		CoordinateDisplay(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum HudPositioning implements Labelled {
		DISABLED("Disabled"), SIMPLE("Simple"), ADVANCED("Advanced");
		private final String label;
		HudPositioning(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum HorizontalPosition implements Labelled {
		LEFT("Left"), CENTERED("Centered"), RIGHT("Right");
		private final String label;
		HorizontalPosition(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum VerticalPosition implements Labelled {
		BOTTOM("Bottom"), CENTERED("Centered"), TOP("Top");
		private final String label;
		VerticalPosition(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum FrontView implements Labelled {
		DISABLED("Disabled"), NORMAL("Normal"), HIDE_HUD("Hide HUD");
		private final String label;
		FrontView(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	/** MouseTweaks' {@code WheelScrollDirection}. */
	public enum WheelDirection implements Labelled {
		NORMAL("Normal"),
		INVERTED("Inverted"),
		POSITION_AWARE("Inventory Position Aware"),
		POSITION_AWARE_INVERTED("Position Aware, Inverted");
		private final String label;
		WheelDirection(String label) { this.label = label; }
		public String label() { return label; }
		/** Scroll away from the other inventory rather than toward it. */
		public boolean isInverted() { return this == INVERTED || this == POSITION_AWARE_INVERTED; }
		/** Flip the direction when the other inventory sits above the hovered slot. */
		public boolean isPositionAware() { return this == POSITION_AWARE || this == POSITION_AWARE_INVERTED; }
		@Override public String toString() { return label; }
	}

	/** MouseTweaks' {@code WheelSearchOrder}. */
	public enum WheelSearchOrder implements Labelled {
		FIRST_TO_LAST("First To Last"), LAST_TO_FIRST("Last To First");
		private final String label;
		WheelSearchOrder(String label) { this.label = label; }
		public String label() { return label; }
		@Override public String toString() { return label; }
	}

	public enum ChestSounds implements Labelled {
		NONE("None", "", ""),
		// UniTweaks also had a "Modern" setting backed by its own sound files. RetroTweaks ships no
		// assets, so that choice is not offered rather than being present and silent.
		DOOR("Door", "random.door_open", "random.door_close");
		private final String label;
		private final String openSound;
		private final String closeSound;
		ChestSounds(String label, String openSound, String closeSound) {
			this.label = label;
			this.openSound = openSound;
			this.closeSound = closeSound;
		}
		public String label() { return label; }
		public String openSound() { return openSound; }
		public String closeSound() { return closeSound; }
		@Override public String toString() { return label; }
	}
}
