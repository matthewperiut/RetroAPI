package com.periut.retrotweaks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenScaler;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.List;

/**
 * The scrolling list every RetroTweaks screen is built on.
 *
 * <p>The screens this replaces paged: b1.7.3's own {@code EntryListWidget} draws rows itself and has
 * no notion of interactive widgets inside them, so the previous port kept every row a real 20px
 * {@code ButtonWidget} and stepped through them a page at a time. That capped a window at a dozen or
 * so options and made "where was that setting" a hunt through pages. Rows here are drawn directly
 * instead of being widgets, which makes them cost 12px each rather than 21, lets the whole list scroll
 * under the wheel, and lets a row carry a checkbox and a value at once.
 *
 * <p>Layout, top to bottom: a title strip, the scrolling row viewport, a chin showing the hovered
 * row's description, and a footer of buttons. Only the viewport scrolls; the chin and the footer are
 * fixed, which is the point of them - a description that scrolled away with the row it describes would
 * be no use. In a world the whole thing is pinned to the left edge and drawn on opaque black, so the
 * live world (or the HUD being positioned) stays visible beside it at any window size; with no world
 * loaded there is nothing to keep clear of, so it centres on the dirt background instead - see
 * {@link #panelCentered()} and {@link #renderBackdrop()}.
 *
 * <p>Clipping the viewport is done by painting the header and chin bands back over the rows after
 * they are drawn, not with {@code glScissor}: the panel is opaque black by design, so black over black
 * cuts a row off exactly where the viewport ends and costs two more quads instead of a scissor
 * rectangle that has to be computed in framebuffer pixels (i.e. undone and redone for every GUI
 * scale). Every quad here goes through {@link net.minecraft.client.gui.DrawContext#fill}, which is
 * {@code Tessellator.INSTANCE}, so nothing in this file talks to GL directly.
 */
@Environment(EnvType.CLIENT)
public abstract class ListScreen extends Screen {

	// ---------------------------------------------------------------- palette

	/** Opaque on purpose - the clipping trick above depends on it. */
	public static final int PANEL_BG = 0xFF000000;
	public static final int PANEL_EDGE = 0xFF4A4A4A;
	public static final int SEPARATOR = 0xFF2E2E2E;
	public static final int ROW_HOVER_BG = 0xFF1C1C1C;
	public static final int TEXT = 0xFFFFFFFF;
	/** Vanilla's own highlight yellow, so a hovered row reads the same as a hovered vanilla button. */
	public static final int TEXT_HOVER = 0xFFFFFF55;
	public static final int TEXT_VALUE = 0xFFA8A8A8;
	public static final int TEXT_DIM = 0xFF8A8A8A;
	public static final int TEXT_OFF = 0xFF585858;
	/** For anything whose consequences reach past this client - see {@link #footnote()}. */
	public static final int TEXT_WARN = 0xFFFF5555;
	/** What {@link Row#serverEdit()} draws, and what a page's {@link #footnote()} explains. */
	public static final String SERVER_EDIT_MARK = "*";
	public static final int BOX_BG = 0xFF0A0A0A;
	public static final int BOX_BORDER = 0xFF787878;
	/** Behind selected text in a {@link TextInput}. Dark enough to keep white text readable on top. */
	public static final int SELECTION = 0xFF2F5AA0;
	public static final int BOX_BORDER_OFF = 0xFF3A3A3A;
	public static final int SCROLL_TRACK = 0xFF141414;
	public static final int SCROLL_THUMB = 0xFF5A5A5A;
	public static final int SCROLL_THUMB_HOVER = 0xFF9A9A9A;
	public static final int BUTTON_BG = 0xFF141414;
	public static final int BUTTON_BG_HOVER = 0xFF2A2A2A;

	// ---------------------------------------------------------------- metrics

	public static final int PAD = 6;
	/** Tight on purpose: 8px of text plus two clear pixels either side. */
	public static final int ROW_HEIGHT = 12;
	/** Extra height a row takes when it has a second line - see {@link Row#subLabel()}. */
	public static final int SUBLINE_HEIGHT = 10;
	public static final int CHECKBOX_SIZE = 8;
	private static final int HEADER_HEIGHT = 20;
	private static final int FOOTER_HEIGHT = 16;
	private static final int CHIN_LINES = 2;
	private static final int CHIN_HEIGHT = 4 + CHIN_LINES * 9 + 2;
	/** One 8px line plus a clear pixel either side - what {@link #footnote()} costs a page that has one. */
	private static final int FOOTNOTE_HEIGHT = 10;
	private static final int HEADER_BUTTON_HEIGHT = 12;
	/** Ticks a marquee holds still at each end before setting off again - see {@link #marqueeOffset}. */
	private static final int MARQUEE_PAUSE_TICKS = 30;
	private static final float MARQUEE_PIXELS_PER_TICK = 0.5F;
	private static final int SCROLLBAR_WIDTH = 3;
	/** Horizontal room the scrollbar takes out of the row area when one is showing. */
	private static final int SCROLLBAR_SPACE = 8;
	private static final int ROWS_PER_WHEEL_NOTCH = 3;

