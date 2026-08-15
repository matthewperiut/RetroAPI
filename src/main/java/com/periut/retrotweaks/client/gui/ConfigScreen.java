package com.periut.retrotweaks.client.gui;

import com.periut.retroapi.config.ConfigNetworking;
import com.periut.retroapi.config.ConfigSync;
import com.periut.retroapi.config.ConfigTree;
import com.periut.retroapi.config.RetroConfig;
import com.periut.retroapi.config.RetroConfigs;
import com.periut.retrotweaks.compat.ApiBridge;
import com.periut.retrotweaks.compat.Mods;
import com.periut.retrotweaks.config.ConfigManager;
import com.periut.retrotweaks.feature.hud.HudEditorMath;
import com.periut.retrotweaks.feature.hud.HudLayout;
import com.periut.retrotweaks.feature.hud.HudPanelState;
import com.periut.retrotweaks.feature.options.ModOptions;
import com.periut.retrotweaks.feature.options.RenderScaleMath;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.Option;

import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * The RetroTweaks config screen: one screen class for the whole tree.
 *
 * <p>Opening it shows the top-level sections; clicking a section opens the same screen one level down,
 * so nesting costs nothing. Everything about how a page is drawn - the black panel, the
 * twelve-pixel rows, the scroll wheel, the chin that explains whatever the cursor is on - belongs to
 * {@link ListScreen}; this class only says what the rows ARE.
 *
 * <p>Three kinds of row exist. Most come straight off the reflected {@link ConfigTree}. The Graphics
 * page adds rows that are not {@code ConfigTree} options at all: RetroDragon's live render scale and
 * filter, and the eight video settings that live on {@code Minecraft.options} - see the "Graphics page
 * extras" section and {@code Config.Graphics}' own doc for why none of that is a config field. And the
 * six HUD-element subsections replace the list entirely with a drag-to-position editor; see the "HUD
 * element drag-to-position" section.
 */
@Environment(EnvType.CLIENT)
public class ConfigScreen extends ListScreen {

	// ---- HUD element drag-to-position - only used when isHudElementScreen() is true ----
	private static final int PANEL_TITLE_HEIGHT = 14;
	private static final int PANEL_PADDING = 4;
	/** Same twelve pixels a list row gets, so the panel's controls read as the same controls. */
	private static final int PANEL_ROW_HEIGHT = ListScreen.ROW_HEIGHT;
	private static final int PANEL_ROW_GAP = 2;
	private static final int PANEL_MINIMIZE_SIZE = 10;
	private static final int PANEL_READOUT_HEIGHT = 10;
	/** Floor on the content column, so a short category name never yields a panel too cramped to click. */
	private static final int MIN_PANEL_CONTENT_WIDTH = 96;
	/** Near-opaque rather than the list's flat black: this floats over the HUD it is describing, and a
	 *  hint of what is behind it is what makes it read as a window rather than a hole. */
	private static final int PANEL_TITLE_COLOR = 0xF0181818;
	private static final int PANEL_BODY_COLOR = 0xE0000000;
	private static final int PANEL_BORDER_COLOR = 0xFFFFD040;
	private static final int OUTLINE_COLOR = 0xA000E5FF;
	private static final int OUTLINE_ACTIVE_COLOR = 0xF0FFD040;

	/**
	 * Clearance kept between the panel's right edge and the hotbar's default left edge on the HUD
	 * Positions page, where the live HUD is drawn behind the screen: the hotbar's default span is
	 * {@code width/2 +- 91} (InGameHudMixin's own half-width), and the panel has to stay off it.
	 */
	private static final int HUD_PREVIEW_CLEARANCE = 91 + 8;
	/** Below this the panel is too narrow to read, so it is allowed onto the hotbar's left end instead. */
	private static final int HUD_PREVIEW_MIN_PANEL = 140;

	private final Screen parent;
	/**
	 * The screen OUTSIDE the config system - the options screen it was opened from. Threaded down every
	 * page so a top-level page can leave in one step instead of retracing the way in; see {@link #done()}.
	 */
	private final Screen exit;
	/** Which mod's config this page belongs to - the one an edit is saved and sent under. */
	private final RetroConfig config;
	private final ConfigTree.Category category;
	private final String breadcrumb;

	// ---- HUD element drag-to-position state - see initHudElementPanel/layoutHudElementPanel ----
	private ConfigTree.Option hOffsetOption;
	private ConfigTree.Option vOffsetOption;
	private List<HudEditorMath.Kind> dragKinds = List.of();
	/** The three Options the panel edits, and why each one cannot be (see
	 *  {@link ConfigTree#unavailableReasonOf}) - resolved once per {@link #init()}. */
	private ConfigTree.Option panelVisibleOption;
	private ConfigTree.Option panelHorizontalOption;
	private ConfigTree.Option panelVerticalOption;
	private String panelVisibleReason = "";
	private String panelHorizontalReason = "";
	private String panelVerticalReason = "";
	/** Screen-space tops of the panel's six rows and the width they share, all set every frame by
	 *  {@link #layoutHudElementPanel} so drawing and hit-testing can never disagree. */
	private int panelContentWidth;
	private int panelVisibleY;
	private int panelHorizontalY;
	private int panelVerticalY;
	private int panelReadoutY;
	private int panelDefaultsY;
	private int panelDoneY;
	private int hudPanelWidth;
	private int hudPanelHeight;
	private boolean panelDragging;
	private int panelGrabDX;
	private int panelGrabDY;
	private boolean elementDragging;
	private int elementDragStartMouseX;
	private int elementDragStartMouseY;
	private int elementDragStartH;
	private int elementDragStartV;

	/** Seeded from {@link ApiBridge#getRenderScale} on every {@link #init()}, then only ever updated
	 *  from what {@link ApiBridge#setRenderScale} actually adopted - never from the value requested. */
	private float graphicsRenderScale;

	/** The landing page: this mod's own settings, with a way across to every other mod's. */
	public ConfigScreen(Screen parent) {
		this(parent, parent, ConfigManager.config(), ConfigManager.tree(), "Configs");
	}

	/** Any registered config's top page, which is what the Mods list opens. */
	public ConfigScreen(Screen parent, Screen exit, RetroConfig config) {
		this(parent, exit, config, config.tree(), config.name());
	}

	private ConfigScreen(Screen parent, Screen exit, RetroConfig config, ConfigTree.Category category,
			String breadcrumb) {
		this.parent = parent;
		this.exit = exit;
		this.config = config;
		this.category = category;
		this.breadcrumb = breadcrumb;
	}

	/**
	 * True while this screen is the "HUD Positions" page itself or one of its six element subsections
	 * (Simple Position / Advanced: Hotbar / Hearts / Armor / Oxygen / Overlay Message) - every other
	 * page (including "HUD" one level up, and "Debug Overlay" beside it) answers false.
	 *
	 * <p>Read by both this screen's own layout below and by
	 * {@link com.periut.retrotweaks.mixin.client.hud.InGameHudMixin}, which is what actually
	 * draws the live preview - this screen only has to know to get out of its way.
	 */
	public boolean isHudPositionsScreen() {
		ConfigTree.Category hudPositions = ConfigManager.hudPositionsNode();
		return hudPositions != null && (category == hudPositions || hudPositions.children.contains(category));
	}

	/**
	 * True while this screen is specifically one of the six HUD-element subsections (a leaf, not the
	 * "HUD Positions" listing page itself) - the ones backed by a {@code Config.HudElement}, which is
	 * what gets the drag-to-position panel instead of the usual list.
	 */
	public boolean isHudElementScreen() {
		ConfigTree.Category hudPositions = ConfigManager.hudPositionsNode();
		return hudPositions != null && category != hudPositions && hudPositions.children.contains(category);
	}

	/**
	 * True while this screen is the top-level "Scoring" page, which gets the WAYS viewer row.
	 */
	public boolean isScoringScreen() {
		ConfigTree.Category scoring = ConfigManager.scoringNode();
		return scoring != null && category == scoring;
	}

