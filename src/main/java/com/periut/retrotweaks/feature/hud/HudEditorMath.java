package com.periut.retrotweaks.feature.hud;

/**
 * Pure geometry for the HUD-position editor (drag-to-move elements plus the movable control panel
 * in {@code ConfigScreen}, which carries every control a HUD-element page has - Visible,
 * Horizontal/Vertical Position, the offset readout, Defaults and Done): no
 * Minecraft/LWJGL types anywhere in this file, on purpose, so every formula here can be checked by
 * the {@code main} below instead of trusted on faith - run it with {@code javac} + {@code java -ea}
 * (assertions are off by default in plain {@code java}).
 *
 * <p>{@link #elementBounds} adds an offset to a fixed vanilla rectangle. The rectangle is not
 * reinventing anything - it is {@code InGameHud.render()}'s own {@code drawTexture}/text-draw
 * geometry, copied out as constants (see each case's comment for the exact source line). The offset
 * is never recomputed here: it is read straight from {@link HudLayout}, which is exactly what
 * {@code InGameHudMixin.retrotweaks$render} already worked out for this frame's real HUD draw. This
 * file only ever adds the two together; it does not know what LEFT/CENTERED/RIGHT or
 * TOP/CENTERED/BOTTOM mean and never touches {@code Config}.
 *
 * <p>{@link #panelWidth} and {@link #panelBody} are the same reasoning
 * applied to the panel's own chrome: {@code ConfigScreen} feeds them measured text widths and its
 * own pixel constants (padding, button height, ...) and gets back where everything goes, instead of
 * that arithmetic living inline where only a human reading it carefully could catch a mistake.
 */
public final class HudEditorMath {

	private HudEditorMath() {}

	/** Half the width of the hotbar/hearts/armor/oxygen row either side of screen-centre - the "91"
	 *  baked into every one of {@code InGameHud.render()}'s status-bar draw calls. */
	private static final int HALF_SPAN = 91;

	/**
	 * The overlay message's hit box is a fixed representative width rather than the live message's
	 * real pixel width: the message most of the time reads "" while a config screen is open (see the
	 * port report), so tracking it live would need the mixin to publish text no editor session would
	 * ever actually see. A human should confirm this feels right in game - see the port report.
	 */
	private static final int OVERLAY_WIDTH = 60;

	/** Mirrors {@code ConfigScreen.MIN_PANEL_CONTENT_WIDTH}, duplicated here only so the self-test
	 *  below has a realistic narrowest case to check against without importing anything. */
	private static final int MIN_PANEL_CONTENT_WIDTH_FOR_TEST = 96;

	/**
	 * Arrow-key nudge step sizes - plain and with Shift held - see {@code ConfigScreen.keyPressed}/
	 * {@code nudgeElement}. Kept here rather than as private constants on the screen so the self-test
	 * below can prove the exact values actually wired to the keyboard never carry an offset past
	 * -256/256, instead of a hardcoded step elsewhere quietly drifting out of sync with what is
	 * tested.
	 */
	public static final int NUDGE_STEP = 1;
	public static final int NUDGE_STEP_FAST = 10;

	/** One HUD element's on-screen rectangle, post-offset, in scaled-GUI pixels. */
	public record Rect(int x, int y, int width, int height) {
		public boolean contains(int px, int py) {
			return px >= x && px < x + width && py >= y && py < y + height;
		}
	}

	/** The five things a player can grab. "Simple Position" moves all five together - see
	 *  {@code ConfigScreen}'s {@code dragKindsFor} - rather than being a sixth kind of its own. */
	public enum Kind { HOTBAR, HEARTS, ARMOR, OXYGEN, OVERLAY }