	// ---------------------------------------------------------------- state

	protected final List<Row> rows = new ArrayList<>();
	private final List<FooterButton> footerButtons = new ArrayList<>();
	/** Each row's top, relative to the first row, plus one final entry holding the total height. Rows
	 *  are not all the same height (see {@link Row#subLabel()}), so index-times-pitch will not do. */
	private int[] rowTops = new int[]{0};

	protected int panelWidth;
	/** Screen-space x of the panel's left edge: 0 in a world, centred otherwise - see
	 *  {@link #panelCentered()}. Everything the panel draws and hit-tests is measured from here. */
	protected int panelLeft;
	protected int listTop;
	protected int listBottom;
	private int chinTop;
	private int footerTop;
	/** Baseline of {@link #footnote()}'s line, or -1 when this page has none. */
	private int footnoteY = -1;
	/** The header button's box, or null. Recomputed in {@link #layout()} because the panel can resize. */
	private HeaderButton header;

	private float scroll;
	private boolean scrollbarDragging;
	private Popup popup;
	/** Whole ticks since this screen opened; {@link #render} adds the partial tick on top, so a marquee
	 *  moves smoothly rather than in 20Hz steps. */
	private int ticks;
	private float now;

	// ---------------------------------------------------------------- subclass contract

	/** Drawn in the title strip. Trimmed to the panel, so a long breadcrumb is safe. */
	protected abstract String title();

	/** Fills {@code out} with this screen's rows, in order. Called on every (re)build. */
	protected abstract void buildRows(List<Row> out);

	/** Fills {@code out} with the footer buttons, left to right. */
	protected void buildFooter(List<FooterButton> out) {}

	/**
	 * A single line of warning text between the chin and the footer buttons, or null for none.
	 *
	 * <p>The panel grows by exactly one line to hold it, so a page that has something to say does not
	 * cost every other page the room. Drawn in {@link #TEXT_WARN}, because the only thing worth a
	 * permanent line here is a consequence that reaches past this client - what the red asterisk on a
	 * row means.
	 */
	protected String footnote() {
		return null;
	}

	/**
	 * A button in the title strip, right-aligned, or null for none. One per page; the title trims
	 * itself around it.
	 */
	protected HeaderButton headerButton() {
		return null;
	}

	/**
	 * False turns the whole panel off - no rows, no chin, no footer, and no input handling - leaving
	 * the screen as a bare {@link Screen} with whatever widgets the subclass added itself. The HUD
	 * element editor uses this: while you are dragging a HUD element around, a list covering the left
	 * third of the screen is in the way of the thing being positioned.
	 */
	protected boolean listVisible() {
		return true;
	}

	/**
	 * What goes behind the panel: in a world, nothing at all; outside one, the dirt texture every other
	 * out-of-world screen uses.
	 *
	 * <p>Vanilla screens open with {@code renderBackground()}, which lays a dark gradient (or the dirt
	 * texture) over the entire window. In a world that exists because a vanilla screen's controls are
	 * spread across the middle of the screen with nothing behind them, so the view has to be dimmed for
	 * them to be legible. This layout does not have that problem: everything lives on an opaque black
	 * panel down the left edge, which is already as readable as it can get. Dimming the other two thirds
	 * bought nothing and cost the view of the world - the very thing you are usually changing settings
	 * to look at.
	 *
	 * <p>Out of a world the opposite holds. Nothing repaints the area the panel does not cover, so
	 * whatever was on screen when this one opened - the options screen this was reached from, title
	 * screen and all - stays there frame after frame beside a black column. So the dirt texture goes
	 * down over the whole window first, which both wipes that and makes this read as the same kind of
	 * screen as the one it was opened from.
	 */
	protected void renderBackdrop() {
		if (this.minecraft != null && this.minecraft.world == null) this.renderBackgroundTexture(0);
	}

	/**
	 * Whether the panel is centred rather than pinned to the left edge. True exactly when there is no
	 * world loaded.
	 *
	 * <p>Left-bound is a concession to the world behind it: the panel gets out of the way of the terrain
	 * or the HUD you are looking at while you change a setting. With no world there is nothing behind it
	 * to get out of the way of - only the dirt background - and a column jammed against one edge of an
	 * otherwise empty screen is just off-centre, which is not how any other menu in the game is laid out.
	 */
	protected boolean panelCentered() {
		return this.minecraft == null || this.minecraft.world == null;
	}