	/**
	 * True while this screen is the top-level "Graphics" page - not its "Video Settings" subsection,
	 * which stays a plain reflected page like any other category.
	 */
	public boolean isGraphicsScreen() {
		ConfigTree.Category graphics = ConfigManager.graphicsNode();
		return graphics != null && category == graphics;
	}

	// ================================================================ list mode

	@Override
	protected boolean listVisible() {
		// While an element is being dragged around the screen, a list covering the left third of it is
		// covering the very thing being positioned. The movable panel carries that page's controls
		// instead - see the HUD element section below.
		return !isHudElementScreen();
	}

	@Override
	protected String title() {
		return breadcrumb;
	}

	@Override
	protected int maxPanelWidth() {
		if (!isHudPositionsScreen()) return super.maxPanelWidth();
		// The live HUD renders behind this page and sits bottom-centre, so the panel keeps clear of the
		// hotbar's default span where the window is wide enough to allow it. Below HUD_PREVIEW_MIN_PANEL
		// there is no width left to read anything in, so a very small window gets a legible panel over
		// the hotbar's left end instead of an illegible one beside it - the pages where an element is
		// actually positioned hide the panel entirely anyway (see listVisible).
		return Math.min(super.maxPanelWidth(),
			Math.max(HUD_PREVIEW_MIN_PANEL, this.width / 2 - HUD_PREVIEW_CLEARANCE));
	}

	@Override
	protected void buildRows(List<Row> out) {
		// Before the settings, not after them: it is a way OUT of this page rather than a value on it,
		// and a page's exits belong at its top where a chevron row is looked for.
		if (isScoringScreen()) addWaysViewerRow(out);
		for (ConfigTree child : category.children) {
			if (child instanceof ConfigTree.Category sub) {
				out.add(new CategoryRow(sub));
			} else {
				ConfigTree.Option option = (ConfigTree.Option) child;
				out.add(new OptionRow(option, ConfigTree.unavailableReasonOf(option)));
			}
		}
		if (isGraphicsScreen()) addGraphicsRows(out);
	}

	/**
	 * The way in to the WAYS pages while they are kept off the achievements screen.
	 *
	 * <p>It belongs on this page and not on a menu: this is where somebody turns those pages off, so
	 * it is where they will look for them afterwards. Opening the achievements screen with navigation
	 * switched to the hidden set costs nothing extra - it IS the achievements screen, so the pages look
	 * exactly as they do when the setting above puts them on the main one.
	 *
	 * <p>Needs a world, because the screen reads this player's stats and the pages show their score, so
	 * out of one the row says so rather than opening something empty.
	 */
	private void addWaysViewerRow(List<Row> out) {
		// Registration happens once at startup; whether the pages are hidden follows the setting right
		// above this row, so it is re-applied before asking what is hidden - otherwise turning the
		// setting off would not make this row appear until the next launch.
		com.periut.retrotweaks.feature.scoring.achievement.ScoreAchievements.applyPageVisibility();
		if (!com.periut.retroapi.achievement.AchievementPage.hasHiddenPages()) return;
		boolean inWorld = this.minecraft != null && this.minecraft.world != null;
		out.add(new LinkRow("WAYS Achievements", "The score pages, kept off the achievements screen",
			inWorld ? "" : "only in a world",
			screen -> {
				// Built first, armed second: the screen's own constructor resets the mode, so every
				// ordinary way in starts on the vanilla page. See AchievementsScreenMixin.
				net.minecraft.client.gui.screen.AchievementsScreen achievements =
					new net.minecraft.client.gui.screen.AchievementsScreen(this.minecraft.stats);
				com.periut.retroapi.achievement.AchievementPage.setViewingHidden(true);
				this.minecraft.setScreen(achievements);
			}));
	}

	@Override
	protected void buildFooter(List<FooterButton> out) {
		out.add(new FooterButton("Defaults", this::confirmDefaults));
		out.add(new FooterButton("Done", this::done));
	}

	/**
	 * Leaves this page - one level up inside a config, straight out at the top of one.
	 *
	 * <p>Stepping back one level is right while there is a breadcrumb to climb, and wrong the moment
	 * there is not: the way IN to another mod's settings is Configs then Mods then the mod, so retracing
	 * it on the way out meant three presses of Escape and two screens nobody wanted to see again. A page
	 * with nothing above it inside the config system leaves the config system.
	 */
	private void done() {
		config.save();
		this.minecraft.setScreen(category == config.tree() ? exit : parent);
	}

	/**
	 * The one page-level control that is not a setting: the way across to every other mod's config.
	 *
	 * <p>Only on the top page - deeper pages have a breadcrumb to climb instead, and a sideways move
	 * from four levels down is a way to lose your place. The list behind it always has at least this
	 * mod in it, so the button is never a dead end even with nothing else installed.
	 */
	@Override
	protected HeaderButton headerButton() {
		if (category != config.tree()) return null;
		return new HeaderButton("Mods", () -> this.minecraft.setScreen(new ModsScreen(exit)));
	}

	/**
	 * Says once, at the bottom, what the red asterisks on this page mean. Only shown when there is at
	 * least one of them in view, so a page of purely client-side settings does not carry a warning
	 * about something it cannot do.
	 */
	@Override
	protected String footnote() {
		if (!ConfigSync.canEditServerConfig()) return null;
		for (Row row : rows) {
			if (row.serverEdit()) return ListScreen.SERVER_EDIT_MARK + " Edits affect server config";
		}
		return null;
	}

	/**
	 * Restores this page and everything under it, after confirming.
	 *
	 * <p>"This page and everything under it" rather than only the rows in view: a page is a section, and
	 * resetting Mobs while leaving Better Burning exactly as it was is not what anyone means by
	 * defaults. It follows that the top-level page resets the entire config, which is the only way to
	 * get back to a clean install without deleting the file by hand - so the confirmation says which of
	 * the two is about to happen rather than asking the same question either way.
	 *
	 * <p>Suppressed options are reset too. Their stored value is still the user's, still saved, and
	 * still what comes back if the mod suppressing them is uninstalled - so leaving them alone would
	 * quietly exempt them from a reset that claimed to cover everything.
	 */
	private void confirmDefaults() {
		boolean everything = category == config.tree();
		String message = everything
			? "Restore every " + config.name() + " setting, on every page, to its default? This cannot be undone."
			: "Restore " + category.name + " - and every section inside it - to defaults? This cannot be undone.";
		openPopup(new ConfirmPopup("Restore Defaults", message, serverDefaultsWarning(), "Restore", this::applyDefaults));
	}

	/**
	 * What a reset on this page would do to the SERVER, or null when it would do nothing to it.
	 *
	 * <p>Defaults is the one control here that moves many options at once, so it is the one where a
	 * player can change the whole server without having read a single asterisk. It says so, in the same
	 * red, and it counts the options rather than saying "some" - the difference between resetting one
	 * server setting and resetting forty is the whole question being asked.
	 */
	private String serverDefaultsWarning() {
		int count = countServerOptions();
		if (count == 0) return null;
		String subject = count == 1 ? "1 setting" : count + " settings";
		return ConfigSync.canEditServerConfig()
			? ListScreen.SERVER_EDIT_MARK + " " + subject + " here belong to the server. Restoring them "
				+ "changes the server's config for everyone on it."
			: ListScreen.SERVER_EDIT_MARK + " " + subject + " here belong to the server and will be left "
				+ "alone - only an operator can change those.";
	}

	/** How many options under this page a server currently owns. */
	private int countServerOptions() {
		int[] count = { 0 };
		ConfigTree.forEachOption(category, option -> {
			if (ConfigSync.isServerScoped(option)) count[0]++;
		});
		return count[0];
	}