	/**
	 * {@code kind}'s rectangle for a HUD of size {@code scaledWidth x scaledHeight}, offset by
	 * {@code xOffset}/{@code yOffset} - {@link HudLayout}'s own published numbers for that kind, e.g.
	 * {@link HudLayout#xOffsetHotbar}/{@link HudLayout#yOffsetHotbar} for {@link Kind#HOTBAR}.
	 */
	public static Rect elementBounds(Kind kind, int scaledWidth, int scaledHeight, int xOffset, int yOffset) {
		int centerX = scaledWidth / 2;
		return switch (kind) {
			// drawTexture(i / 2 - 91, j - 22, 0, 0, 182, 22) - the hotbar background slab.
			case HOTBAR -> new Rect(centerX - HALF_SPAN + xOffset, scaledHeight - 22 + yOffset, 182, 22);
			// 10 icons, 8px pitch + 9px wide: drawTexture(i / 2 - 91 + q * 8, j - 32, ..., 9, 9), q 0..9.
			case HEARTS -> new Rect(centerX - HALF_SPAN + xOffset, scaledHeight - 32 + yOffset, 81, 9);
			// Mirrored on the right: drawTexture(i / 2 + 91 - q * 8 - 9, j - 32, ..., 9, 9), q 0..9.
			case ARMOR -> new Rect(centerX + 10 + xOffset, scaledHeight - 32 + yOffset, 81, 9);
			// Same span as hearts, one row above: drawTexture(i / 2 - 91 + c * 8, j - 32 - 9, ...).
			case OXYGEN -> new Rect(centerX - HALF_SPAN + xOffset, scaledHeight - 41 + yOffset, 81, 9);
			// glTranslatef(i / 2, j - 48, 0) then draw(msg, -w / 2, -4, ...) - centred on screen-centre.
			case OVERLAY -> new Rect(centerX - OVERLAY_WIDTH / 2 + xOffset, scaledHeight - 52 + yOffset, OVERLAY_WIDTH, 8);
		};
	}

	/** Clamps a panel edge so the panel sits fully on screen whenever it is smaller than the screen,
	 *  and is pinned to 0 (never negative, never past the far edge) when it is not. */
	public static int clampPanelPosition(int position, int panelSize, int screenSize) {
		int max = Math.max(0, screenSize - panelSize);
		return Math.max(0, Math.min(max, position));
	}