	/** Ceiling on the panel's width, so a page that has something behind it can keep clear of it. */
	protected int maxPanelWidth() {
		return 280;
	}

	/** Drawn after the backdrop but before the panel and any vanilla widgets. */
	protected void renderBeforeWidgets(int mouseX, int mouseY, float delta) {}

	/** Drawn after the panel and any vanilla widgets, but under an open popup. */
	protected void renderAfterWidgets(int mouseX, int mouseY, float delta) {}

	// ---------------------------------------------------------------- lifecycle

	@Override
	public void init() {
		// Held keys repeat: without this, deleting a resource URL in a TextPopup is one keypress per
		// character, and nudging a HUD element with an arrow key is one pixel per press. Off again in
		// removed(), matching the convention the multiplayer screens next door already use.
		Keyboard.enableRepeatEvents(true);
		rebuildRows();
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
	}

	/**
	 * Rebuilds the rows and the footer in place, keeping the scroll position. Safe to call from a row's
	 * own click handler - unlike the vanilla button list, nothing is iterating this while it happens.
	 */
	public void rebuildRows() {
		rows.clear();
		footerButtons.clear();
		if (listVisible()) {
			buildRows(rows);
			buildFooter(footerButtons);
		}
		layout();
	}

	private void layout() {
		rowTops = new int[rows.size() + 1];
		int top = 0;
		for (int i = 0; i < rows.size(); i++) {
			rowTops[i] = top;
			top += rowHeight(rows.get(i));
		}
		rowTops[rows.size()] = top;

		int desired = Math.max(170, this.width * 2 / 5);
		panelWidth = Math.max(80, Math.min(Math.min(desired, maxPanelWidth()), this.width));
		panelLeft = panelCentered() ? Math.max(0, (this.width - panelWidth) / 2) : 0;

		footerTop = this.height - 4 - FOOTER_HEIGHT;
		String note = footnote();
		footnoteY = note == null || note.isEmpty() ? -1 : footerTop - FOOTNOTE_HEIGHT;
		chinTop = (footnoteY < 0 ? footerTop : footnoteY) - 3 - CHIN_HEIGHT;
		listTop = HEADER_HEIGHT;
		listBottom = Math.max(listTop + ROW_HEIGHT, chinTop);

		header = headerButton();
		if (header != null) {
			header.width = Math.min(this.textRenderer.getWidth(header.text) + 12, panelWidth / 2);
			header.height = HEADER_BUTTON_HEIGHT;
			header.x = panelRight() - PAD - header.width;
			header.y = 4;
		}

		int count = footerButtons.size();
		if (count > 0) {
			int span = panelWidth - PAD * 2;
			int gap = 4;
			int each = (span - gap * (count - 1)) / count;
			for (int i = 0; i < count; i++) {
				FooterButton button = footerButtons.get(i);
				button.x = panelLeft + PAD + i * (each + gap);
				button.y = footerTop;
				button.width = i == count - 1 ? panelRight() - PAD - button.x : each;
				button.height = FOOTER_HEIGHT;
			}
		}
		clampScroll();
	}

	private static int rowHeight(Row row) {
		return row.subLabel() == null ? ROW_HEIGHT : ROW_HEIGHT + SUBLINE_HEIGHT;
	}

	private int viewportHeight() {
		return listBottom - listTop;
	}

	private int contentHeight() {
		return rowTops[rowTops.length - 1];
	}

	private float maxScroll() {
		return Math.max(0, contentHeight() - viewportHeight());
	}

	private void clampScroll() {
		scroll = Math.max(0.0F, Math.min(maxScroll(), scroll));
	}

	private boolean scrollbarVisible() {
		return maxScroll() > 0;
	}

	/** Screen-space x just past the panel's right edge. */
	public int panelRight() {
		return panelLeft + panelWidth;
	}

	/** Screen-space x a row's contents stop at, short of the scrollbar when one is showing. */
	private int rowRight() {
		return panelRight() - PAD - (scrollbarVisible() ? SCROLLBAR_SPACE : 0);
	}

	// ---------------------------------------------------------------- popups

	/** Opens {@code popup}, replacing any that is already up. Null closes. */
	public void openPopup(Popup popup) {
		this.popup = popup;
		if (popup != null) popup.layout(this);
	}

	public void closePopup() {
		this.popup = null;
	}

	public boolean hasPopup() {
		return popup != null;
	}

	// ---------------------------------------------------------------- rendering