	/**
	 * Resets every option on this page - each to wherever its value actually lives.
	 *
	 * <p>A server's option cannot be reset by writing the local copy: the field reads the server's
	 * value while the server is in charge, so {@code set} would move a number nobody is reading and the
	 * row would not budge. An operator's reset is therefore SENT, one option at a time, exactly as
	 * clicking each of them would have been; for anyone else it is skipped, which is what the
	 * confirmation said would happen.
	 */
	private void applyDefaults() {
		ConfigTree.forEachOption(category, option -> {
			if (ConfigSync.isServerScoped(option)) {
				if (ConfigSync.canEditServerConfig() && ConfigTree.isSyncable(option)) {
					ConfigNetworking.pushServerEdit(config, option, option.defaultValue);
				}
				return;
			}
			option.set(option.defaultValue);
		});
		// The video settings are not ConfigTree options (see the Graphics page extras section), so a
		// reset that covers their page has to put them back separately or the page would claim to have
		// reset rows it did not touch.
		if (category == ConfigManager.tree() || isGraphicsScreen()) {
			ModOptions.restoreVideoDefaults(this.minecraft);
		}
		HudLayout.configUpdated = true;
		config.save();
		if (isHudElementScreen()) {
			init(this.minecraft, this.width, this.height);
		} else {
			rebuildRows();
		}
	}

	@Override
	public void removed() {
		super.removed();
		config.save();
	}