	/** Clamps a dragged or nudged offset to the option's own declared range (see {@code
	 *  Config.HudElement}). */
	public static int clampOffset(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	// ---------------------------------------------------------------- panel chrome layout

	/** The title bar's own minimum width: its left padding, the category name, another padding, the
	 *  minimize square, and a final margin past it. */
	public static int panelTitleWidth(int categoryNameWidth, int padding, int minimizeSize) {
		return padding + categoryNameWidth + padding + minimizeSize + padding;
	}

	/** The panel's outer width: wide enough for its content column (every button/readout row, each
	 *  padded the same on both sides) or for the title bar, whichever needs more room. */
	public static int panelWidth(int contentWidth, int padding, int titleWidth) {
		return Math.max(contentWidth + 2 * padding, titleWidth);
	}

	/**
	 * Where every row of the panel's body sits, top to bottom, relative to the panel's own (0, 0) -
	 * i.e. these already include the title bar and its padding, so {@code ConfigScreen} only has to
	 * add {@code HudPanelState.x}/{@code y}. In order: Visible, Horizontal Position, Vertical
	 * Position (each a {@code PANEL_ROW_HEIGHT}-tall row), the offset readout (a text line,
	 * {@code readoutHeight} tall), Defaults, and Done - the same six rows a HUD-element page used to
	 * split between the panel and a left-hand row column, now all in one place. {@code height} is the
	 * panel's total expanded height, padded top and bottom to match.
	 */
	public record PanelBody(int visibleY, int horizontalY, int verticalY, int readoutY, int navRowY, int doneY, int height) {}

	public static PanelBody panelBody(int titleHeight, int padding, int buttonHeight, int buttonGap, int readoutHeight) {
		int y = titleHeight + padding;
		int visibleY = y;
		y += buttonHeight + buttonGap;
		int horizontalY = y;
		y += buttonHeight + buttonGap;
		int verticalY = y;
		y += buttonHeight + buttonGap;
		int readoutY = y;
		y += readoutHeight;
		int navRowY = y;
		y += buttonHeight + buttonGap;
		int doneY = y;
		y += buttonHeight;
		int height = y + padding;
		return new PanelBody(visibleY, horizontalY, verticalY, readoutY, navRowY, doneY, height);
	}

	// ---------------------------------------------------------------- self-test

	public static void main(String[] args) {
		// Hotbar at a representative scaled size, no offset - matches InGameHud.render() exactly.
		Rect hotbar = elementBounds(Kind.HOTBAR, 854, 480, 0, 0);
		assert hotbar.equals(new Rect(854 / 2 - 91, 480 - 22, 182, 22)) : hotbar;
		assert hotbar.contains(854 / 2, 479) : "hotbar should contain a point on its own bottom row";
		assert !hotbar.contains(0, 0) : "hotbar should not contain the screen's top-left corner";

		// Offsets translate the rect exactly, both axes, either sign.
		Rect moved = elementBounds(Kind.HOTBAR, 854, 480, 40, -15);
		assert moved.x() == hotbar.x() + 40 && moved.y() == hotbar.y() - 15 : moved;

		// Hearts/armor mirror each other around centre, same row, same size.
		Rect hearts = elementBounds(Kind.HEARTS, 854, 480, 0, 0);
		Rect armor = elementBounds(Kind.ARMOR, 854, 480, 0, 0);
		assert hearts.y() == armor.y() && hearts.width() == armor.width() && hearts.height() == armor.height();
		assert hearts.x() < 854 / 2 && armor.x() > 854 / 2 : "hearts sit left of centre, armor right of centre";

		// Oxygen sits directly above hearts, same horizontal span.
		Rect oxygen = elementBounds(Kind.OXYGEN, 854, 480, 0, 0);
		assert oxygen.x() == hearts.x() && oxygen.width() == hearts.width();
		assert oxygen.y() == hearts.y() - 9 : "oxygen row should sit directly above the hearts row";

		// ScreenScaler's smallest legal width still produces a sane, non-negative rect.
		Rect smallHotbar = elementBounds(Kind.HOTBAR, 320, 240, 0, 0);
		assert smallHotbar.x() >= 0 : smallHotbar;

		// Offset clamp holds exactly at, and never past, -256/256.
		assert clampOffset(300, -256, 256) == 256;
		assert clampOffset(-300, -256, 256) == -256;
		assert clampOffset(0, -256, 256) == 0;
		assert clampOffset(256, -256, 256) == 256;
		assert clampOffset(-256, -256, 256) == -256;

		// Arrow-key nudging (plain and Shift-fast) from just inside an edge lands exactly on it, and
		// from already-at or past an edge never steps further past it, either direction.
		assert clampOffset(255 + NUDGE_STEP, -256, 256) == 256 : "one plain nudge from just inside the max should land exactly on it";
		assert clampOffset(-255 - NUDGE_STEP, -256, 256) == -256 : "one plain nudge from just inside the min should land exactly on it";
		assert clampOffset(256 + NUDGE_STEP, -256, 256) == 256 : "plain nudge must not step past the max";
		assert clampOffset(-256 - NUDGE_STEP, -256, 256) == -256 : "plain nudge must not step past the min";
		assert clampOffset(256 + NUDGE_STEP_FAST, -256, 256) == 256 : "fast nudge must not step past the max";
		assert clampOffset(-256 - NUDGE_STEP_FAST, -256, 256) == -256 : "fast nudge must not step past the min";
		assert clampOffset(250 + NUDGE_STEP_FAST, -256, 256) == 256 : "a fast nudge that would overshoot the max should clamp to it";
		assert clampOffset(-250 - NUDGE_STEP_FAST, -256, 256) == -256 : "a fast nudge that would overshoot the min should clamp to it";

		// The panel can never be dragged fully off screen, either direction, at any panel/screen size.
		assert clampPanelPosition(-500, 120, 854) == 0;
		assert clampPanelPosition(9000, 120, 854) == 854 - 120;
		assert clampPanelPosition(400, 120, 854) == 400;
		// Panel wider than the screen: pinned to 0, never left dangling at a negative position.
		assert clampPanelPosition(-40, 900, 320) == 0;
		assert clampPanelPosition(40, 900, 320) == 0;

		// panelWidth: the content column (buttons/readout, padded both sides) or the title bar,
		// whichever needs more room, wins.
		assert panelWidth(60, 4, 50) == 68 : "content column (60 + 2*4 padding) should beat a narrower title";
		assert panelWidth(20, 4, 120) == 120 : "a long category name should beat a narrower content column";

		// panelTitleWidth: left padding, the name, another padding, the minimize square, one more margin.
		assert panelTitleWidth(40, 4, 12) == 4 + 40 + 4 + 12 + 4;

		// panelBody at ConfigScreen's real pixel constants (title 14, padding 4, button 20, gap 2,
		// readout 10): every row strictly below the last, Done doesn't clip the panel's own bottom
		// edge, and the numbers are exactly what the six rows plus top/bottom padding add up to.
		PanelBody body = panelBody(14, 4, 20, 2, 10);
		assert body.visibleY() == 18 : body;
		assert body.horizontalY() == 40 : body;
		assert body.verticalY() == 62 : body;
		assert body.readoutY() == 84 : body;
		assert body.navRowY() == 94 : body;
		assert body.doneY() == 116 : body;
		assert body.height() == 140 : body;
		assert body.visibleY() < body.horizontalY() && body.horizontalY() < body.verticalY()
			&& body.verticalY() < body.readoutY() && body.readoutY() < body.navRowY()
			&& body.navRowY() < body.doneY() : body;
		assert body.doneY() + 20 <= body.height() : "Done should not clip the panel's own bottom edge";

		// Degenerate sizes (zero padding/gap, 1px rows) still keep every row in order and inside height.
		PanelBody tinyBody = panelBody(1, 0, 1, 0, 1);
		assert tinyBody.visibleY() <= tinyBody.horizontalY() && tinyBody.horizontalY() <= tinyBody.verticalY()
			&& tinyBody.verticalY() <= tinyBody.readoutY() && tinyBody.readoutY() <= tinyBody.navRowY()
			&& tinyBody.navRowY() <= tinyBody.doneY() && tinyBody.doneY() < tinyBody.height() : tinyBody;

		// Whole-panel sizing: the content column fits inside the panel at a representative width and at
		// ConfigScreen's own narrowest floor (MIN_PANEL_CONTENT_WIDTH), and the fully expanded panel -
		// title bar included - is short enough that ScreenScaler's smallest legal scaledHeight (240)
		// can still clamp it fully on screen.
		for (int buttonWidth : new int[] {MIN_PANEL_CONTENT_WIDTH_FOR_TEST, 150}) {
			int titleWidth = panelTitleWidth(60, 4, 12);
			int panelW = panelWidth(buttonWidth, 4, titleWidth);
			PanelBody typicalBody = panelBody(14, 4, 12, 2, 10);

			// The content column (Visible/Horizontal/Vertical Position, Done, ...) fits between the
			// panel's own left/right padding.
			assert 4 + buttonWidth + 4 <= panelW : "buttonWidth=" + buttonWidth + " should fit inside panelWidth=" + panelW;

			// The title bar's own minimize square never clips off either edge of panelW.
			int minimizeX = panelW - 4 - 12;
			assert minimizeX >= 0 && minimizeX + 12 <= panelW : "minimize square should fit inside panelWidth=" + panelW;

			// Short enough that the smallest legal scaledHeight (240) can still clamp the whole panel
			// fully on screen.
			assert typicalBody.height() <= 240 : "panel should fit the smallest legal scaledHeight; was " + typicalBody.height();
			assert clampPanelPosition(0, typicalBody.height(), 240) + typicalBody.height() <= 240;
		}

		// The title bar specifically: dragged far off either edge (or never moved from (0,0)), the
		// clamp still leaves its own full-width top strip entirely on screen at the smallest legal
		// scaledWidth/Height, i.e. it can never itself end up clamped off screen even though the
		// panel body below it can be much taller.
		int panelHeightAtFloor = panelBody(14, 4, 20, 2, 10).height();
		int panelWidthAtFloor = panelWidth(150, 4, panelTitleWidth(60, 4, 12));
		for (int attempt : new int[] {-9000, 0, 9000}) {
			int clampedX = clampPanelPosition(attempt, panelWidthAtFloor, 320);
			int clampedY = clampPanelPosition(attempt, panelHeightAtFloor, 240);
			assert clampedX >= 0 && clampedX + panelWidthAtFloor <= 320 : "title bar should stay on screen horizontally; clampedX=" + clampedX;
			assert clampedY >= 0 && clampedY + 14 <= 240 : "title bar should stay on screen vertically; clampedY=" + clampedY;
		}

		System.out.println("HudEditorMath self-test OK");
	}
}