	@Override
	public void render(int mouseX, int mouseY, float delta) {
		now = ticks + delta;
		syncToDisplaySize();
		renderBackdrop();
		renderBeforeWidgets(mouseX, mouseY, delta);
		super.render(mouseX, mouseY, delta);
		// The panel steps aside entirely while a popup is up. A popup is always about ONE setting, and
		// most of them - the render scale above all - are judged by looking at the world, not at the
		// list. Leaving a black column down a third of the screen while you drag a scale slider hides
		// the only thing you are trying to see. It comes straight back when the popup closes, which is
		// Escape, Done, or a click outside it.
		if (listVisible() && popup == null) {
			layout();
			renderPanel(mouseX, mouseY);
		}
		renderAfterWidgets(mouseX, mouseY, delta);
		if (popup != null) {
			popup.layout(this);
			popup.render(this, mouseX, mouseY);
		}
	}

	/**
	 * Re-lays the screen out when the scaled size has changed underneath it.
	 *
	 * <p>{@code Screen.init(minecraft, width, height)} is the only thing that updates {@link #width}/
	 * {@link #height}, and b1.7.3 only calls it from {@code Minecraft.resize}, which only runs when
	 * {@code Display.wasResized()} says the WINDOW changed. A mod that changes the framebuffer without
	 * resizing the window - RetroDragon's render scale does exactly that - moves what
	 * {@code ScreenScaler} reports while leaving this screen laid out for the old size, and everything
	 * anchored to the bottom (the chin, Done, Defaults) ends up below the visible area.
	 *
	 * <p>Not to be confused with the same symptom on the VANILLA video settings screen, which had a
	 * different cause entirely - see {@code VideoOptionsScreenRetroDragonMixin}. RGSS and Mipmaps cannot
	 * trigger this one: RetroDragon's {@code setRgss} is a single volatile store and {@code setMipmap}
	 * at most re-uploads the terrain atlas; neither touches the display size.
	 *
	 * <p>So the size is taken from the same {@code ScreenScaler} the game itself would build, every
	 * frame, and any disagreement re-inits. It converges after one frame because it is computed from
	 * the same inputs {@code Minecraft.resize} uses.
	 */
	private void syncToDisplaySize() {
		if (this.minecraft == null || this.minecraft.options == null) return;
		ScreenScaler scaler = new ScreenScaler(this.minecraft.options, this.minecraft.displayWidth, this.minecraft.displayHeight);
		int scaledWidth = scaler.getScaledWidth();
		int scaledHeight = scaler.getScaledHeight();
		if (scaledWidth == this.width && scaledHeight == this.height) return;
		if (scaledWidth <= 0 || scaledHeight <= 0) return;
		init(this.minecraft, scaledWidth, scaledHeight);
	}

	/** Only reached with no popup open - see {@link #render}. */
	private void renderPanel(int mouseX, int mouseY) {
		int hovered = rowAt(mouseX, mouseY);
		int left = panelLeft;
		int panelRight = panelRight();

		this.fill(left, 0, panelRight, this.height, PANEL_BG);

		// Rows, including the ones that straddle the viewport edges - they are painted over below.
		int right = rowRight();
		for (int i = 0; i < rows.size(); i++) {
			int y = listTop - Math.round(scroll) + rowTops[i];
			if (y + rowHeight(rows.get(i)) <= listTop || y >= listBottom) continue;
			drawRow(rows.get(i), y, right, i == hovered);
		}

		// Cut the viewport off at both ends. See this class's doc for why this rather than a scissor.
		this.fill(left, 0, panelRight, listTop, PANEL_BG);
		this.fill(left, listBottom, panelRight, this.height, PANEL_BG);

		// The title is a breadcrumb, so it is the line most likely to outgrow the panel; it scrolls for
		// the same reason a row does. Drawn after the top mask so its own clip strips land on black.
		int titleRight = header == null ? panelRight - PAD : header.x - 4;
		drawScrolling(title(), left + PAD, 7, titleRight, TEXT, PANEL_BG);
		if (header != null) {
			drawButton(header.x, header.y, header.width, header.height, header.text, header.enabled,
				header.contains(mouseX, mouseY));
		}
		this.fill(left, listTop - 1, panelRight, listTop, SEPARATOR);

		if (scrollbarVisible()) drawScrollbar(mouseX, mouseY);

		this.fill(left, chinTop, panelRight, chinTop + 1, SEPARATOR);
		drawChin(hovered);

		if (footnoteY >= 0) {
			this.drawTextWithShadow(this.textRenderer, trim(footnote(), panelWidth - PAD * 2),
				left + PAD, footnoteY, TEXT_WARN);
		}

		for (FooterButton button : footerButtons) {
			drawButton(button.x, button.y, button.width, button.height, button.text, button.enabled,
				button.contains(mouseX, mouseY));
		}

		this.fill(panelRight - 1, 0, panelRight, this.height, PANEL_EDGE);
		// Only there to close the box when the panel is centred; left-bound, this column is off screen.
		if (left > 0) this.fill(left, 0, left + 1, this.height, PANEL_EDGE);
	}