	private static boolean isShiftDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
	}

	// ================================================================ rows

	/** A subsection: opens the same screen one level down. */
	private final class CategoryRow extends Row {
		private final ConfigTree.Category sub;

		CategoryRow(ConfigTree.Category sub) {
			this.sub = sub;
		}

		@Override
		public String label() {
			return sub.name;
		}

		@Override
		public String value() {
			return sub.isGateOpen() ? ">" : null;
		}

		@Override
		public String subLabel() {
			return sub.isGateOpen() ? null : "needs " + sub.gate.describeEnabledValues();
		}

		@Override
		public boolean enabled() {
			return sub.hasContent() && sub.isGateOpen();
		}

		@Override
		public String hint() {
			if (!sub.isGateOpen()) {
				// Names the controlling option, the value that would open the gate, and where that
				// option lives - built from this screen's own breadcrumb rather than a hardcoded string,
				// so it cannot go stale if the option or this section is ever moved.
				String gateHint = "Only used when " + sub.gate.controller.name + " is "
					+ sub.gate.describeEnabledValues() + " (" + breadcrumb + ")";
				return sub.desc.isEmpty() ? gateHint : sub.desc + ". " + gateHint;
			}
			return sub.desc;
		}

		@Override
		public void click(ListScreen screen, int relativeX, int button) {
			if (button != 0) return;
			minecraft.setScreen(new ConfigScreen(ConfigScreen.this, exit, config, sub, breadcrumb + " > " + sub.name));
		}
	}

	/** One reflected {@link ConfigTree.Option}. The kind decides the gesture; see {@link #click}. */
	private final class OptionRow extends Row {
		private final ConfigTree.Option option;
		private final String reason;

		OptionRow(ConfigTree.Option option, String reason) {
			this.option = option;
			this.reason = reason;
		}

		@Override
		public String label() {
			return option.name;
		}

		/** The value, or why it cannot be changed - see {@link Row#subLabel()}. A live boolean needs
		 *  neither: its checkbox already says everything there is to say. */
		@Override
		public String subLabel() {
			if (!reason.isEmpty()) return reason;
			return option.kind == ConfigTree.Kind.BOOLEAN ? null : valueText();
		}

		private String valueText() {
			Object value = option.effective();
			return switch (option.kind) {
				case FLOAT -> String.format(java.util.Locale.ROOT, "%.2f", ((Number) value).floatValue());
				// Arrays have no sensible gesture in a twelve-pixel row, so they stay read-only here and
				// are edited in the JSON file - which is exactly why the config is a JSON file.
				case LIST -> java.lang.reflect.Array.getLength(value) + " entries";
				default -> String.valueOf(value);
			};
		}

		@Override
		public boolean checkbox() {
			return option.kind == ConfigTree.Kind.BOOLEAN;
		}

		/**
		 * The value that is actually in force, which under a server is the SERVER's - not the value
		 * this player saved. Editing a row is editing what it shows, so a row showing the local value
		 * while sending the server's would be lying about both.
		 */
		@Override
		public boolean checked() {
			return Boolean.TRUE.equals(option.effective());
		}

		/** Red asterisk: this value belongs to the server, and this player is allowed to change it. */
		@Override
		public boolean serverEdit() {
			return ConfigSync.canEditServerConfig() && ConfigSync.isServerScoped(option);
		}

		/**
		 * A world option under a server the player cannot administer is the server's answer, full stop:
		 * shown, so they can see what they are playing under, but not editable, because nothing here
		 * could make the change take.
		 */
		@Override
		public boolean enabled() {
			if (ConfigSync.isServerScoped(option) && !ConfigSync.canEditServerConfig()) return false;
			return reason.isEmpty() && option.kind != ConfigTree.Kind.LIST;
		}

		@Override
		public String hint() {
			String hint = option.desc;
			if (ConfigSync.isServerScoped(option)) {
				// A greyed row with no explanation reads as broken. Say whose value it is, and - for an
				// operator, who CAN change it - what changing it would actually do.
				String note = ConfigSync.canEditServerConfig()
					? "This is the server's setting; changing it changes it for everyone"
					: "Set by the server for this session";
				hint = hint.isEmpty() ? note : hint + ". " + note;
			}
			if (option.restart) hint = hint.isEmpty() ? "Restart required" : hint + " (restart required)";
			String shared = ConfigTree.sharedNoteOf(option);
			if (!shared.isEmpty()) {
				// The row is live and editable; this only says where the value ends up, so it reads as
				// information rather than as the refusal the greyed-out row used to be.
				String note = "Applied through " + shared.substring("shared with ".length())
					+ ", which owns this feature while it is installed";
				hint = hint.isEmpty() ? note : hint + ". " + note;
			}
			if (option.kind == ConfigTree.Kind.LIST) {
				hint = hint.isEmpty() ? "Edit this list in the config file" : hint + ". Edit this list in the config file";
			}
			return hint;
		}

		@Override
		public void click(ListScreen screen, int relativeX, int button) {
			if (button != 0) return;
			switch (option.kind) {
				case BOOLEAN -> apply(!Boolean.TRUE.equals(option.effective()));
				case ENUM -> screen.openPopup(new ChoicePopup(option.name, enumLabels(), enumIndex(),
					index -> apply(option.enumValues[index])));
				case STRING -> screen.openPopup(new TextPopup(option.name, String.valueOf(option.effective()),
					String.valueOf(option.defaultValue), this::apply));
				case INT, FLOAT -> screen.openPopup(new SliderPopup(option.name, new NumberModel(option, this::apply),
					() -> apply(option.defaultValue)));
				default -> { }
			}
		}

		/**
		 * Writes a new value wherever this option's value actually lives.
		 *
		 * <p>Two destinations, and which one it is has nothing to do with the widget: a world option
		 * while a server is in charge belongs to the SERVER, so the value is sent and the row only moves
		 * once the server has accepted it and told everyone. Anything else is this client's own, and is
		 * set and saved here. Sending is safe to do unconditionally in that branch - only a row an
		 * operator was shown can be clicked at all (see {@link #enabled()}), and the server re-checks
		 * anyway.
		 */
		private void apply(Object value) {
			if (ConfigSync.isServerScoped(option)) {
				ConfigNetworking.pushServerEdit(config, option, value);
				return;
			}
			option.set(value);
			changed();
		}

		private String[] enumLabels() {
			String[] labels = new String[option.enumValues.length];
			for (int i = 0; i < labels.length; i++) labels[i] = String.valueOf(option.enumValues[i]);
			return labels;
		}

		private int enumIndex() {
			return ((Enum<?>) option.effective()).ordinal();
		}

		/**
		 * Applies the change there and then, rather than banking it until Done.
		 *
		 * <p>Setting the option already updates the field every mixin reads, but four things used to
		 * wait for {@link ConfigManager#save()}: the JSON on disk, the recipe and stack-size rebuild, the
		 * HUD layout recompute, and handing the value to UniTweaks. So a recipe toggle appeared to do
		 * nothing until the screen was closed. Saving here makes what you see match what you clicked -
		 * and it means a crash, or an Escape, cannot lose the change either.
		 *
		 * <p>Rebuilding the rows afterwards because a changed option can be some sibling section's gate
		 * controller (e.g. HUD Positioning System), and that section has to grey or ungrey immediately.
		 * Safe to do here because, unlike the vanilla button list, nothing is iterating the rows.
		 */
		private void changed() {
			HudLayout.configUpdated = true;
			config.save();
			rebuildRows();
		}
	}

	/**
	 * Maps a numeric {@link ConfigTree.Option}'s min/max onto the 0..1 a {@link SliderPopup} speaks.
	 *
	 * <p>Where the value goes while the handle is moving depends on whose value it is. A local option
	 * is written on every frame of the drag, because the field is what the mixins read and watching the
	 * world change under the slider is the point of dragging it. A SERVER's option cannot work that way
	 * - each write is a packet, and sixty a second for the length of a drag is a flood - so the drag is
	 * held here and sent once, when the handle is let go. Until then the readout shows the held value
	 * rather than the server's, so the slider tracks the hand that is moving it.
	 */
	private static final class NumberModel implements SliderPopup.Model {
		private final ConfigTree.Option option;
		private final java.util.function.Consumer<Object> apply;
		private final boolean deferred;
		/** The in-progress value of a deferred drag, or null when there is none. */
		private Object pending;

		NumberModel(ConfigTree.Option option, java.util.function.Consumer<Object> apply) {
			this.option = option;
			this.apply = apply;
			this.deferred = ConfigSync.isServerScoped(option);
		}

		/** What the slider is showing: the held value mid-drag, otherwise whatever is in force. */
		private Object shown() {
			return pending != null ? pending : option.effective();
		}

		@Override
		public float fraction() {
			double span = option.max - option.min;
			if (span <= 0) return 0.0F;
			return (float) Math.max(0.0, Math.min(1.0, (((Number) shown()).doubleValue() - option.min) / span));
		}

		@Override
		public void setFraction(float fraction) {
			double raw = option.min + fraction * (option.max - option.min);
			Object value = option.kind == ConfigTree.Kind.INT ? (Object) (int) Math.round(raw) : (Object) (float) raw;
			if (deferred) {
				pending = value;
			} else {
				apply.accept(value);
			}
		}

		@Override
		public String display() {
			Object value = shown();
			return option.kind == ConfigTree.Kind.FLOAT
				? String.format(java.util.Locale.ROOT, "%.2f", ((Number) value).floatValue())
				: String.valueOf(value);
		}

		@Override
		public float step() {
			double span = option.max - option.min;
			if (span <= 0) return 0.05F;
			// One whole unit per tap for an integer - the reason arrow keys exist on this popup at all
			// is options like the HUD offsets, whose descriptions recommend an exact number.
			return option.kind == ConfigTree.Kind.INT ? (float) (1.0 / span) : 0.01F;
		}

		/**
		 * The end of a drag: the one moment a deferred value is sent, and the moment a local one is
		 * written to disk. A local value is already live - {@code set} wrote the field the mixins read
		 * on the way past - so for that case this is only the disk write and the recipe/HUD/UniTweaks
		 * work a save does, none of which is worth doing sixty times a second.
		 */
		@Override
		public void released() {
			Object value = pending;
			pending = null;
			apply.accept(value != null ? value : option.effective());
		}
	}

	/**
	 * A row that opens something, drawn exactly like a subsection: label on the left, chevron on the
	 * right, one line.
	 *
	 * <p>Distinct from {@link ActionRow}, which shows a VALUE and therefore puts it on a second line
	 * where it has the full width. There is no value here - the row is a door - so it takes
	 * {@link CategoryRow}'s shape instead, which is what a chevron means everywhere else on these
	 * screens.
	 */
	private static final class LinkRow extends Row {
		private final String name;
		private final String description;
		private final String reason;
		private final java.util.function.Consumer<ListScreen> action;

		LinkRow(String name, String description, String reason,
				java.util.function.Consumer<ListScreen> action) {
			this.name = name;
			this.description = description;
			this.reason = reason;
			this.action = action;
		}

		@Override
		public String label() {
			return name;
		}

		@Override
		public String value() {
			return reason.isEmpty() ? ">" : null;
		}

		/** Only when it cannot be opened; an available door needs no second line. */
		@Override
		public String subLabel() {
			return reason.isEmpty() ? null : reason;
		}

		@Override
		public boolean enabled() {
			return reason.isEmpty();
		}

		@Override
		public String hint() {
			return description;
		}

		@Override
		public void click(ListScreen screen, int relativeX, int button) {
			if (button == 0) action.accept(screen);
		}
	}

	/** A row whose state is a plain boolean somewhere outside the config tree (RGSS, Mipmaps, Clouds). */
	private static final class ToggleRow extends Row {
		private final String name;
		private final String description;
		private final java.util.function.BooleanSupplier state;
		private final Runnable toggle;
		private final String reason;

		ToggleRow(String name, String description, java.util.function.BooleanSupplier state, Runnable toggle, String reason) {
			this.name = name;
			this.description = description;
			this.state = state;
			this.toggle = toggle;
			this.reason = reason;
		}

		@Override
		public String label() {
			return name;
		}

		@Override
		public String subLabel() {
			return reason.isEmpty() ? null : reason;
		}

		@Override
		public boolean checkbox() {
			return true;
		}

		@Override
		public boolean checked() {
			return state.getAsBoolean();
		}

		@Override
		public boolean enabled() {
			return reason.isEmpty();
		}

		@Override
		public String hint() {
			return description;
		}

		@Override
		public void click(ListScreen screen, int relativeX, int button) {
			if (button == 0) toggle.run();
		}
	}

	/** A row that shows a value and opens something when clicked. Used by the render scale and filter. */
	private static final class ActionRow extends Row {
		private final String name;
		private final String description;
		private final java.util.function.Supplier<String> value;
		private final java.util.function.Consumer<ListScreen> action;
		private final java.util.function.BooleanSupplier available;
		private final String reason;

		ActionRow(String name, String description, java.util.function.Supplier<String> value,
				java.util.function.BooleanSupplier available, String reason,
				java.util.function.Consumer<ListScreen> action) {
			this.name = name;
			this.description = description;
			this.value = value;
			this.available = available;
			this.reason = reason;
			this.action = action;
		}

		@Override
		public String label() {
			return name;
		}

		/** The live value, or the reason there is no point reading it - see {@link Row#subLabel()}. */
		@Override
		public String subLabel() {
			return reason.isEmpty() ? value.get() : reason;
		}

		@Override
		public boolean enabled() {
			return available.getAsBoolean();
		}

		@Override
		public String hint() {
			return description;
		}

		@Override
		public void click(ListScreen screen, int relativeX, int button) {
			if (button == 0) action.accept(screen);
		}
	}

	// ================================================================ Graphics page extras
	//
	// Only ever built on the top-level "Graphics" page (isGraphicsScreen()) - not its "Video Settings"
	// subsection, which stays a plain reflected page like any other category (still reachable there,
	// still the boolean override flags - see Config.VideoSettings' own doc). None of what follows is
	// backed by a ConfigTree.Option: render scale/filter/RGSS/Mipmaps are RetroDragon's own live
	// RetroSettings state, and the eight video-option rows are bound straight through Minecraft.options'
	// getFloat/setFloat/getString - the very methods GameOptionsMixin is woven into - so a row here and
	// its vanilla-screen counterpart always show and store the exact same value, with no second
	// formatter to drift out of sync.

	private void addGraphicsRows(List<Row> out) {
		if (ApiBridge.hasRenderScale()) {
			// Seeded from RetroDragon's own current value every time this page is (re)built - RetroDragon
			// already seeds ITS default from the window's pixels-per-point (1.0 normally, 2.0 on an
			// untouched Retina/HiDPI window), so reading it here rather than assuming 1.0 is what makes
			// this page agree with whatever RetroDragon actually started at.
			graphicsRenderScale = ApiBridge.getRenderScale();
			out.add(new ActionRow("Render Scale", "Renders the world above or below the window's own resolution",
				() -> String.format(java.util.Locale.ROOT, "%.2fx", graphicsRenderScale),
				() -> true, "",
				screen -> screen.openPopup(new SliderPopup("Render Scale", new RenderScaleModel(), null))));
			out.add(new ActionRow("Scale Filter", "How the render is resampled to the window",
				this::filterValue,
				() -> currentScaleFilters().length > 0, "",
				screen -> {
					// Asked for fresh every time rather than cached: which filters exist depends on which
					// side of 1.0 the render scale currently sits on (upsamplers stop being offered above
					// it), so the list re-queries itself the moment the scale crosses over.
					String[] filters = currentScaleFilters();
					screen.openPopup(new ChoicePopup("Scale Filter", filters, indexOf(filters, ApiBridge.getScaleFilter()),
						index -> ApiBridge.setScaleFilter(filters[index])));
				}));
		}
		if (ApiBridge.hasRetroDragonSettings()) {
			out.add(new ToggleRow("RGSS Anti-Aliasing", "Rotated grid supersampling, from RetroDragon",
				ApiBridge::isRgss, () -> ApiBridge.setRgss(!ApiBridge.isRgss()), ""));
			out.add(new ToggleRow("Mipmaps", "Smooths distant textures, from RetroDragon",
				ApiBridge::isMipmap, () -> ApiBridge.setMipmap(!ApiBridge.isMipmap()), ""));
		}

		// UniTweaks supersedes the entire video-settings stack, OptionMixin included, so under it these
		// eight Option constants were never created and there is nothing here to bind a row to. The
		// "Video Settings" subsection still lists the override flags, which now drive UniTweaks' own
		// sliders through UniTweaksBridge - so the settings remain reachable, just not as rows here.
		if (ModOptions.fogDensityOption == null) return;

		ConfigTree.Category videoCat = ConfigTree.find(category, "video");
		out.add(videoRow(ModOptions.fogDensityOption, videoCat, "fogDensityOverride", "How far you can see before fog closes in", 0.05F));
		out.add(videoRow(ModOptions.cloudsOption, videoCat, "cloudsToggle", "Draw clouds at all", 0.0F));
		out.add(videoRow(ModOptions.cloudHeightOption, videoCat, "cloudHeightOverride", "Height in blocks the clouds are drawn at", 0.02F));
		out.add(videoRow(ModOptions.brightnessOption, videoCat, "brightnessOverride", "Extra light added to the whole world", 0.05F));
		out.add(videoRow(ModOptions.fovOption, videoCat, "fovOverride", "Field of view, added on top of vanilla's own 70 degrees", 0.025F));
		// One tap per chunk / per stop, so the -/+ buttons land on whole values rather than between two.
		out.add(videoRow(ModOptions.renderDistanceOption, videoCat, "renderDistanceOverride", "Chunks drawn around you", 1.0F / 30.0F));
		out.add(videoRow(ModOptions.fpsLimitOption, videoCat, "fpsLimitOverride", "VSync, a numeric cap, or unlimited", 1.0F / 26.0F));
		out.add(videoRow(ModOptions.guiScaleOption, videoCat, "guiScaleOverride", "Reaches scales vanilla's four fixed steps cannot", 0.0F));
	}

	/**
	 * One of RetroTweaks' added video options as a row. {@code overrideFieldKey} names the sibling
	 * {@link com.periut.retrotweaks.config.Config.VideoSettings} flag whose
	 * {@link ConfigTree#unavailableReasonOf} greys this row out in lock-step with whether
	 * {@link ModOptions#enabled} would honour a drag or a click at all - so a row is never left live
	 * while the value it writes is being ignored.
	 */
	private Row videoRow(Option option, ConfigTree.Category videoCat, String overrideFieldKey, String description, float step) {
		ConfigTree.Option gate = videoCat != null ? ConfigTree.findOption(videoCat, overrideFieldKey) : null;
		String reason = gate != null ? ConfigTree.unavailableReasonOf(gate) : "";
		String name = stripLabelSuffix(ModOptions.optionLabel(option));

		if (option == ModOptions.cloudsOption) {
			return new ToggleRow(name, description, () -> ModOptions.clouds,
				() -> this.minecraft.options.setInt(option, 1), reason);
		}
		if (option == ModOptions.guiScaleOption) {
			// One tap per reachable step, which is only known once the window size is - see
			// ModOptions.guiScaleCeiling.
			return new ActionRow(name, description, () -> videoValue(option), () -> reason.isEmpty(), reason,
				screen -> screen.openPopup(new SliderPopup(name,
					new VideoOptionModel(option, 1.0F / Math.max(1, ModOptions.guiScaleCeiling())), null)));
		}
		return new ActionRow(name, description, () -> videoValue(option), () -> reason.isEmpty(), reason,
			screen -> screen.openPopup(new SliderPopup(name, new VideoOptionModel(option, step), null)));
	}

	/** "Fog Density: " -> "Fog Density"; the row draws the value in its own column instead. */
	private static String stripLabelSuffix(String label) {
		return label.endsWith(": ") ? label.substring(0, label.length() - 2) : label;
	}

	/**
	 * The value the vanilla screen would show, without the "Fog Density: " that
	 * {@code GameOptionsMixin.getString} prepends - the row's own label column already says which
	 * option it is. Still shown (greyed) on a row that is standing down for another mod, since the
	 * stored value is real and comes back the moment that mod is removed.
	 */
	private String videoValue(Option option) {
		String full = this.minecraft.options.getString(option);
		String prefix = ModOptions.optionLabel(option);
		return full.startsWith(prefix) ? full.substring(prefix.length()) : full;
	}

	/** Binds a {@link SliderPopup} straight to {@code Minecraft.options}, with no cached copy. */
	private final class VideoOptionModel implements SliderPopup.Model {
		private final Option option;
		private final float step;

		VideoOptionModel(Option option, float step) {
			this.option = option;
			this.step = step <= 0.0F ? 0.05F : step;
		}

		@Override
		public float fraction() {
			return minecraft.options.getFloat(option);
		}

		@Override
		public void setFraction(float fraction) {
			minecraft.options.setFloat(option, fraction);
		}

		@Override
		public String display() {
			return minecraft.options.getString(option);
		}

		@Override
		public float step() {
			return step;
		}

		/**
		 * GUI scale and the FPS limit only snap onto a whole step - and GUI scale is only actually
		 * applied to the render - once the mouse button is up, which {@code GameOptionsMixin.setFloat}
		 * tests for directly. Nothing calls it again by itself when a drag ends, so this does.
		 */
		@Override
		public void released() {
			minecraft.options.setFloat(option, minecraft.options.getFloat(option));
		}
	}

	/**
	 * The render scale, expressed as a fraction of RetroDragon's documented {@code [0.1, 4.0]}. Every
	 * write is a REQUEST: {@link ApiBridge#setRenderScale} returns what RetroDragon actually adopted
	 * after its own clamp, and that - never the value asked for - is what the readout is updated from,
	 * so the display cannot lie at either end of the range.
	 */
	private final class RenderScaleModel implements SliderPopup.Model {
		private static final float SPAN = RenderScaleMath.MAX_SCALE - RenderScaleMath.MIN_SCALE;

		@Override
		public float fraction() {
			return (graphicsRenderScale - RenderScaleMath.MIN_SCALE) / SPAN;
		}

		@Override
		public void setFraction(float fraction) {
			float requested = RenderScaleMath.step(RenderScaleMath.MIN_SCALE + fraction * SPAN, 0.0F);
			graphicsRenderScale = ApiBridge.setRenderScale(requested);
		}

		@Override
		public String display() {
			return String.format(java.util.Locale.ROOT, "%.2fx", graphicsRenderScale);
		}

		@Override
		public float step() {
			return 0.05F / SPAN;
		}
	}

	/** Where {@code value} sits in {@code values}, or 0 when it is not there at all (nothing chosen yet,
	 *  or the current filter stopped being offered because the scale crossed 1.0). */
	private static int indexOf(String[] values, String value) {
		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(value)) return i;
		}
		return 0;
	}

	private String[] currentScaleFilters() {
		return ApiBridge.getAvailableScaleFilters(graphicsRenderScale < 1.0F);
	}

	private String filterValue() {
		return currentScaleFilters().length == 0 ? "none available" : ApiBridge.getScaleFilter();
	}

	// ================================================================ HUD element drag-to-position
	//
	// Only ever active on one of the six HUD-element subsections (isHudElementScreen()); the "HUD
	// Positions" listing page one level up keeps the normal list, since there is nothing on it to drag.
	//
	// Precedence, closest to the player first: the movable panel (drawn on top of the HUD, like
	// everything else this screen draws) always wins a click inside its own footprint, including empty
	// padding between its buttons; there is no list on this page to check next (see listVisible()), so a
	// click that lands outside the panel can start dragging a HUD element.
	//
	// Bounds for hit-testing and outlining come from HudEditorMath.elementBounds, fed by HudLayout's
	// published offsets - this never recomputes InGameHudMixin's own offset maths, only adds a fixed
	// vanilla rectangle to a number that mixin already worked out (see both classes' docs).
	//
	// Which Option a drag writes into is decided once, in initHudElementPanel, by which of the six
	// subsections this screen is: "Simple Position" maps every one of the five on-screen regions to the
	// *same* pair of Options (Config.HudPositions.simple's own offsets), so grabbing any part of the HUD
	// moves the whole block together, matching what Enums.HudPositioning.SIMPLE actually reads; each
	// "Advanced: *" subsection maps only its own region to its own element's Options. DISABLED is
	// handled implicitly: none of the six subsections is reachable from the listing page while it is
	// selected (their row's gate is closed), so a HUD-element screen only exists while its gate is open.

	@Override
	public void init() {
		super.init();
		if (isHudElementScreen()) {
			initHudElementPanel();
			// Lays the panel out (position/size/clamp) once immediately, so its buttons sit somewhere
			// correct and clickable even before the first render() call.
			layoutHudElementPanel(0, 0);
		} else {
			hOffsetOption = null;
			vOffsetOption = null;
			dragKinds = List.of();
		}
	}

	private static List<HudEditorMath.Kind> dragKindsFor(String categoryKey) {
		return switch (categoryKey) {
			case "simple" -> List.of(HudEditorMath.Kind.HOTBAR, HudEditorMath.Kind.HEARTS,
				HudEditorMath.Kind.ARMOR, HudEditorMath.Kind.OXYGEN, HudEditorMath.Kind.OVERLAY);
			case "hotbar" -> List.of(HudEditorMath.Kind.HOTBAR);
			case "hearts" -> List.of(HudEditorMath.Kind.HEARTS);
			case "armor" -> List.of(HudEditorMath.Kind.ARMOR);
			case "oxygen" -> List.of(HudEditorMath.Kind.OXYGEN);
			case "overlayMessage" -> List.of(HudEditorMath.Kind.OVERLAY);
			default -> List.of();
		};
	}

	/** Picks up which Options this page edits. Positions are decided every frame in
	 *  {@link #layoutHudElementPanel}; nothing here is a widget - see {@link #drawHudPanel}. */
	private void initHudElementPanel() {
		hOffsetOption = ConfigTree.findOption(category, "horizontalOffset");
		vOffsetOption = ConfigTree.findOption(category, "verticalOffset");
		dragKinds = dragKindsFor(category.key);

		panelVisibleOption = ConfigTree.findOption(category, "visible");
		panelHorizontalOption = ConfigTree.findOption(category, "horizontal");
		panelVerticalOption = ConfigTree.findOption(category, "vertical");
		panelVisibleReason = ConfigTree.unavailableReasonOf(panelVisibleOption);
		panelHorizontalReason = ConfigTree.unavailableReasonOf(panelHorizontalOption);
		panelVerticalReason = ConfigTree.unavailableReasonOf(panelVerticalOption);

		panelDragging = false;
		elementDragging = false;
	}

	/**
	 * Escape steps back ONE level rather than closing everything.
	 *
	 * <p>Vanilla's {@code Screen.keyPressed} sends Escape straight to {@code setScreen(null)}, i.e. back
	 * into the game. On a tree of pages that is the wrong shape entirely: six levels down in HUD
	 * Positions, the key that everywhere else means "back" would throw away the whole trail. It does
	 * exactly what Done does instead, and since each page's parent is the page it was opened from, that
	 * walks back up one level at a time and only leaves for the Options screen from the top page.
	 *
	 * <p>The other branch is the arrow-key precision fallback for what dragging cannot reliably hit -
	 * the option's own description recommends -32 for the Xbox layout (see {@code Config.HudElement}),
	 * so an exact value has to stay reachable. An open popup gets first refusal on both, via
	 * {@link ListScreen#keyPressed}.
	 */
	@Override
	protected void keyPressed(char character, int keyCode) {
		if (!hasPopup()) {
			if (keyCode == Keyboard.KEY_ESCAPE) {
				done();
				return;
			}
			if (isHudElementScreen() && category.isGateOpen() && isNudgeKey(keyCode)) {
				nudgeElement(keyCode);
				return;
			}
		}
		super.keyPressed(character, keyCode);
	}

	private static boolean isNudgeKey(int keyCode) {
		return keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT
			|| keyCode == Keyboard.KEY_UP || keyCode == Keyboard.KEY_DOWN;
	}

	/**
	 * Moves {@link #hOffsetOption}/{@link #vOffsetOption} - the same pair a drag on this page writes
	 * into - by {@link HudEditorMath#NUDGE_STEP}, or {@link HudEditorMath#NUDGE_STEP_FAST} with Shift
	 * held, clamped exactly like a drag is. Up/Left decrease, Down/Right increase, matching the sign a
	 * mouse drag already uses (screen Y grows downward, so "up" has to subtract).
	 */
	private void nudgeElement(int keyCode) {
		int step = isShiftDown() ? HudEditorMath.NUDGE_STEP_FAST : HudEditorMath.NUDGE_STEP;
		int dh = 0;
		int dv = 0;
		switch (keyCode) {
			case Keyboard.KEY_LEFT -> dh = -step;
			case Keyboard.KEY_RIGHT -> dh = step;
			case Keyboard.KEY_UP -> dv = -step;
			case Keyboard.KEY_DOWN -> dv = step;
			default -> { return; }
		}
		int currentH = ((Number) hOffsetOption.get()).intValue();
		int currentV = ((Number) vOffsetOption.get()).intValue();
		hOffsetOption.set(HudEditorMath.clampOffset(currentH + dh, (int) hOffsetOption.min, (int) hOffsetOption.max));
		vOffsetOption.set(HudEditorMath.clampOffset(currentV + dv, (int) vOffsetOption.min, (int) vOffsetOption.max));
		HudLayout.configUpdated = true;
	}

	@Override
	protected void renderAfterWidgets(int mouseX, int mouseY, float delta) {
		if (isHudElementScreen()) {
			layoutHudElementPanel(mouseX, mouseY);
			// Outlines first, panel over them: the panel can be dragged on top of the element it is
			// describing, and the thing you are holding should not be drawn under the thing you are not.
			// Outlining needs the live HUD to have drawn a frame at all, which needs a world - see the
			// matching guard in mouseClicked.
			if (this.minecraft.world != null) drawHudElementOutlines();
			drawHudPanel(mouseX, mouseY);
		}
		if (isHudPositionsScreen() && this.minecraft.world == null) {
			// HONEST LIMIT: with no world loaded, InGameHud.render() is never called at all (see
			// GameRenderer), so there is nothing to preview from here - say so rather than leaving an
			// unexplained empty side. Centred in whichever gap beside the panel is the wider - which is
			// the right one when the panel is left-bound, and either one once it centres itself with no
			// world behind it (ListScreen.panelCentered) - and trimmed to that gap rather than allowed to
			// run back under the panel.
			int leftGap = listVisible() ? panelLeft : 0;
			int rightGap = listVisible() ? this.width - panelRight() : this.width;
			int gapLeft = rightGap >= leftGap ? this.width - rightGap : 0;
			int gapWidth = Math.max(rightGap, leftGap);
			this.drawCenteredTextWithShadow(this.textRenderer,
				trim("Live preview appears here once you are in a world", gapWidth - 8),
				gapLeft + gapWidth / 2, this.height / 2 - 4, 0x808080);
		}
	}

	/** "Offset: h, v" - the live values, drawn whenever the panel is expanded (not only while dragging),
	 *  so a player nudging or dragging can always see exactly what they are hitting. */
	private String readoutText() {
		return "Offset: " + hOffsetOption.get() + ", " + vOffsetOption.get();
	}

	/**
	 * Every-frame layout: follows an active panel drag, measures the widest row so the content column
	 * fits it (never just once - Horizontal/Vertical Position's value changes length every time it is
	 * picked, and the offset readout grows as the numbers do), works out the panel's size and every
	 * row's position via {@link HudEditorMath#panelWidth}/{@link HudEditorMath#panelBody}, clamps the
	 * panel on screen, and - while an element drag is active - keeps writing the live mouse delta into
	 * the two offset options.
	 */
	private void layoutHudElementPanel(int mouseX, int mouseY) {
		if (HudPanelState.x < 0) {
			// First time this session: park it up near the top-right, clear of the HUD's default
			// bottom-centre span. Not pixel-exact (hudPanelWidth is not known yet) - the clamp below
			// corrects it onto the screen regardless, and the player can drag it anywhere after.
			HudPanelState.x = Math.max(0, this.width - 150);
			HudPanelState.y = 8;
		}
		if (panelDragging) {
			HudPanelState.x = mouseX - panelGrabDX;
			HudPanelState.y = mouseY - panelGrabDY;
		}

		int content = ListScreen.CHECKBOX_SIZE + 4 + this.textRenderer.getWidth(panelVisibleOption.name);
		content = Math.max(content, valueRowWidth(panelHorizontalOption));
		content = Math.max(content, valueRowWidth(panelVerticalOption));
		content = Math.max(content, this.textRenderer.getWidth(readoutText()));
		content = Math.max(content, MIN_PANEL_CONTENT_WIDTH);
		panelContentWidth = content;

		int titleWidth = HudEditorMath.panelTitleWidth(this.textRenderer.getWidth(category.name), PANEL_PADDING, PANEL_MINIMIZE_SIZE);
		hudPanelWidth = HudEditorMath.panelWidth(content, PANEL_PADDING, titleWidth);

		HudEditorMath.PanelBody body = HudEditorMath.panelBody(
			PANEL_TITLE_HEIGHT, PANEL_PADDING, PANEL_ROW_HEIGHT, PANEL_ROW_GAP, PANEL_READOUT_HEIGHT);
		hudPanelHeight = HudPanelState.minimized ? PANEL_TITLE_HEIGHT : body.height();

		HudPanelState.x = HudEditorMath.clampPanelPosition(HudPanelState.x, hudPanelWidth, this.width);
		HudPanelState.y = HudEditorMath.clampPanelPosition(HudPanelState.y, hudPanelHeight, this.height);

		panelVisibleY = HudPanelState.y + body.visibleY();
		panelHorizontalY = HudPanelState.y + body.horizontalY();
		panelVerticalY = HudPanelState.y + body.verticalY();
		panelReadoutY = HudPanelState.y + body.readoutY();
		panelDefaultsY = HudPanelState.y + body.navRowY();
		panelDoneY = HudPanelState.y + body.doneY();

		if (elementDragging) {
			int dx = mouseX - elementDragStartMouseX;
			int dy = mouseY - elementDragStartMouseY;
			hOffsetOption.set(HudEditorMath.clampOffset(elementDragStartH + dx, (int) hOffsetOption.min, (int) hOffsetOption.max));
			vOffsetOption.set(HudEditorMath.clampOffset(elementDragStartV + dy, (int) vOffsetOption.min, (int) vOffsetOption.max));
			// Without this, the live preview would only pick the new offset up once the screen closes
			// (or "Defaults"/leaving triggers ConfigManager.save()) - see HudLayout's own class doc.
			HudLayout.configUpdated = true;
		}
	}

	/** Room a "Label            Value" row needs: both texts plus a readable gap between them. */
	private int valueRowWidth(ConfigTree.Option option) {
		return this.textRenderer.getWidth(option.name) + 10 + this.textRenderer.getWidth(String.valueOf(option.get()));
	}

	/**
	 * The whole floating panel, drawn in one pass: title bar, the three controls, the live offset
	 * readout, Defaults and Done.
	 *
	 * <p>These were vanilla {@code ButtonWidget}s until the config list stopped being made of them.
	 * Keeping them would have left the one screen a player uses to make the HUD look right as the one
	 * screen that did not match the rest of the mod - and cost this panel roughly twice its height, at
	 * 20px a button, hovering over the very HUD it exists to get out of the way of. The controls are
	 * the same checkbox and value rows the list uses, at the same twelve pixels each.
	 */
	private void drawHudPanel(int mouseX, int mouseY) {
		int x = HudPanelState.x;
		int y = HudPanelState.y;
		boolean expanded = !HudPanelState.minimized;

		this.fill(x, y, x + hudPanelWidth, y + PANEL_TITLE_HEIGHT, PANEL_TITLE_COLOR);
		if (expanded) this.fill(x, y + PANEL_TITLE_HEIGHT, x + hudPanelWidth, y + hudPanelHeight, PANEL_BODY_COLOR);
		outline(x, y, hudPanelWidth, hudPanelHeight, PANEL_BORDER_COLOR);
		this.drawTextWithShadow(this.textRenderer,
			trim(category.name, hudPanelWidth - PANEL_PADDING * 2 - PANEL_MINIMIZE_SIZE - 2),
			x + PANEL_PADDING, y + 3, ListScreen.TEXT);

		HudEditorMath.Rect minimize = minimizeRect();
		drawButton(minimize.x(), minimize.y(), minimize.width(), minimize.height(),
			HudPanelState.minimized ? "+" : "-", true, minimize.contains(mouseX, mouseY));
		if (!expanded) return;

		int left = x + PANEL_PADDING;
		int right = left + panelContentWidth;
		drawPanelCheckboxRow(left, right, panelVisibleY, panelVisibleOption, panelVisibleReason, mouseX, mouseY);
		drawPanelValueRow(left, right, panelHorizontalY, panelHorizontalOption, panelHorizontalReason, mouseX, mouseY);
		drawPanelValueRow(left, right, panelVerticalY, panelVerticalOption, panelVerticalReason, mouseX, mouseY);

		this.drawTextWithShadow(this.textRenderer, readoutText(), left, panelReadoutY, ListScreen.TEXT_DIM);
		drawButton(left, panelDefaultsY, panelContentWidth, PANEL_ROW_HEIGHT, "Defaults", true,
			inPanelRow(mouseX, mouseY, left, right, panelDefaultsY));
		drawButton(left, panelDoneY, panelContentWidth, PANEL_ROW_HEIGHT, "Done", true,
			inPanelRow(mouseX, mouseY, left, right, panelDoneY));
	}

	private void drawPanelCheckboxRow(int left, int right, int rowY, ConfigTree.Option option, String reason,
			int mouseX, int mouseY) {
		boolean enabled = reason.isEmpty();
		boolean hovered = enabled && inPanelRow(mouseX, mouseY, left, right, rowY);
		int color = !enabled ? ListScreen.TEXT_OFF : hovered ? ListScreen.TEXT_HOVER : ListScreen.TEXT;
		if (hovered) this.fill(left - 1, rowY, right + 1, rowY + PANEL_ROW_HEIGHT, ListScreen.ROW_HOVER_BG);
		drawCheckbox(left, rowY + 2, Boolean.TRUE.equals(option.get()), enabled, hovered, color);
		int textX = left + ListScreen.CHECKBOX_SIZE + 4;
		this.drawTextWithShadow(this.textRenderer, trim(enabled ? option.name : reason, right - textX), textX, rowY + 2, color);
	}

	private void drawPanelValueRow(int left, int right, int rowY, ConfigTree.Option option, String reason,
			int mouseX, int mouseY) {
		boolean enabled = reason.isEmpty();
		boolean hovered = enabled && inPanelRow(mouseX, mouseY, left, right, rowY);
		int color = !enabled ? ListScreen.TEXT_OFF : hovered ? ListScreen.TEXT_HOVER : ListScreen.TEXT;
		if (hovered) this.fill(left - 1, rowY, right + 1, rowY + PANEL_ROW_HEIGHT, ListScreen.ROW_HOVER_BG);
		if (!enabled) {
			this.drawTextWithShadow(this.textRenderer, trim(reason, right - left), left, rowY + 2, color);
			return;
		}
		String value = String.valueOf(option.get());
		int valueWidth = this.textRenderer.getWidth(value);
		this.drawTextWithShadow(this.textRenderer, trim(option.name, right - left - valueWidth - 6), left, rowY + 2, color);
		this.drawTextWithShadow(this.textRenderer, value, right - valueWidth, rowY + 2,
			hovered ? ListScreen.TEXT_HOVER : ListScreen.TEXT_VALUE);
	}

	private static boolean inPanelRow(int mouseX, int mouseY, int left, int right, int rowY) {
		return mouseX >= left - 1 && mouseX <= right + 1 && mouseY >= rowY && mouseY < rowY + PANEL_ROW_HEIGHT;
	}

	private HudEditorMath.Rect minimizeRect() {
		return new HudEditorMath.Rect(HudPanelState.x + hudPanelWidth - PANEL_PADDING - PANEL_MINIMIZE_SIZE,
			HudPanelState.y + 2, PANEL_MINIMIZE_SIZE, PANEL_MINIMIZE_SIZE);
	}

	/** Dispatches a click that landed inside the panel. Anything not on a control drags the panel. */
	private void clickedHudPanel(int mouseX, int mouseY) {
		if (minimizeRect().contains(mouseX, mouseY)) {
			playClick();
			HudPanelState.minimized = !HudPanelState.minimized;
			return;
		}
		if (mouseY < HudPanelState.y + PANEL_TITLE_HEIGHT) {
			panelDragging = true;
			panelGrabDX = mouseX - HudPanelState.x;
			panelGrabDY = mouseY - HudPanelState.y;
			return;
		}
		if (HudPanelState.minimized) return;

		int left = HudPanelState.x + PANEL_PADDING;
		int right = left + panelContentWidth;
		if (panelVisibleReason.isEmpty() && inPanelRow(mouseX, mouseY, left, right, panelVisibleY)) {
			playClick();
			panelVisibleOption.set(!Boolean.TRUE.equals(panelVisibleOption.get()));
			// Without this the live preview would not show the new anchor or visibility until the screen
			// is closed - see HudLayout's own class doc.
			HudLayout.configUpdated = true;
			config.save();
			return;
		}
		if (panelHorizontalReason.isEmpty() && inPanelRow(mouseX, mouseY, left, right, panelHorizontalY)) {
			openPanelChoice(panelHorizontalOption);
			return;
		}
		if (panelVerticalReason.isEmpty() && inPanelRow(mouseX, mouseY, left, right, panelVerticalY)) {
			openPanelChoice(panelVerticalOption);
			return;
		}
		if (inPanelRow(mouseX, mouseY, left, right, panelDefaultsY)) {
			playClick();
			confirmDefaults();
			return;
		}
		if (inPanelRow(mouseX, mouseY, left, right, panelDoneY)) {
			playClick();
			done();
			return;
		}
		// Empty padding between the controls: drag from there too, so the panel is grabbable by more
		// than its title bar without any control ever being missed for a drag.
		panelDragging = true;
		panelGrabDX = mouseX - HudPanelState.x;
		panelGrabDY = mouseY - HudPanelState.y;
	}

	private void openPanelChoice(ConfigTree.Option option) {
		playClick();
		String[] labels = new String[option.enumValues.length];
		for (int i = 0; i < labels.length; i++) labels[i] = String.valueOf(option.enumValues[i]);
		openPopup(new ChoicePopup(option.name, labels, ((Enum<?>) option.get()).ordinal(), index -> {
			option.set(option.enumValues[index]);
			HudLayout.configUpdated = true;
			config.save();
		}));
	}

	private void drawHudElementOutlines() {
		if (!category.isGateOpen()) return;
		int color = elementDragging ? OUTLINE_ACTIVE_COLOR : OUTLINE_COLOR;
		for (HudEditorMath.Kind kind : dragKinds) {
			if (!HudLayout.visible(kind)) continue;
			HudEditorMath.Rect r = HudEditorMath.elementBounds(kind, HudLayout.scaledWidth, HudLayout.scaledHeight,
				HudLayout.xOffset(kind), HudLayout.yOffset(kind));
			outline(r.x(), r.y(), r.width(), r.height(), color);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		// Sampled before the click is dispatched: a click that DISMISSES a popup must not also start
		// dragging whatever HUD element happened to be under the popup.
		boolean hadPopup = hasPopup();
		super.mouseClicked(mouseX, mouseY, button);
		if (hadPopup || button != 0 || !isHudElementScreen()) return;

		// The panel is drawn on top of everything this screen draws, so it always wins a click inside
		// its own footprint - including empty padding, which just absorbs the click rather than passing
		// it through to whatever HUD element happens to be underneath.
		HudEditorMath.Rect panelRect = new HudEditorMath.Rect(HudPanelState.x, HudPanelState.y, hudPanelWidth, hudPanelHeight);
		if (panelRect.contains(mouseX, mouseY)) {
			clickedHudPanel(mouseX, mouseY);
			return;
		}

		// Nothing to hit-test without a live HUD frame to have published HudLayout's numbers.
		if (this.minecraft.world == null) return;
		if (!category.isGateOpen()) return;

		for (HudEditorMath.Kind kind : dragKinds) {
			if (!HudLayout.visible(kind)) continue;
			HudEditorMath.Rect rect = HudEditorMath.elementBounds(kind, HudLayout.scaledWidth, HudLayout.scaledHeight,
				HudLayout.xOffset(kind), HudLayout.yOffset(kind));
			if (rect.contains(mouseX, mouseY)) {
				elementDragging = true;
				elementDragStartMouseX = mouseX;
				elementDragStartMouseY = mouseY;
				elementDragStartH = ((Number) hOffsetOption.get()).intValue();
				elementDragStartV = ((Number) vOffsetOption.get()).intValue();
				return;
			}
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		super.mouseReleased(mouseX, mouseY, button);
		if (button == 0) {
			panelDragging = false;
			elementDragging = false;
		}
	}
}