	private void drawRow(Row row, int y, int right, boolean hovered) {
		boolean enabled = row.enabled();
		int color = !enabled ? TEXT_OFF : hovered ? TEXT_HOVER : TEXT;
		int background = hovered ? ROW_HOVER_BG : PANEL_BG;
		if (hovered) this.fill(panelLeft + 1, y, panelRight() - 1, y + rowHeight(row), ROW_HOVER_BG);

		int textX = panelLeft + (row.checkbox() ? PAD + CHECKBOX_SIZE + 4 : PAD);
		String marker = row.value();
		int markerWidth = marker == null ? 0 : this.textRenderer.getWidth(marker) + 6;
		// The asterisk sits at the far right, in the column a chevron would use - which no row that can
		// carry one is using, since a subsection is not a setting and cannot be a server's.
		int starWidth = row.serverEdit() ? this.textRenderer.getWidth(SERVER_EDIT_MARK) + 4 : 0;

		// The label goes down FIRST, because a scrolling one is clipped by repainting the strips either
		// side of it - and the checkbox and marker live in exactly those strips.
		drawScrolling(row.label(), textX, y + 2, right - markerWidth - starWidth, color, background);

		if (row.checkbox()) drawCheckbox(panelLeft + PAD, y + 2, row.checked(), enabled, hovered, color);
		if (marker != null) {
			this.drawTextWithShadow(this.textRenderer, marker, right - starWidth - this.textRenderer.getWidth(marker),
				y + 2, !enabled ? TEXT_OFF : hovered ? TEXT_HOVER : TEXT_VALUE);
		}
		if (starWidth > 0) {
			this.drawTextWithShadow(this.textRenderer, SERVER_EDIT_MARK,
				right - this.textRenderer.getWidth(SERVER_EDIT_MARK), y + 2, TEXT_WARN);
		}

		String sub = row.subLabel();
		if (sub != null) {
			// Indented under the label, never under the checkbox, so the two lines read as one entry.
			drawScrolling(sub, textX, y + ROW_HEIGHT, right, TEXT_DIM, background);
		}
	}

	/**
	 * Draws one line of text, scrolling it back and forth when it does not fit.
	 *
	 * <p>Cutting a name off with ".." saves the layout and loses the information - "Achievements Done
	 * Ret.." and "Achievements Done Ren.." are different options that read the same. So an overflowing
	 * line slides left until its end is visible, holds there, slides back, and holds again; the pauses
	 * are what make it readable rather than a permanently moving thing the eye cannot settle on.
	 *
	 * <p>Clipping is the same trick the viewport uses: the strips either side are repainted in the row's
	 * own background colour afterwards. That is why everything sharing the line - the checkbox, the
	 * chevron - has to be drawn after this rather than before.
	 */
	private void drawScrolling(String text, int x, int textY, int right, int color, int background) {
		int available = right - x;
		if (available <= 0) return;
		int width = this.textRenderer.getWidth(text);
		if (width <= available) {
			this.drawTextWithShadow(this.textRenderer, text, x, textY, color);
			return;
		}

		// Only the slice that can land INSIDE the panel is drawn. Repainting the strips either side is
		// what clips this, and a repaint must stop at the panel's own right edge or it would be painting
		// over the world - so drawing the whole string offset leftwards would leave everything past that
		// edge on screen with nothing able to cover it.
		int offset = marqueeOffset(width - available);
		int start = 0;
		int skipped = 0;
		while (start < text.length()) {
			int charWidth = this.textRenderer.getWidth(String.valueOf(text.charAt(start)));
			if (skipped + charWidth > offset) break;
			skipped += charWidth;
			start++;
		}
		// Whatever of the first visible character is already off the left edge; drawing from a sub-glyph
		// position is what keeps the motion smooth instead of jumping a character at a time.
		int drawX = x - (offset - skipped);
		// The panel's edge column is repainted after every row, so a shadow landing on it is covered.
		int limit = panelRight() - 1 - drawX;
		int end = start;
		int drawn = 0;
		while (end < text.length()) {
			int charWidth = this.textRenderer.getWidth(String.valueOf(text.charAt(end)));
			if (drawn + charWidth > limit) break;
			drawn += charWidth;
			end++;
		}

		this.drawTextWithShadow(this.textRenderer, text.substring(start, end), drawX, textY, color);
		this.fill(panelLeft, textY - 2, x, textY + 10, background);
		this.fill(right, textY - 2, panelRight() - 1, textY + 10, background);
	}

	/**
	 * How far left an overflowing line has slid this frame: still at 0, sliding out, still at
	 * {@code overflow}, sliding back. One shared clock drives every marquee on screen, so two long rows
	 * move together instead of drifting into a shimmer.
	 */
	private int marqueeOffset(int overflow) {
		float travelTicks = overflow / MARQUEE_PIXELS_PER_TICK;
		float cycle = (MARQUEE_PAUSE_TICKS + travelTicks) * 2.0F;
		float phase = now % cycle;
		if (phase < MARQUEE_PAUSE_TICKS) return 0;
		phase -= MARQUEE_PAUSE_TICKS;
		if (phase < travelTicks) return clampOffset(Math.round(phase * MARQUEE_PIXELS_PER_TICK), overflow);
		phase -= travelTicks;
		if (phase < MARQUEE_PAUSE_TICKS) return overflow;
		phase -= MARQUEE_PAUSE_TICKS;
		return clampOffset(overflow - Math.round(phase * MARQUEE_PIXELS_PER_TICK), overflow);
	}

	private static int clampOffset(int offset, int overflow) {
		return Math.max(0, Math.min(overflow, offset));
	}

	private void drawChin(int hovered) {
		String hint = hovered >= 0 ? rows.get(hovered).hint() : "";
		if (hint == null || hint.isEmpty()) return;
		List<String> lines = wrap(hint, panelWidth - PAD * 2, CHIN_LINES);
		for (int i = 0; i < lines.size(); i++) {
			this.drawTextWithShadow(this.textRenderer, trim(lines.get(i), panelWidth - PAD * 2),
				panelLeft + PAD, chinTop + 5 + i * 9, TEXT_DIM);
		}
	}

	private void drawScrollbar(int mouseX, int mouseY) {
		int x = panelRight() - PAD + 1;
		int view = viewportHeight();
		this.fill(x, listTop, x + SCROLLBAR_WIDTH, listBottom, SCROLL_TRACK);
		int thumbHeight = Math.max(10, view * view / Math.max(1, contentHeight()));
		int thumbY = listTop + Math.round((view - thumbHeight) * (scroll / maxScroll()));
		boolean hot = scrollbarDragging
			|| (mouseX >= x - 2 && mouseX < x + SCROLLBAR_WIDTH + 2 && mouseY >= listTop && mouseY < listBottom);
		this.fill(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, hot ? SCROLL_THUMB_HOVER : SCROLL_THUMB);
	}

	// ---------------------------------------------------------------- shared drawing

	void drawCheckbox(int x, int y, boolean checked, boolean enabled, boolean hovered, int markColor) {
		this.fill(x, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, BOX_BG);
		int border = !enabled ? BOX_BORDER_OFF : hovered ? TEXT_HOVER : BOX_BORDER;
		outline(x, y, CHECKBOX_SIZE, CHECKBOX_SIZE, border);
		if (checked) this.fill(x + 2, y + 2, x + CHECKBOX_SIZE - 2, y + CHECKBOX_SIZE - 2, enabled ? markColor : TEXT_OFF);
	}

	void outline(int x, int y, int width, int height, int color) {
		this.fill(x, y, x + width, y + 1, color);
		this.fill(x, y + height - 1, x + width, y + height, color);
		this.fill(x, y, x + 1, y + height, color);
		this.fill(x + width - 1, y, x + width, y + height, color);
	}

	void drawButton(int x, int y, int width, int height, String text, boolean enabled, boolean hovered) {
		boolean lit = enabled && hovered;
		this.fill(x, y, x + width, y + height, lit ? BUTTON_BG_HOVER : BUTTON_BG);
		outline(x, y, width, height, !enabled ? BOX_BORDER_OFF : lit ? TEXT_HOVER : BOX_BORDER);
		int color = !enabled ? TEXT_OFF : lit ? TEXT_HOVER : TEXT;
		this.drawCenteredTextWithShadow(this.textRenderer, trim(text, width - 6), x + width / 2, y + (height - 8) / 2, color);
	}

	/**
	 * The click every vanilla button makes when it is pressed.
	 *
	 * <p>Vanilla does not play this from the button: {@code Screen.mouseClicked} plays it itself once a
	 * {@code ButtonWidget} has claimed the press. Nothing on these screens is a {@code ButtonWidget} -
	 * rows, footer buttons and everything in a popup are drawn, not widgets - so no press here went
	 * through the code that makes the sound, and the one mod screen you spend the most time clicking
	 * around in was the one screen that was silent. Every control that actually does something calls
	 * this; grabbing a scrollbar or dismissing a popup by clicking outside it does not, matching the
	 * vanilla rule that the sound means "a control was pressed".
	 */
	public void playClick() {
		if (this.minecraft != null && this.minecraft.soundManager != null) {
			this.minecraft.soundManager.playSound("random.click", 1.0F, 1.0F);
		}
	}

	// Popups live in this package and draw through these rather than reaching for GL themselves;
	// DrawContext's own primitives are protected, so this is where they are handed out.

	void gFill(int x1, int y1, int x2, int y2, int color) {
		this.fill(x1, y1, x2, y2, color);
	}

	void gText(String text, int x, int y, int color) {
		this.drawTextWithShadow(this.textRenderer, text, x, y, color);
	}

	void gTextCentered(String text, int centerX, int y, int color) {
		this.drawCenteredTextWithShadow(this.textRenderer, text, centerX, y, color);
	}

	int gWidth(String text) {
		return this.textRenderer.getWidth(text);
	}

	TextRenderer font() {
		return this.textRenderer;
	}

	int screenWidth() {
		return this.width;
	}

	int screenHeight() {
		return this.height;
	}

	// ---------------------------------------------------------------- text helpers

	/** {@code text} cut to {@code maxWidth} with ".." on the end, or "" if not even that fits. */
	public String trim(String text, int maxWidth) {
		if (text == null || text.isEmpty() || maxWidth <= 0) return "";
		if (this.textRenderer.getWidth(text) <= maxWidth) return text;
		int room = maxWidth - this.textRenderer.getWidth("..");
		if (room <= 0) return "";
		StringBuilder out = new StringBuilder();
		int used = 0;
		for (int i = 0; i < text.length(); i++) {
			int charWidth = this.textRenderer.getWidth(String.valueOf(text.charAt(i)));
			if (used + charWidth > room) break;
			out.append(text.charAt(i));
			used += charWidth;
		}
		return out + "..";
	}

	/** Greedy word wrap, at most {@code maxLines}; anything past that is folded into the last line. */
	public List<String> wrap(String text, int maxWidth, int maxLines) {
		List<String> all = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (word.isEmpty()) continue;
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() == 0 || this.textRenderer.getWidth(candidate) <= maxWidth) {
				line.setLength(0);
				line.append(candidate);
			} else {
				all.add(line.toString());
				line.setLength(0);
				line.append(word);
			}
		}
		if (line.length() > 0) all.add(line.toString());
		if (all.size() <= maxLines) return all;

		List<String> out = new ArrayList<>(all.subList(0, maxLines));
		StringBuilder rest = new StringBuilder(out.get(maxLines - 1));
		for (int i = maxLines; i < all.size(); i++) rest.append(' ').append(all.get(i));
		out.set(maxLines - 1, rest.toString());
		return out;
	}

	// ---------------------------------------------------------------- input

	/** Which row is under the cursor, or -1. Rows outside the viewport are never hit. */
	private int rowAt(int mouseX, int mouseY) {
		if (mouseX < panelLeft || mouseX >= rowRight() + PAD || mouseY < listTop || mouseY >= listBottom) return -1;
		int offset = Math.round(mouseY - listTop + scroll);
		if (offset < 0 || offset >= contentHeight()) return -1;
		for (int i = 0; i < rows.size(); i++) {
			if (offset < rowTops[i + 1]) return i;
		}
		return -1;
	}

	/**
	 * b1.7.3 gives a screen the mouse wheel nowhere else: {@code Screen.onMouseEvent} reads the LWJGL
	 * event queue and dispatches only presses and releases, throwing the wheel delta away. Reading it
	 * here, before handing the same event on to vanilla, is the only place it can still be seen.
	 */
	@Override
	public void onMouseEvent() {
		int wheel = Mouse.getEventDWheel();
		if (wheel != 0) {
			// LWJGL 2 reports a notch as +-120, but a trackpad can report less; treat anything smaller
			// than a notch as a whole one so a slow scroll still moves rather than rounding to nothing.
			float notches = wheel / 120.0F;
			if (Math.abs(notches) < 1.0F) notches = Math.signum(wheel);
			mouseScrolled(notches);
		}
		super.onMouseEvent();
	}

	private void mouseScrolled(float notches) {
		if (popup != null) {
			popup.scrolled(notches);
			return;
		}
		if (!listVisible()) return;
		scroll -= notches * ROW_HEIGHT * ROWS_PER_WHEEL_NOTCH;
		clampScroll();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		if (popup != null) {
			popup.mouseClicked(this, mouseX, mouseY, button);
			return;
		}
		if (listVisible() && mouseX >= panelLeft && mouseX < panelRight()) {
			if (clickedPanel(mouseX, mouseY, button)) return;
			// A click anywhere else on the panel is absorbed rather than falling through to whatever
			// is behind it - the panel is opaque, so passing a click through it would be a lie.
			return;
		}
		super.mouseClicked(mouseX, mouseY, button);
	}

	private boolean clickedPanel(int mouseX, int mouseY, int button) {
		if (button == 0) {
			if (header != null && header.enabled && header.contains(mouseX, mouseY)) {
				playClick();
				header.action.run();
				return true;
			}
			for (FooterButton footer : footerButtons) {
				if (footer.enabled && footer.contains(mouseX, mouseY)) {
					playClick();
					footer.action.run();
					return true;
				}
			}
			if (scrollbarVisible() && mouseX >= panelRight() - PAD - 1 && mouseY >= listTop && mouseY < listBottom) {
				scrollbarDragging = true;
				dragScrollbarTo(mouseY);
				return true;
			}
		}
		int index = rowAt(mouseX, mouseY);
		if (index >= 0) {
			Row row = rows.get(index);
			if (row.enabled()) {
				playClick();
				row.click(this, mouseX - panelLeft - PAD, button);
			}
			return true;
		}
		return false;
	}

	private void dragScrollbarTo(int mouseY) {
		int view = viewportHeight();
		int thumbHeight = Math.max(10, view * view / Math.max(1, contentHeight()));
		int travel = view - thumbHeight;
		if (travel <= 0) return;
		scroll = (mouseY - listTop - thumbHeight / 2.0F) / travel * maxScroll();
		clampScroll();
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		if (popup != null) {
			popup.mouseReleased(this, mouseX, mouseY, button);
			return;
		}
		// Vanilla also routes plain mouse MOVEMENT here with button -1, which is what keeps a scrollbar
		// drag following the cursor without a drag event of its own.
		if (scrollbarDragging) {
			if (button == 0) scrollbarDragging = false;
			else dragScrollbarTo(mouseY);
			return;
		}
		super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	protected void keyPressed(char character, int keyCode) {
		if (popup != null) {
			popup.keyPressed(this, character, keyCode);
			return;
		}
		super.keyPressed(character, keyCode);
	}

	@Override
	public void tick() {
		super.tick();
		ticks++;
		if (popup != null) popup.tick();
	}

	// ---------------------------------------------------------------- row and footer models

	/**
	 * One line of the list. Everything a row can be - a checkbox, a value, a chevron into a subsection -
	 * is a few overrides on this rather than a widget class of its own, so a screen only has to describe
	 * its rows and never lays them out.
	 */
	public abstract static class Row {

		/** The white (or yellow, when hovered) text on the left. */
		public abstract String label();

		/**
		 * A short right-aligned marker - in practice the ">" chevron on a row that opens a subsection.
		 * NOT for values: anything with real text in it belongs on {@link #subLabel()}, because a value
		 * column and a label column share one line and the label always loses.
		 */
		public String value() {
			return null;
		}

		/**
		 * A second, dimmer line under the label, or null for a single-line row.
		 *
		 * <p>This is where every value lives - the current enum choice, the number, the string, and the
		 * reason an option cannot be changed at all ("handled by unitweaks"). Putting them under the
		 * label instead of beside it gives both the full width of the panel, which is what stops a long
		 * option name and a long value from squeezing each other into nothing. Booleans have no
		 * subLabel: the checkbox is the value.
		 */
		public String subLabel() {
			return null;
		}

		/**
		 * True to mark this row as one whose value belongs to the server rather than to this client, so
		 * changing it changes the server's settings for everybody. Drawn as a red asterisk at the right
		 * of the row; the page says what it means once, in its {@link ListScreen#footnote()}, rather
		 * than on every row that carries one.
		 */
		public boolean serverEdit() {
			return false;
		}

		/** True to draw a checkbox before the label instead of leaving the space blank. */
		public boolean checkbox() {
			return false;
		}

		public boolean checked() {
			return false;
		}

		/** False greys the row out and makes it unclickable. */
		public boolean enabled() {
			return true;
		}

		/** Shown in the chin while the cursor is on this row. */
		public String hint() {
			return "";
		}

		/** {@code relativeX} is measured from the row's left text edge; {@code button} is the LWJGL button. */
		public void click(ListScreen screen, int relativeX, int button) {}
	}

	/**
	 * A button in the title strip, right-aligned and sized to its own text. One per page - it is the
	 * page's one sideways move, not a second footer.
	 */
	public static final class HeaderButton {
		public final String text;
		public final Runnable action;
		public boolean enabled = true;
		int x;
		int y;
		int width;
		int height;

		public HeaderButton(String text, Runnable action) {
			this.text = text;
			this.action = action;
		}

		boolean contains(int mouseX, int mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}

	/** A fixed button along the bottom of the panel. Laid out automatically, evenly across the width. */
	public static final class FooterButton {
		public final String text;
		public final Runnable action;
		public boolean enabled = true;
		int x;
		int y;
		int width;
		int height;

		public FooterButton(String text, Runnable action) {
			this.text = text;
			this.action = action;
		}

		boolean contains(int mouseX, int mouseY) {
			return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
		}
	}
}
