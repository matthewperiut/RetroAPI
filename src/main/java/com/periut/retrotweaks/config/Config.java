package com.periut.retrotweaks.config;

import com.periut.retroapi.config.Cat;
import com.periut.retroapi.config.ConfigTree;
import com.periut.retroapi.config.Opt;
import com.periut.retroapi.config.Scope;
import com.periut.retrotweaks.compat.Mods;

/**
 * Every RetroTweaks setting.
 *
 * <p>The merged mods each shipped their own flat config; keeping fifteen flat configs would have
 * meant fifteen places to look for "the fence option". Options are grouped here by what they affect
 * instead of by which mod they arrived from, and where two mods implemented the same behaviour they
 * collapse into a single option (the source mods are named in a comment so the lineage is not lost).
 *
 * <p>{@link Opt#source()} names a mod that provides the same feature. When that mod is installed the
 * option is forced off and greyed out rather than fighting it - see {@link ConfigTree.Option#push()}.
 *
 * <p>Read these from anywhere as plain fields, e.g. {@code Config.BLOCKS.fenceShapeFixes}.
 */
public final class Config {

	private Config() {}

	public static final Graphics GRAPHICS = new Graphics();
	public static final System SYSTEM = new System();
	public static final Interface INTERFACE = new Interface();
	public static final Hud HUD = new Hud();
	public static final Inventory INVENTORY = new Inventory();
	public static final Gameplay GAMEPLAY = new Gameplay();
	public static final Equipment EQUIPMENT = new Equipment();
	public static final Blocks BLOCKS = new Blocks();
	public static final Mobs MOBS = new Mobs();
	public static final World WORLD = new World();
	public static final Recipes RECIPES = new Recipes();
	public static final Bugfixes BUGFIXES = new Bugfixes();
	public static final Scoring SCORING = new Scoring();
	public static final FishingAndFood FISHING = new FishingAndFood();
	public static final Sounds SOUNDS = new Sounds();
	public static final Particles PARTICLES = new Particles();
	public static final Multiplayer MULTIPLAYER = new Multiplayer();

	/** The object the config tree, the JSON file and the screen are all reflected out of. */
	public static final class Root {
		@Cat(name = "Graphics", desc = "Render scale, resample filter and video settings", scope = Scope.CLIENT) public final Graphics graphics = GRAPHICS;
		@Cat(name = "System", desc = "Startup, saving and input", scope = Scope.CLIENT) public final System system = SYSTEM;
		@Cat(name = "Interface", desc = "Menus, options screens and title screen", scope = Scope.CLIENT) public final Interface ui = INTERFACE;
		@Cat(name = "HUD", desc = "In-game overlay and debug screen", scope = Scope.CLIENT) public final Hud hud = HUD;
		@Cat(name = "Inventory", desc = "Container clicking, dragging and shortcuts", scope = Scope.CLIENT) public final Inventory inventory = INVENTORY;
		@Cat(name = "Gameplay", desc = "Player interaction and old-version behaviour") public final Gameplay gameplay = GAMEPLAY;
		@Cat(name = "Blocks", desc = "Placement, tools and block behaviour") public final Blocks blocks = BLOCKS;
		@Cat(name = "Mobs & Entities", desc = "Mob behaviour, drops and vehicles") public final Mobs mobs = MOBS;
		@Cat(name = "World", desc = "Explosions, fire, flora and generation") public final World world = WORLD;
		@Cat(name = "Recipes", desc = "Crafting, smelting and fuels") public final Recipes recipes = RECIPES;
		@Cat(name = "Bugfixes", desc = "Vanilla bugs, off only if you want them back") public final Bugfixes bugfixes = BUGFIXES;
		@Cat(name = "Scoring", desc = "Extended score tracking and display", scope = Scope.CLIENT) public final Scoring scoring = SCORING;
		// FISHINFOODTWEAKS DISABLED - re-enable by uncommenting this line (and the other sites: grep -rn "FISHINFOODTWEAKS DISABLED").
		// The FishingAndFood class and the Config.FISHING instance stay declared, so every
		// Config.FISHING.* read elsewhere still compiles and reads its default; only the screen
		// entry and the JSON section disappear.
		// @Cat(name = "Fishing & Food", desc = "Fish sizes, types and food tooltips") public final FishingAndFood fishing = FISHING;
		@Cat(name = "Sounds", desc = "Extra sound effects", scope = Scope.CLIENT) public final Sounds sounds = SOUNDS;
		@Cat(name = "Particles", desc = "Turn individual particle types off", scope = Scope.CLIENT) public final Particles particles = PARTICLES;
		@Cat(name = "Multiplayer", desc = "Networking, skins and resources", scope = Scope.CLIENT) public final Multiplayer multiplayer = MULTIPLAYER;
	}

	public static final Root ROOT = new Root();

	// ================================================================ Graphics

	/**
	 * Render scale, resample filter, RGSS/Mipmaps and the video-settings toggles formerly under
	 * Interface. First category in the tree, ahead of System, so render tuning is the first thing a
	 * player finds.
	 *
	 * <p>Render scale, the filter cycle, RGSS and Mipmaps are deliberately NOT modelled as
	 * {@code @Opt}/{@code @Cat} fields here: they are RetroDragon 0.1.7+'s own live
	 * {@code RetroSettings} state (see {@link com.periut.retrotweaks.compat.ApiBridge}), not
	 * a RetroTweaks-owned value with a JSON-backed default - RetroDragon keeps that state itself and
	 * resets it to its own launch-time default every run, exactly like RGSS/Mipmaps already do. So
	 * {@link com.periut.retrotweaks.client.gui.ConfigScreen} special-cases this category the
	 * same way it already special-cases the HUD-element pages, and builds those rows itself instead
	 * of reflecting them off a field - see its "Graphics tab extras" section. Absent RetroDragon (or
	 * older than 0.1.7, which has RGSS/Mipmaps but not render scale), those rows simply do not
	 * appear, the same as the RGSS/Mipmaps split on the vanilla video screen.
	 *
	 * <p>All seven numeric video settings (fog density, cloud height, brightness, FOV, render
	 * distance, FPS limit, GUI scale) plus the clouds toggle get a real row here, bound straight
	 * through {@code Minecraft.options}' own {@code getFloat}/{@code setFloat}/{@code getString} -
	 * the same methods {@code GameOptionsMixin} is woven into and vanilla's own {@code SliderWidget}
	 * uses - so a row here and its vanilla-screen counterpart (render distance, FPS limit and GUI
	 * scale still replace a vanilla entry there too; see {@code VideoOptionsScreenMixin}) always show
	 * and store the exact same value. Each row greys out in lock-step with its own
	 * {@link VideoSettings} override flag (i.e. together with the vanilla-screen slider, whenever
	 * UniTweaks is installed and suppresses it) rather than staying live and quietly writing to a
	 * value {@code ModOptions.enabled} then ignores. See {@code ConfigScreen}'s "Graphics tab extras"
	 * section.
	 */
	public static final class Graphics {
		@Cat(name = "Video Settings", desc = "Extra sliders in the video options screen")
		public final VideoSettings video = new VideoSettings();
	}

	// ================================================================ System

	/** From UniTweaks (General). */
	public static final class System {
		@Opt(name = "Pause On Lost Focus", desc = "Pause singleplayer when the window loses focus", source = Mods.UNITWEAKS)
		public Boolean pauseOnLostFocus = true;

		@Opt(name = "Autosave Interval", desc = "Seconds between world saves", min = 1, max = 3600, source = Mods.UNITWEAKS)
		public Integer autosaveInterval = 30;

		@Opt(name = "Raw Input", desc = "Bypass OS mouse acceleration", source = Mods.UNITWEAKS)
		public Boolean rawInput = false;

		@Opt(name = "Disable Controller Init", desc = "LWJGL controller setup only causes trouble", restart = true, source = Mods.UNITWEAKS)
		public Boolean disableControllerInit = true;

		@Opt(name = "Disabled Dimensions", desc = "Dimension ids the server will not load", source = Mods.UNITWEAKS)
		public Integer[] disabledDimensions = new Integer[]{2};
	}

	// ================================================================ Interface

	/**
	 * From UniTweaks (User Interface), MojangFix and BetaTweaks.
	 *
	 * <p>Video Settings moved out to {@link Graphics#video} - see that class's doc for why.
	 */
	public static final class Interface {
		@Cat(name = "Version Text", desc = "The version string drawn in the corner")
		public final VersionText versionText = new VersionText();

		@Opt(name = "Show Quit Button", desc = "Adds a quit button to the title screen", source = Mods.UNITWEAKS)
		public Boolean showQuitButton = true;

		@Opt(name = "Improved Controls Menu", desc = "Scrollable, wider keybind list", source = Mods.UNITWEAKS)
		public Boolean improvedControlsMenu = true;

		@Opt(name = "Hide Achievement Toast", desc = "Stops the achievement popup appearing", source = Mods.UNITWEAKS)
		public Boolean hideAchievementToast = false;

		@Opt(name = "Achievements Done Returns To Menu", desc = "Instead of unpausing the game", source = Mods.UNITWEAKS)
		public Boolean achievementBackToMenu = true;

		// Off by default: a front-facing camera is a new mode b1.7.3 never had, not a fix, and it sits
		// in the middle of the vanilla F5 cycle where it would surprise someone who never asked for it.
		@Opt(name = "Front View Third Person", desc = "Adds a front-facing third person camera", source = Mods.UNITWEAKS)
		public Enums.FrontView frontViewThirdPerson = Enums.FrontView.DISABLED;
	}

	/**
	 * From UniTweaks (Video Settings).
	 *
	 * <p>Each of these gates whether RetroTweaks controls the named value AT ALL - not whether a
	 * control for it is drawn anywhere in particular. Renamed from "* Slider" (which read as "show a
	 * slider" when what it actually does is let RetroTweaks override vanilla) once the seven numeric
	 * settings got a real slider on the Graphics tab instead of just this enable-boolean: see
	 * {@link com.periut.retrotweaks.client.gui.ConfigScreen}'s "Graphics tab extras" section,
	 * which greys a tab row out using this same flag rather than leaving it live and silently
	 * ignored. Turning one of these off still does exactly what it always did - stand the feature
	 * down everywhere ({@code ModOptions.enabled}'s nine call sites) - only the field name changed.
	 *
	 * <p>Renaming a field changes the JSON key {@link com.periut.retroapi.config.ConfigTree}
	 * saves it under: an existing {@code retrotweaks.json} has the old key (e.g. {@code "fovSlider"}),
	 * which this reads as absent and therefore leaves at this field's default - {@code true} for
	 * every one renamed here, which is also what the old key held for anyone who had not turned it
	 * off. Anyone who HAD turned one off will find it back on once after upgrading and needs to
	 * turn it off again; there is no way to migrate a field rename automatically without keeping the
	 * old key around indefinitely, which was judged not worth it for a one-time, easily-noticed reset.
	 */
	public static final class VideoSettings {
		@Opt(name = "Brightness Override", desc = "Lets RetroTweaks control screen brightness beyond vanilla's own toggle", source = Mods.UNITWEAKS)
		public Boolean brightnessOverride = true;

		@Opt(name = "Cloud Height Override", desc = "Lets RetroTweaks draw clouds at a custom height", source = Mods.UNITWEAKS)
		public Boolean cloudHeightOverride = true;

		@Opt(name = "Clouds Toggle", desc = "Lets RetroTweaks turn clouds off entirely", source = Mods.UNITWEAKS)
		public Boolean cloudsToggle = true;

		@Opt(name = "Fog Density Override", desc = "Lets RetroTweaks control how far fog reaches", source = Mods.UNITWEAKS)
		public Boolean fogDensityOverride = true;

		@Opt(name = "FOV Override", desc = "Lets RetroTweaks add an FOV offset, and a Zoom key", source = Mods.UNITWEAKS)
		public Boolean fovOverride = true;

		@Opt(name = "GUI Scale Override", desc = "Lets RetroTweaks reach a GUI scale vanilla's four fixed steps cannot", source = Mods.UNITWEAKS)
		public Boolean guiScaleOverride = true;

		// RetroDragon is deliberately not a source here (unlike most other options it patches): it
		// only owns the numeric fps caps, which ModOptions.fpsLimitActive() stands down for - the
		// VSync stop keeps working under RetroDragon too (a driver swap interval, not a sleep-based
		// limiter, so it does not fight RetroDragon's own pacing). Naming it as a source would grey
		// the whole control out and make VSync unreachable, which would make this row lie about what
		// still works.
		@Opt(name = "FPS Limit Override", desc = "Caps the frame rate finer than vanilla's three steps, on either backend",
			source = Mods.UNITWEAKS)
		public Boolean fpsLimitOverride = true;

		@Opt(name = "Render Distance Override", desc = "Lets RetroTweaks raise render distance past vanilla's four steps", source = Mods.UNITWEAKS)
		public Boolean renderDistanceOverride = true;

		@Opt(name = "Vanilla Far Fog Values", desc = "Keeps b1.7.3 fog when render distance is raised", source = Mods.UNITWEAKS)
		public Boolean vanillaFarValues = false;
	}

	/** From UniTweaks (Version Text). Default flipped to off - see the README. */
	public static final class VersionText {
		@Opt(name = "Show Version Text Ingame", source = Mods.UNITWEAKS)
		public Boolean showVersionTextIngame = false;

		@Opt(name = "Unlicensed Copy", desc = "Appends the vanilla unlicensed-copy notice", source = Mods.UNITWEAKS)
		public Boolean unlicensedCopy = false;

		@Opt(name = "Enable Custom Version Text", source = Mods.UNITWEAKS)
		public Boolean enableCustomVersionText = false;

		@Opt(name = "Custom Version Text", source = Mods.UNITWEAKS)
		public String customVersionText = "Minecraft Beta 1.7.3 (RetroTweaks)";
	}

	// ================================================================ HUD

	/** From HudTweaks, UniTweaksTelsAddons (debug overlay) and MojangFix (debug screen). */
	public static final class Hud {
		@Cat(name = "HUD Positions", desc = "Move the hotbar, bars and messages")
		public final HudPositions positions = new HudPositions();

		@Cat(name = "Debug Overlay", desc = "Extra lines on the F3 screen")
		public final DebugOverlay debug = new DebugOverlay();

		// 100 = vanilla, 10 = 1s (the mixin doubles this to ticks and 20 ticks = 1s), so max = 600 is
		// a full minute of fade - already far past anything a chat message needs. 32000 was ~27 minutes.
		@Opt(name = "Chat Message Fade Time", desc = "100 = vanilla, 10 = one second", min = 0, max = 600)
		public Integer chatFadeTime = 100;

		@Opt(name = "Disable Crosshair")
		public Boolean disableCrosshair = false;

		@Opt(name = "Disable Vignette")
		public Boolean disableVignette = false;

		@Opt(name = "Draw Xbox X And Y Buttons", desc = "Console-style prompts on the hotbar")
		public Boolean drawXboxXAndYButtons = false;

		@Opt(name = "Hotbar Block Rendering Fix", desc = "Stops held blocks drawing over messages")
		public Boolean enableHotbarBlockRenderingFix = true;

		@Opt(name = "Hotbar Item Selection Tooltips", desc = "Names the item as you scroll the hotbar")
		public Boolean enableHotbarItemSelectionTooltips = false;

		// This field is ticks directly (20/s), no doubling - 40 = default is 2s. max = 200 is 10s,
		// already generous for a hotbar tooltip; 32000 was almost 27 minutes.
		@Opt(name = "Hotbar Item Selection Fade Time", desc = "40 = default", min = 0, max = 200)
		public Integer hotbarItemSelectionFadeTime = 40;
	}

	/** From HudTweaks. */
	public static final class HudPositions {
		// Declared first, and the value the six sections below name in their own @Cat(gateOption=...),
		// so it is the first thing a player sees on this page and the sections it controls sit right
		// beneath it - not the other way around, which is what originally hid the fact that only one
		// group of the six below is ever read (see InGameHudMixin.retrotweaks$render).
		@Opt(name = "HUD Positioning System", desc = "Controls which section below is used")
		public Enums.HudPositioning hudPositioningSystem = Enums.HudPositioning.SIMPLE;

		@Cat(name = "Simple Position", desc = "Moves the whole HUD as one block",
			gateOption = "hudPositioningSystem", gateValues = "SIMPLE")
		public final HudElement simple = new HudElement();

		@Cat(name = "Advanced: Hotbar", gateOption = "hudPositioningSystem", gateValues = "ADVANCED")
		public final HudElement hotbar = new HudElement();
		@Cat(name = "Advanced: Health Bar", gateOption = "hudPositioningSystem", gateValues = "ADVANCED")
		public final HudElement hearts = new HudElement();
		@Cat(name = "Advanced: Armor Bar", gateOption = "hudPositioningSystem", gateValues = "ADVANCED")
		public final HudElement armor = new HudElement();
		@Cat(name = "Advanced: Oxygen Bar", gateOption = "hudPositioningSystem", gateValues = "ADVANCED")
		public final HudElement oxygen = new HudElement();
		@Cat(name = "Advanced: Overlay Message", gateOption = "hudPositioningSystem", gateValues = "ADVANCED")
		public final HudElement overlayMessage = new HudElement();

		@Opt(name = "Overlay Messages Below Hotbar", desc = "Vertical offset for overlay text only")
		public Boolean putOverlayMessagesBelowHotbar = false;

		@Opt(name = "Status Bar Icons Below Hotbar", desc = "Vertical offset for the status bars only")
		public Boolean putStatusBarIconsBelowHotbar = false;
	}

	/**
	 * One positionable HUD element. HudTweaks declared six near-identical copies of this block;
	 * one shared class means adding a field (or fixing a bug in one) hits all six at once.
	 */
	public static final class HudElement {
		@Opt(name = "Visible")
		public Boolean visible = true;

		@Opt(name = "Horizontal Position")
		public Enums.HorizontalPosition horizontal = Enums.HorizontalPosition.CENTERED;

		// These offsets land straight on scaledWidth/scaledHeight (HudLayout / InGameHudMixin) - the
		// same space ScreenScaler produces, which under the normal AUTO gui scale stays in roughly the
		// 320-850 (width) / 240-480 (height) range for any real display. +-32000 needed a track well
		// over a hundred thousand pixels wide to resolve to single units; even the documented -32 for
		// the Xbox layout landed inside one slider pixel's worth of other values and was unreachable.
		// +-256 keeps every step of the 192px track worth ~2.7 units - enough to place an element
		// anywhere from dead center to off either edge on any of the sizes above, while -32 itself
		// falls on an exact slider pixel (224/512 * 192 = 84).
		@Opt(name = "Horizontal Offset", min = -256, max = 256)
		public Integer horizontalOffset = 0;

		@Opt(name = "Vertical Position")
		public Enums.VerticalPosition vertical = Enums.VerticalPosition.BOTTOM;

		@Opt(name = "Vertical Offset", desc = "Bottom with -32 matches the Xbox layout", min = -256, max = 256)
		public Integer verticalOffset = 0;
	}

	/** From UniTweaksTelsAddons, HudTweaks and MojangFix. */
	public static final class DebugOverlay {
		@Opt(name = "Coordinate Display", desc = "Hide or scramble coordinates for streaming")
		public Enums.CoordinateDisplay coordinateDisplay = Enums.CoordinateDisplay.SHOW;

		@Opt(name = "Show Biome")
		public Boolean showBiome = true;

		@Opt(name = "Show Day Counter")
		public Boolean showDayCounter = true;

		@Opt(name = "Show Hours Played")
		public Boolean showHoursPlayed = true;

		@Opt(name = "Show Light Level", desc = "Measured at the player's eye level")
		public Boolean showLightLevel = true;

		@Opt(name = "Show Slime Chunk")
		public Boolean showSlimeChunk = true;

		@Opt(name = "Show World Seed")
		public Boolean showWorldSeed = true;

		@Opt(name = "Additions Y Offset", min = -4096, max = 4096)
		public Integer additionsYOffset = 0;

		@Opt(name = "Disable Entity ID Tags", desc = "Hides the id text above entities", source = Mods.UNITWEAKS)
		public Boolean disableEntityIdTags = true;

		@Opt(name = "Modern Debug Graph", desc = "Adds the pie chart and frame graph")
		public Boolean enableDebugGraph = true;

		@Opt(name = "Debug Graph Needs Shift+F3", desc = "Off = the graph key alone toggles it")
		public Boolean debugGraphModernToggle = true;
	}

	// ================================================================ Inventory

	/** From InventoryTweaks, MouseTweaks and Glass Inventory Tweaks. */
	public static final class Inventory {
		@Cat(name = "Crafting Result", desc = "Shortcuts on the crafting output slot")
		public final CraftingResult crafting = new CraftingResult();

		@Cat(name = "Modern Clicking", desc = "Bring container clicks up to modern parity")
		public final ModernClicking modern = new ModernClicking();

		@Cat(name = "Click And Drag", desc = "Drag to distribute or collect items")
		public final ClickDrag drag = new ClickDrag();

		@Cat(name = "Scroll Wheel", desc = "Scroll over a slot to move items")
		public final ScrollWheel scroll = new ScrollWheel();

		@Opt(name = "Use 2x2 Grid As Inventory", desc = "The player crafting grid keeps its contents")
		public Boolean craftingGridAsInventory = false;
	}

	/** From InventoryTweaks. */
	public static final class CraftingResult {
		@Opt(name = "[Ctrl-Click] Crafting", desc = "Craft one stack into the inventory")
		public Boolean ctrlClickCrafting = true;

		@Opt(name = "[Right-Click] Crafting", desc = "Craft one stack onto the cursor")
		public Boolean rightClickCrafting = true;

		@Opt(name = "[Shift-Click] Crafting", desc = "Craft as many as will fit")
		public Boolean shiftClickCrafting = true;

		@Opt(name = "Stop [Shift-Click] When Item Changes", desc = "Shift crafting sticks to one item type")
		public Boolean stopShiftClickWhenItemChanges = true;
	}

	/** From InventoryTweaks and Glass Inventory Tweaks. */
	public static final class ModernClicking {
		@Opt(name = "[Shift-Click] Into Armor Slots")
		public Boolean shiftClickIntoArmorSlots = true;

		@Opt(name = "[Shift-Click] Into Furnaces")
		public Boolean shiftClickIntoFurnaces = true;

		@Opt(name = "Double [Left-Click] Collects Stack", desc = "Sweeps matching items onto the cursor")
		public Boolean doubleClickCollect = true;

		@Opt(name = "[Number Keys] Swap To Hotbar", desc = "Hover a slot or hold an item")
		public Boolean numberKeyHotbarSwap = true;

		@Opt(name = "[Drop Key] Drops Hovered Item", desc = "Only when the cursor is empty")
		public Boolean dropKeyInInventory = true;

		@Opt(name = "[Ctrl + Drop Key] Drops Whole Stack")
		public Boolean ctrlDropWholeStack = true;

		@Opt(name = "[Shift-Click] Into Dispensers", desc = "Server must run RetroTweaks too")
		public Boolean multiplayerShiftClickDispensers = false;

		@Opt(name = "[Shift-Click] Slot Priority", desc = "Hotbar/armor first in chests and crafting tables")
		public Boolean prioritySlotShiftClick = true;
	}

	/** From InventoryTweaks and MouseTweaks, which shipped overlapping copies of these. */
	public static final class ClickDrag {
		@Opt(name = "Enable [Left-Click + Drag]", desc = "Spread the held stack evenly")
		public Boolean leftClickDrag = true;

		@Opt(name = "Enable [Right-Click + Drag]", desc = "Drop one item per slot")
		public Boolean rightClickDrag = true;

		@Opt(name = "Drag Graphics", desc = "Highlight every slot the drag has crossed")
		public Boolean dragGraphics = true;

		@Opt(name = "Prefer [Shift-Click] Over [LMB + Drag]")
		public Boolean lmbPreferShiftClick = true;

		@Opt(name = "Prefer [Shift-Click] Over [RMB + Drag]")
		public Boolean rmbPreferShiftClick = true;

		@Opt(name = "Empty Cursor [Shift + LMB + Drag]", desc = "Shift-clicks items of any type")
		public Boolean lmbDragShiftClickAny = true;

		@Opt(name = "Held Item [Shift + LMB + Drag]", desc = "Shift-clicks items of the held type")
		public Boolean lmbDragShiftClickHeld = true;

		@Opt(name = "[RMB + Drag] Over Filled Slots", desc = "Tops slots up while dragging")
		public Boolean rmbDragOverFilledSlots = false;

		@Opt(name = "[LMB + Drag] Picks Up Items", desc = "Sweep the cursor to collect")
		public Boolean lmbDragPickUp = true;
	}

	/** From InventoryTweaks, MouseTweaks and Glass Inventory Tweaks. */
	public static final class ScrollWheel {
		@Opt(name = "[Scroll Wheel] Move Between Inventories", desc = "MouseTweaks' wheel tweak; works in multiplayer")
		public Boolean inventoryTransferScroll = true;

		@Opt(name = "Between-Inventory Scroll Direction", desc = "Which way the wheel sends items")
		public Enums.WheelDirection transferScrollDirection = Enums.WheelDirection.NORMAL;

		@Opt(name = "Between-Inventory Search Order", desc = "Which end of the other inventory is drained first")
		public Enums.WheelSearchOrder transferScrollSearchOrder = Enums.WheelSearchOrder.LAST_TO_FIRST;

		@Opt(name = "Enable Scroll Wheel Tweaks", desc = "Cursor/slot transfer; used when holding a stack, singleplayer only")
		public Boolean enableScrollWheelTweaks = true;

		@Opt(name = "Invert Cursor/Slot Direction")
		public Boolean invertCursorSlotDirection = false;

		@Opt(name = "[Scroll Wheel] Slot Priority", desc = "Peels one item toward the best slot instead; singleplayer only")
		public Boolean priorityRoutedScroll = false;
	}

	// ================================================================ Gameplay

	/** From MiscTweaks. Restart required for all of these - they change items at construction. */
	public static final class Equipment {
		@Opt(name = "Bows Have Durability", desc = "384 uses, as in modern Minecraft")
		public Boolean bowsHaveDurability = false;

		@Opt(name = "Equalize Base Armor Durability", desc = "Every slot of a material lasts the same")
		public Boolean equalizeBaseArmorDurability = false;

		@Opt(name = "Modern Armor Defense Points", desc = "Protection varies by material")
		public Boolean modernArmorDefensePoints = false;
	}

	/** From GameplayEssentials, UniTweaks (Gameplay), BetaTweaks and AnnoyanceFix. */
	public static final class Gameplay {
		@Cat(name = "Old Features", desc = "Behaviour from earlier versions, all off by default")
		public final OldFeatures oldFeatures = new OldFeatures();

		@Cat(name = "Equipment", desc = "Armor and bows; restart required")
		public final Equipment equipment = EQUIPMENT;

		@Opt(name = "Shift Placing", desc = "Place blocks against interactive blocks", source = Mods.UNITWEAKS)
		public Boolean shiftPlacing = true;

		@Opt(name = "Shift Placing Blacklist", desc = "Block ids that keep their sneak action", source = Mods.UNITWEAKS)
		public Integer[] shiftPlacingBlacklist = new Integer[0];

		@Opt(name = "Disable Block Interactions Keybind", desc = "Multiplayer always uses sneak")
		public Boolean disableBlockInteractionsKeybind = true;

		@Opt(name = "Right-Click To Equip Armor", source = Mods.UNITWEAKS)
		public Boolean rightClickEquipArmor = true;

		@Opt(name = "No Food Wastage", desc = "Cannot eat at full health", source = Mods.UNITWEAKS)
		public Boolean disableEatingAtMaxHealth = false;

		@Opt(name = "Pick Block From Inventory", desc = "Searches the whole inventory, not just the hotbar", source = Mods.UNITWEAKS)
		public Boolean pickBlockFromInventory = true;

		// source = UNITWEAKS: UniTweaks' pickblockfix rewrites the picked id at the IDENTICAL
		// injection point (@ModifyVariable at the same field read in handlePickBlock) and recomputes
		// it from scratch, so the two implementations cannot layer - whichever applied last silently
		// won. MinecraftPickBlockMixin therefore stands down with this option, and UniTweaksBridge
		// drives UniTweaks' bugfixes.pickBlockFix from this row instead. What UniTweaks' version does
		// not cover (boat/minecart/painting picking, the extra block->item table) is lost on such
		// installs - the price of one owner per injection point.
		@Opt(name = "Pick Block Fixes", desc = "Makes more blocks pickable", source = Mods.UNITWEAKS)
		public Boolean pickBlockFixes = true;

		// source = UNITWEAKS with no bridge binding, so the row greys out: with UniTweaks installed
		// both of this option's consumers (MinecraftPickBlockMixin and PlayerInventoryPickBlockMixin)
		// stand down, and UniTweaks' pick block has no equivalent knob to hand the value to.
		@Opt(name = "Pick Block Behavior", source = Mods.UNITWEAKS)
		public Enums.PickBlock pickBlockBehavior = Enums.PickBlock.CHECK_META;

		@Opt(name = "Step Assist", desc = "Step up a full block without jumping", source = Mods.UNITWEAKS)
		public Boolean stepAssist = false;

		@Opt(name = "Fence Jumping", desc = "Needs the fence bounding box fix on", source = Mods.UNITWEAKS)
		public Boolean fenceJumping = false;

		@Opt(name = "Bed Behavior When Used")
		public Enums.BedBehavior bedBehavior = Enums.BedBehavior.VANILLA;

		@Opt(name = "Disable Sleep Mob Spawning", desc = "Sleeping no longer spawns the ambush", source = Mods.UNITWEAKS)
		public Boolean disableSleepSpawning = false;
	}

	/** From BetaTweaks and UniTweaks (Old Features). */
	public static final class OldFeatures {
		@Opt(name = "Allow Gaps In Ladders", source = Mods.UNITWEAKS)
		public Boolean allowGapsInLadders = false;

		@Opt(name = "Elevator Boats", desc = "Boats climb when pushed into a wall", source = Mods.UNITWEAKS)
		public Boolean elevatorBoats = false;

		@Opt(name = "Minecart Boosters", source = Mods.UNITWEAKS)
		public Boolean minecartBoosters = false;

		@Opt(name = "Hoe Grass For Seeds", source = Mods.UNITWEAKS)
		public Boolean hoeGrassForSeeds = false;

		@Opt(name = "Punch Sheep For Wool", source = Mods.UNITWEAKS)
		public Boolean punchSheepForWool = false;

		@Opt(name = "Punch TNT To Ignite", source = Mods.UNITWEAKS)
		public Boolean punchTntToIgnite = false;

		@Opt(name = "Punch Primed TNT To Defuse", source = Mods.UNITWEAKS)
		public Boolean punchTntToDefuse = false;

		@Opt(name = "Milk Squids")
		public Boolean milkSquids = false;

		@Opt(name = "Pigs Drop Brown Mushrooms")
		public Boolean pigsDropBrownMushrooms = false;

		// Lives here rather than beside the other movement options: it is not a fix or a tweak, it is
		// literally "how an older version decided you were in a fluid", which is what this section is.
		@Opt(name = "Enter Fluids By South-East Corner", desc = "Restores the pre-1.8 corner rule")
		public Boolean allowSouthEastRule = false;
	}

	// ================================================================ Blocks

	/** From AnnoyanceFix, GameplayEssentials, MiscTweaks and UniTweaks (Tweaks). */
	public static final class Blocks {
		@Cat(name = "Axe Effectivity", desc = "Restart required", source = Mods.UNITWEAKS)
		public final AxeEffectivity axes = new AxeEffectivity();

		@Cat(name = "Pickaxe Effectivity", desc = "Restart required", source = Mods.UNITWEAKS)
		public final PickaxeEffectivity pickaxes = new PickaxeEffectivity();

		@Cat(name = "Shovel Effectivity", desc = "Restart required")
		public final ShovelEffectivity shovels = new ShovelEffectivity();

		@Opt(name = "Fence Placement Fixes", desc = "Place fences on any side", source = Mods.UNITWEAKS)
		public Boolean fencePlacementFixes = true;

		@Opt(name = "Fence Shape Fixes", desc = "Bounding box follows the connections", source = Mods.UNITWEAKS)
		public Boolean fenceShapeFixes = true;

		@Opt(name = "Fences Connect To Blocks", source = Mods.UNITWEAKS)
		public Boolean fencesConnectBlocks = true;

		@Opt(name = "Pumpkin Placement Fixes", desc = "Pumpkins place like a normal block", source = Mods.UNITWEAKS)
		public Boolean pumpkinPlacementFixes = true;

		@Opt(name = "Slab Placement Fixes", desc = "Merge slabs into a double slab")
		public Boolean slabPlacementFixes = true;

		@Opt(name = "Stair Fixes", desc = "Stairs drop themselves and place correctly", source = Mods.UNITWEAKS)
		public Boolean stairFixes = true;

		@Opt(name = "Trapdoors Without Support", source = Mods.UNITWEAKS)
		public Boolean trapdoorsWithoutSupport = true;

		@Opt(name = "Pressure Plates On Fences", source = Mods.UNITWEAKS)
		public Boolean pressurePlatesOnFences = true;

		@Opt(name = "Sugar Cane On Sand", source = Mods.UNITWEAKS)
		public Boolean sugarCaneOnSand = true;

		@Opt(name = "Stackable Chests", desc = "Chests may sit directly on each other", source = Mods.UNITWEAKS)
		public Boolean stackableChests = true;

		@Opt(name = "Chests Open When Blocked", desc = "A block above no longer seals a chest")
		public Boolean chestsOpenWhenBlocked = false;

		@Opt(name = "Bookshelves Drop 3 Books", source = Mods.UNITWEAKS)
		public Boolean bookshelvesDropBooks = true;

		@Opt(name = "Cobweb Fixes", desc = "Shears and swords collect cobwebs")
		public Boolean cobwebFixes = true;

		@Opt(name = "Plant Replacement Fixes", desc = "Place blocks straight into tall grass")
		public Boolean plantReplacementFixes = true;

		@Opt(name = "Log Rotation", desc = "Off converts rotated logs back to oak")
		public Boolean logRotation = false;

		@Opt(name = "Player Placed Leaves Persist", desc = "Placed leaves stop decaying")
		public Boolean playerPlacedLeafPersistence = false;

		@Opt(name = "Glue Trapdoors With A Slimeball", desc = "Consumes the slimeball")
		public Boolean glueTrapdoorsWithSlimeballs = false;

		@Opt(name = "Color Signs With Dye", desc = "Consumes the dye")
		public Boolean colorSignsWithDye = false;

		@Opt(name = "Flint And Steel Fixes", desc = "No durability loss on a failed light", source = Mods.UNITWEAKS)
		public Boolean flintAndSteelFixes = true;

		@Opt(name = "Water Fixes", desc = "Sources form correctly next to falling water", source = Mods.UNITWEAKS)
		public Boolean waterFixes = true;

		@Opt(name = "Lava Fixes", desc = "Lava vanishes when its source is gone", source = Mods.UNITWEAKS)
		public Boolean lavaFixes = true;
	}

	/** From AnnoyanceFix. */
	public static final class AxeEffectivity {
		@Opt(name = "Workbench") public Boolean workbench = true;
		@Opt(name = "Noteblock") public Boolean noteblock = true;
		@Opt(name = "Wood Door") public Boolean woodDoor = true;
		@Opt(name = "Ladders") public Boolean ladders = true;
		@Opt(name = "Signs") public Boolean signs = true;
		@Opt(name = "Wood Pressure Plate") public Boolean woodPressurePlate = true;
		@Opt(name = "Jukebox") public Boolean jukebox = true;
		@Opt(name = "Wood Stairs") public Boolean woodStairs = true;
		@Opt(name = "Fence") public Boolean fence = true;
		@Opt(name = "Pumpkin") public Boolean pumpkin = true;
		@Opt(name = "Jack o' Lantern") public Boolean jackOLantern = true;
		@Opt(name = "Trapdoor") public Boolean trapdoor = true;
	}

	/** From AnnoyanceFix. */
	public static final class PickaxeEffectivity {
		@Opt(name = "Dispenser") public Boolean dispenser = true;
		@Opt(name = "Normal Rails") public Boolean normalRails = true;
		@Opt(name = "Detector Rails") public Boolean detectorRails = true;
		@Opt(name = "Golden Rails") public Boolean goldenRails = true;
		@Opt(name = "Furnace") public Boolean furnace = true;
		@Opt(name = "Lit Furnace") public Boolean furnaceLit = true;
		@Opt(name = "Cobblestone Stairs") public Boolean cobblestoneStairs = true;
		@Opt(name = "Stone Pressure Plate") public Boolean stonePressurePlate = true;
		@Opt(name = "Iron Door") public Boolean ironDoor = true;
		@Opt(name = "Redstone Ore") public Boolean redstoneOre = true;
		@Opt(name = "Lit Redstone Ore") public Boolean redstoneOreLit = true;
		@Opt(name = "Stone Button") public Boolean stoneButton = true;
		@Opt(name = "Bricks") public Boolean bricks = true;
		@Opt(name = "Mob Spawner") public Boolean mobSpawner = true;
	}

	/** From AnnoyanceFix and UniTweaksTelsAddons. */
	public static final class ShovelEffectivity {
		@Opt(name = "Soul Sand") public Boolean soulSand = true;
	}

	// ================================================================ Mobs

	/** From AnnoyanceFix, MiscTweaks, BetaTweaks and UniTweaks. */
	public static final class Mobs {
		@Cat(name = "Better Burning", desc = "Fire spreads between entities")
		public final BetterBurning betterBurning = new BetterBurning();

		@Cat(name = "Farmland Trampling")
		public final FarmlandTrampling trampling = new FarmlandTrampling();

		// Vanilla by default: an invincible boat is a real quality-of-life change, but it is a change,
		// and this mod's defaults are meant to leave b1.7.3 behaving like b1.7.3 until asked otherwise.
		@Opt(name = "Boat Collision Behavior")
		public Enums.BoatCollision boatCollisionBehavior = Enums.BoatCollision.VANILLA;

		@Opt(name = "Boats Drop Themselves", source = Mods.UNITWEAKS)
		public Boolean boatDropFixes = true;

		@Opt(name = "Vehicle Logout/Login Fixes", desc = "Stops boats and carts eating you on relog")
		public Boolean vehicleLogoutLoginFixes = true;

		@Opt(name = "Expand Chicken Hitbox", desc = "Matches the modern hitbox", source = Mods.UNITWEAKS)
		public Boolean expandChickenHitbox = true;

		@Opt(name = "Pig Saddle Drop Fix", source = Mods.UNITWEAKS)
		public Boolean pigSaddleDropFix = true;

		@Opt(name = "Disable Colored Sheep Spawning", desc = "Sheep always spawn white")
		public Boolean disableColoredSheepSpawning = false;

		// Replaces the feather rather than adding a second drop beside it: a zombie carrying a feather
		// AND something else was never the point - the point is choosing what a zombie leaves behind.
		// The count roll is untouched, so this only ever swaps the item vanilla would have dropped.
		@Opt(name = "Zombies Drop A Different Item", desc = "Replaces the feather rather than adding to it")
		public Boolean zombieDropItem = false;

		@Opt(name = "Zombies Drop", desc = "Feathers is what vanilla drops")
		public Enums.ZombieDrop zombieDropChoice = Enums.ZombieDrop.FEATHER;

		@Opt(name = "Pigmen Drop An Extra Item")
		public Boolean pigmanDropItem = false;

		@Opt(name = "Pigman Extra Drop")
		public Enums.PigmanDrop pigmanDropChoice = Enums.PigmanDrop.COOKED_PORKCHOP;

		@Opt(name = "Ghast Fireballs Insta-Kill Ghasts")
		public Boolean ghastFireballsKillGhasts = false;
	}

	/**
	 * From UniTweaks (Better Burning).
	 *
	 * <p>{@link #enabled} governs the four fire-spreading options below it, not the whole section:
	 * {@link #igniteEntitiesWithFlintAndSteel} moved in here because setting a mob alight is what this
	 * section is about, but it is a self-contained item behaviour with nothing to spread, and gating it
	 * behind a switch it does not need would leave it looking on while quietly doing nothing.
	 */
	public static final class BetterBurning {
		// Lives here, not under Blocks: it is a mob interaction, and Blocks was only ever where it
		// landed because the rest of the flint and steel handling is block placement.
		@Opt(name = "Ignite Entities With Flint And Steel", desc = "Right-click a mob to set it alight", source = Mods.UNITWEAKS)
		public Boolean igniteEntitiesWithFlintAndSteel = false;

		@Opt(name = "Enable Better Burning", desc = "Master switch for the fire spreading below", source = Mods.UNITWEAKS)
		public Boolean enabled = false;

		@Opt(name = "Burning Skeletons Shoot Fire Arrows", source = Mods.UNITWEAKS)
		public Boolean skeletonsBurningArrows = true;

		@Opt(name = "Fire Arrow Chance", min = 0, max = 100, source = Mods.UNITWEAKS)
		public Integer skeletonBurningArrowChance = 70;

		@Opt(name = "Burning Arrows Set Entities On Fire", source = Mods.UNITWEAKS)
		public Boolean burningArrowsSetOnFire = true;

		@Opt(name = "Burning Entities Spread Fire", source = Mods.UNITWEAKS)
		public Boolean burningEntitySpread = true;

		@Opt(name = "Fire Spread Chance", min = 0, max = 100, source = Mods.UNITWEAKS)
		public Integer burningEntitySpreadChance = 30;
	}

	/** From MiscTweaks. */
	public static final class FarmlandTrampling {
		@Opt(name = "Disable Trampling Entirely", desc = "Nothing tramples farmland")
		public Boolean disableTrampling = false;

		@Opt(name = "No Trampling When Barefoot", desc = "Player without boots only")
		public Boolean noTramplingBarefoot = false;

		@Opt(name = "No Trampling With Leather Boots", desc = "Player in leather boots only")
		public Boolean noTramplingLeatherBoots = false;
	}

	// ================================================================ World

	/** From MiscTweaks, BetaTweaks and UniTweaks. */
	public static final class World {
		@Cat(name = "Explosions", desc = "What TNT, creepers and ghasts destroy")
		public final Explosions explosions = new Explosions();

		@Cat(name = "Fire")
		public final Fire fire = new Fire();

		@Cat(name = "Flora", desc = "Leaves, grass and bushes")
		public final Flora flora = new Flora();

		// Off by default: b1.7.3 sponges do nothing at all, so this is a new behaviour rather than a fix.
		@Opt(name = "Sponge Soaks Up Water", desc = "Clears a 5x5 cube when placed")
		public Boolean spongeSoaksUpWater = false;
	}

	/** From MiscTweaks. */
	public static final class Explosions {
		@Opt(name = "Block Drop Chance", desc = "0.0 = nothing drops, 1.0 = everything", min = 0, max = 1)
		public Float blockDropChance = 0.3F;

		@Opt(name = "Disable All Block Breaking")
		public Boolean disableAllBlockBreaking = false;

		@Opt(name = "Creepers Do Not Break Blocks")
		public Boolean disableCreeperBlockBreaking = false;

		@Opt(name = "Ghasts Do Not Break Blocks")
		public Boolean disableGhastBlockBreaking = false;

		@Opt(name = "Disable TNT Block Breaking")
		public Boolean disableTntBlockBreaking = false;

		@Opt(name = "Defuse TNT With Shears", desc = "Left-click primed TNT holding shears")
		public Boolean defuseTntWithShears = false;
	}

	/** From MiscTweaks and BetaTweaks. */
	public static final class Fire {
		@Opt(name = "Spread Fire Infinitely", desc = "Fire never burns out on its own")
		public Boolean infiniteFireSpread = false;

		@Opt(name = "Fire Spread Tick Rate", desc = "40 = beta default, 10 = alpha", min = 1, max = 36863)
		public Integer fireSpreadTickRate = 40;

		@Opt(name = "Snowballs Extinguish Fire")
		public Boolean snowballsExtinguishFire = false;

		@Opt(name = "Fire Turns Grass Into Dirt")
		public Boolean fireTurnsGrassIntoDirt = false;

		@Opt(name = "Ghast Explosions Cause Fire", desc = "Off removes the fire, not the explosion")
		public Boolean ghastExplosionsCauseFire = true;

		@Opt(name = "Lightning Causes Fire")
		public Boolean lightningCausesFire = true;
	}

	/** From MiscTweaks, BetaTweaks and UniTweaks. */
	public static final class Flora {
		@Opt(name = "Fast Leaf Decay", source = Mods.UNITWEAKS)
		public Boolean fastLeafDecay = false;

		@Opt(name = "Minimum Decay Time", desc = "Ticks", min = 1, max = 1200, source = Mods.UNITWEAKS)
		public Integer minimumDecayTime = 10;

		@Opt(name = "Maximum Decay Time", desc = "Ticks", min = 1, max = 1200, source = Mods.UNITWEAKS)
		public Integer maximumDecayTime = 25;

		@Opt(name = "Apple Drop Chance", desc = "0 = off, 0.5 = the modern 0.5% chance", min = 0, max = 1)
		public Float appleDropChance = 0.0F;

		// The whole tall-plant variant story: the sheared drop keeping its meta, the three metas being
		// real item subtypes (own sprite, own name, own tint), placing back what you picked up, and the
		// minecraft:dead_shrub/short_grass/fern identifiers an installed API can resolve.
		//
		// WORLD scope, and deliberately not sourced to UniTweaks: this changes what a block drops and
		// what an item places, so a client running it against a server that is not would place a fern
		// and watch the server put a dead shrub there instead.
		@Opt(name = "Tall Grass Items", desc = "Fern, short grass and dead shrub are separate items")
		public Boolean tallGrassItems = true;

		// On by default, unlike the other three "shears collect" options: with Tall Grass Items above it
		// is no longer a curiosity that hands you a stack of dead shrubs whatever you sheared, but the
		// only way to obtain a fern or a short grass at all - and shears are already the tool the modern
		// game uses for exactly this.
		@Opt(name = "Shears Collect Tall Grass", desc = "Short grass and dead shrubs; ferns below", source = Mods.UNITWEAKS)
		public Boolean shearsCollectTallGrass = true;

		// Split out from Shears Collect Tall Grass rather than folded into it: wanting ferns obtainable
		// is not the same wish as wanting every patch of short grass to become a stack of grass items.
		// This drops RetroTweaks' own fern (block 31, meta 2) and needs no other mod - it used to hand
		// the drop to BHCreative's separately registered fern whenever that mod was installed, which
		// meant the same shears gave you different things depending on your mod list. If BHCreative is
		// present its fern simply exists alongside this one.
		@Opt(name = "Shears Collect Ferns")
		public Boolean shearsCollectFern = true;

		@Opt(name = "Shears Collect Dead Bushes")
		public Boolean shearsCollectDeadBush = true;

		@Opt(name = "Dead Bushes Drop Sticks", desc = "Same chance as seeds from grass")
		public Boolean deadBushesDropSticks = false;

		@Opt(name = "Hide Long Grass", desc = "Reload the world to take effect")
		public Boolean hideLongGrass = false;

		@Opt(name = "Hide Dead Shrubs", desc = "Reload the world to take effect")
		public Boolean hideDeadShrubs = false;

		@Opt(name = "Disable Tall Grass Generation", source = Mods.UNITWEAKS)
		public Boolean disableTallGrassGeneration = false;

		@Opt(name = "Disable Dead Bush Generation", source = Mods.UNITWEAKS)
		public Boolean disableDeadBushGeneration = false;

		@Opt(name = "Beta 1.8 Leaves Rendering", source = Mods.UNITWEAKS)
		public Boolean beta18LeavesRendering = false;
	}

	// ================================================================ Recipes

	/** From MostlyModernRecipes, NowObtainableRecipes, GameplayEssentials, AnnoyanceFix and UniTweaks. */
	public static final class Recipes {
		@Cat(name = "Modern", desc = "Recipes as they are in modern Minecraft")
		public final ModernRecipes modern = new ModernRecipes();

		@Cat(name = "Tweaked", desc = "Recipes changed rather than modernised")
		public final TweakedRecipes tweaked = new TweakedRecipes();

		@Cat(name = "Obtainable", desc = "Craft things b1.7.3 gives no way to get")
		public final ObtainableRecipes obtainable = new ObtainableRecipes();

		@Cat(name = "Stack Sizes", desc = "Restart required")
		public final StackSizes stackSizes = new StackSizes();

		@Opt(name = "Enable Recipe Tweaks", desc = "Master switch for this whole section")
		public Boolean enableRecipes = true;

		@Opt(name = "More Wooden Items Burn As Fuel")
		public Boolean furnaceFuels = true;

		// Split in two: these were one switch, which meant anyone who wanted armor repair had to accept
		// tool repair as well (and vice versa). They are separate crafting behaviours on separate item
		// classes, so they get separate switches - see CraftingRecipeManagerMixin, which picks the flag
		// from the class of the pair actually in the grid.
		@Opt(name = "Repair Weapons And Tools In The Crafting Grid", desc = "Combine two damaged tools of a kind")
		public Boolean toolRepair = true;

		@Opt(name = "Repair Armor In The Crafting Grid", desc = "Combine two damaged pieces of a kind")
		public Boolean armorRepair = true;
	}

	/** From MostlyModernRecipes and UniTweaks. */
	public static final class ModernRecipes {
		@Opt(name = "Shapeless Flint And Steel") public Boolean shapelessFlintAndSteel = true;
		@Opt(name = "Shapeless Mushroom Stew") public Boolean shapelessMushroomStew = true;
		@Opt(name = "Shapeless Chest Minecart") public Boolean shapelessChestMinecart = true;
		@Opt(name = "Shapeless Furnace Minecart") public Boolean shapelessFurnaceMinecart = true;
		@Opt(name = "Shapeless Sticky Piston") public Boolean shapelessStickyPiston = true;
		@Opt(name = "Books Require Leather") public Boolean booksRequireLeather = false;
		@Opt(name = "Wool Redyeing") public Boolean woolRedyeing = true;
		@Opt(name = "6 Slabs Per Craft") public Boolean sixSlabsPerCraft = true;
		@Opt(name = "Button Costs 1 Stone") public Boolean oneStonePerButton = true;
		@Opt(name = "Modern Fence Recipe", desc = "4 planks and 2 sticks make 3") public Boolean modernFenceRecipe = true;
		@Opt(name = "Snow Layer Recipe") public Boolean snowLayerRecipe = true;
		@Opt(name = "3 Ladders Per Craft") public Boolean threeLaddersPerCraft = true;
		@Opt(name = "3 Doors Per Craft") public Boolean threeDoorsPerCraft = true;
		@Opt(name = "3 Signs Per Craft") public Boolean threeSignsPerCraft = true;
		@Opt(name = "Modern Golden Apple", desc = "8 ingots, not 8 blocks") public Boolean modernGoldenApple = true;
	}

	/** From GameplayEssentials and UniTweaks. */
	public static final class TweakedRecipes {
		@Opt(name = "Shapeless Jack o' Lantern") public Boolean shapelessJackOLantern = true;
		@Opt(name = "Stairs Per Craft", min = 1, max = 16) public Integer stairsPerCraft = 4;
		@Opt(name = "Trapdoors Per Craft", min = 1, max = 8) public Integer trapdoorsPerCraft = 2;
	}

	/** From NowObtainableRecipes and UniTweaks. */
	public static final class ObtainableRecipes {
		@Opt(name = "Craftable Grass Block") public Boolean grass = false;
		@Opt(name = "Craftable Cobweb") public Boolean cobweb = false;
		@Opt(name = "Craftable Fire") public Boolean fire = false;
		@Opt(name = "Craftable Apple") public Boolean apple = false;
		@Opt(name = "Craftable Dead Bush") public Boolean deadBush = false;
		@Opt(name = "Craftable Sponge") public Boolean sponge = false;
		@Opt(name = "Craftable Ice") public Boolean ice = false;
		@Opt(name = "Craftable Double Stone Slab") public Boolean doubleSlab = false;
		@Opt(name = "Craftable Coal Ore", desc = "8 coal around a stone") public Boolean coalOre = false;
		@Opt(name = "Craftable Iron Ore") public Boolean ironOre = false;
		@Opt(name = "Craftable Gold Ore") public Boolean goldOre = false;
		@Opt(name = "Craftable Lapis Ore") public Boolean lapisOre = false;
		@Opt(name = "Craftable Diamond Ore") public Boolean diamondOre = false;
		@Opt(name = "Craftable Redstone Ore") public Boolean redstoneOre = false;
	}

	/** From UniTweaksTelsAddons. */
	public static final class StackSizes {
		@Opt(name = "Modern Wood Door Stack Size", desc = "64 instead of 1") public Boolean woodDoor = false;
		@Opt(name = "Modern Iron Door Stack Size", desc = "64 instead of 1") public Boolean ironDoor = false;
		@Opt(name = "Modern Sign Stack Size", desc = "16 instead of 1") public Boolean sign = false;
	}

	// ================================================================ Bugfixes

	/** From UniTweaks (Bugfixes), MojangFix, AnnoyanceFix and GameplayEssentials. */
	public static final class Bugfixes {
		@Opt(name = "Bit Depth Fix", desc = "24-bit buffer; fixes AMD banding", restart = true,
			source = {Mods.UNITWEAKS, Mods.RETRODRAGON}, scope = Scope.CLIENT)
		public Boolean bitDepthFix = true;

		@Opt(name = "Far Lands Jitter Fix", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean farLandsJitterFix = true;

		@Opt(name = "Slime Split Fix", desc = "Slimes split even when killed outright", source = Mods.UNITWEAKS)
		public Boolean slimeSplitFix = true;

		@Opt(name = "Nightmare Pathfinding Fix", source = Mods.UNITWEAKS)
		public Boolean nightmarePathfindingFix = true;

		@Opt(name = "Multiplayer Entity Jitter Fix", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean multiplayerEntityJitterFix = true;

		@Opt(name = "Boat Dismount Fix", desc = "Stops you falling through the boat", source = Mods.UNITWEAKS)
		public Boolean boatDismountFix = true;

		@Opt(name = "Sleeping Camera Rotation Fix", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean sleepingCameraRotationFix = true;

		@Opt(name = "Wooden Slab Mining Fix", desc = "Mineable by hand and axe", source = Mods.UNITWEAKS)
		public Boolean woodenSlabMiningFix = true;

		@Opt(name = "Liquid Block Drop Fix", desc = "Torches under water still drop", source = Mods.UNITWEAKS)
		public Boolean liquidBlockDropFix = true;

		@Opt(name = "Bow Held Fix", desc = "Bows are held the right way round", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean bowHeldFix = true;

		@Opt(name = "Leggings When Riding Fix", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean leggingsWhenRidingFix = true;

		@Opt(name = "ItemStack Rendering Fix", desc = "Items no longer draw under container text", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean itemstackRenderingFix = true;

		@Opt(name = "Fish Velocity Fix", desc = "Caught fish stop flying over your head")
		public Boolean fishVelocityFix = true;

		@Opt(name = "Furnace Lava Bucket Fix", desc = "The bucket comes back")
		public Boolean furnaceConsumeBucketFix = true;

		// StationAPI is named here for the same reason UniTweaks is, even though it does not offer this
		// as a setting: its arsenic renderer replaces the dropped-item renderer wholesale, so RetroTweaks
		// cannot reach the size constant at all. Better a greyed row that says who owns it than a live
		// toggle that does nothing.
		@Opt(name = "Dropped Item Size Fix", source = {Mods.UNITWEAKS, Mods.STATIONAPI}, scope = Scope.CLIENT)
		public Boolean droppedItemSizeFix = true;

		@Opt(name = "Breaking Animation Fix", desc = "Renders on the bottom face too", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean breakingAnimationFix = true;

		@Opt(name = "Death Screen Formatting Fix", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean deathScreenFormattingFix = true;

		@Opt(name = "Death Screen Score Fix", desc = "Shows the real score, not a stale one", scope = Scope.CLIENT)
		public Boolean deathScreenScoreFix = true;

		@Opt(name = "Hotbar Rendering Fix", desc = "No white flash on Fast graphics", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean hotbarRenderingFix = true;

		// Covers the texture AND the tint. Vanilla gives grass, ferns and tall grass a biome colour only
		// when there is a world to read one from, so as items they come out grey; see
		// client.render.TallPlantColorMixin and BlockRenderManagerMixin's grass-top section.
		//
		// Under StationAPI neither of those two mixins is reached - its arsenic renderer replaces every
		// item render path and tints a face only where the MODEL declares a tintindex. That is supplied
		// instead: assets/minecraft/stationapi/models/item/{grass_block,grass}.json, plus the colour
		// providers in compat.stationapi.StationApiItemColors. So the fix now lands on all three setups,
		// by two different routes; see those files for why each is shaped the way it is.
		//
		// The source = UNITWEAKS stand-down only reaches the MIXIN route (where UniTweaks' own fix
		// really does run). The StationAPI route ignores it and follows the player's chosen value
		// (ConfigManager.chosenBoolean), because under StationAPI UniTweaks' fix is dead code and a
		// stood-down provider left the shipped models untinted - a grey grass block item.
		@Opt(name = "Grass Item Rendering Fix", desc = "Right texture, plus the green tint vanilla forgets",
			source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean grassBlockItemFix = true;

		@Opt(name = "Multiplayer Mining Delay Fix", desc = "Some servers may consider this a hack", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean miningDelayFix = false;

		@Opt(name = "Last Durability Point Fix", desc = "The final swing still drops the block", source = Mods.UNITWEAKS)
		public Boolean lastDurabilityFix = true;

		@Opt(name = "Fence Lighting Fix", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean fenceLightingFix = true;

		@Opt(name = "Torch Bottom Face Fix", source = Mods.UNITWEAKS, scope = Scope.CLIENT)
		public Boolean torchBottomFaceFix = true;

		@Opt(name = "Minecart Stopping On Items Fix", scope = Scope.CLIENT)
		public Boolean minecartStoppingOnItemsFix = true;

		@Opt(name = "Slime Drop Fix", desc = "Small slimes drop slimeballs")
		public Boolean slimeDropFix = true;

		@Opt(name = "Stats Checksum Fix", desc = "Lets mods reorganise the stats file")
		public Boolean disableStatsChecksumVerification = true;
	}

	// ================================================================ Scoring

	/**
	 * From WhatAreYouScoring.
	 *
	 * <p>The two display modes are declared FIRST, and the sections they control sit directly under
	 * them, because nothing else in here shows up anywhere until one of them is set: the counters below
	 * tick along either way, but with both modes on Vanilla the game looks exactly like b1.7.3 and a
	 * page that opened with "Basic Score..." gave no hint of that. Same reasoning as
	 * {@link HudPositions#hudPositioningSystem}, and the two Custom sections carry a real
	 * {@link Cat#gateOption()} so they grey out and say so when their mode is not Custom rather than
	 * sitting editable and ignored.
	 */
	public static final class Scoring {
		// Vanilla by default, like the HUD mode beside it: b1.7.3's death screen says "Score: n" and
		// nothing else, and replacing that with a day count is a visible change to a vanilla screen
		// that nobody asked for. Pick Listed/Combined/Custom to get the extra scores back.
		@Opt(name = "Death Screen Display Mode", desc = "Vanilla leaves the death screen untouched")
		public Enums.ScoreDisplay deathScoreDisplayMode = Enums.ScoreDisplay.VANILLA;

		@Opt(name = "In Game HUD Display Mode", desc = "Vanilla draws no score on the HUD")
		public Enums.ScoreDisplay hudScoreDisplayMode = Enums.ScoreDisplay.VANILLA;

		@Cat(name = "Custom Death Screen Display", desc = "Which scores the Custom death screen lists",
			gateOption = "deathScoreDisplayMode", gateValues = "CUSTOM")
		public final ScoreDisplayParts customDeathDisplay = new ScoreDisplayParts();

		@Cat(name = "Custom HUD Display", desc = "Which scores the Custom HUD line lists",
			gateOption = "hudScoreDisplayMode", gateValues = "CUSTOM")
		public final ScoreDisplayParts customHudDisplay = new ScoreDisplayParts();

		@Cat(name = "Basic Score") public final BasicScore basic = new BasicScore();
		@Cat(name = "Days Score") public final DaysScore days = new DaysScore();
		@Cat(name = "404 Challenge Score") public final Challenge404 challenge404 = new Challenge404();

		// Off by default: three extra pages on the achievements screen is a lot to hand somebody who
		// never asked for score tracking, and it pushes the vanilla page behind < > navigation that
		// was not there before. The pages still exist either way - their achievements are registered,
		// their progress is tracked - they are simply reached through the game menu's own "WAYS
		// Achievements..." button instead. See AchievementPage.setHidden.
		@Opt(name = "WAYS Pages On Achievements Screen", desc = "Adds the three WAYS pages beside the vanilla one",
			requires = Mods.RETROAPI)
		public Boolean waysAchievementPages = false;

		// Needs RetroAPI's achievement registry - see ScoreAchievements. Restored (was deleted as a
		// dead toggle before that registry was wired up); requires so the screen greys it out and
		// says why instead of pretending it works on a bare install.
		@Opt(name = "Show Score On Scoring Achievement", desc = "On the WAYS achievement pages",
			requires = Mods.RETROAPI)
		public Boolean showScoreOnAchievement = true;

		@Opt(name = "Difficulty Score Multiplier On Death", desc = "Hard=0, Normal=0.5, Easy=0.75, Peaceful=1")
		public Boolean difficultyDeathMultiplier = false;
	}

	/** From WhatAreYouScoring. */
	public static final class ScoreDisplayParts {
		@Opt(name = "Show Basic Score") public Boolean basic = true;
		@Opt(name = "Show Days Score") public Boolean days = false;
		@Opt(name = "Show 404 Challenge Score") public Boolean challenge404 = false;
	}

	/** From WhatAreYouScoring. */
	public static final class BasicScore {
		@Opt(name = "Enable Basic Scoring", desc = "Reload the world to take effect") public Boolean enabled = true;
		@Opt(name = "+1 Per Block Placed") public Boolean onBlockPlaced = true;
		@Opt(name = "+1 Per Block Removed") public Boolean onBlockRemoved = true;
		@Opt(name = "+1 Per Monster Killed") public Boolean onMonsterKilled = true;
		@Opt(name = "+1 Per Passive Mob Killed") public Boolean onPassiveKilled = true;
	}

	/** From WhatAreYouScoring. */
	public static final class DaysScore {
		@Opt(name = "Enable Days Scoring", desc = "Reload the world to take effect") public Boolean enabled = true;
		@Opt(name = "+1 Per Day Survived") public Boolean perDay = true;
		@Opt(name = "+25 Per 100 Days") public Boolean per100Days = false;
		@Opt(name = "+100 Per 365 Days") public Boolean perYear = false;
	}

	/** From WhatAreYouScoring. */
	public static final class Challenge404 {
		@Opt(name = "Enable 404 Challenge Scoring", desc = "Reload the world to take effect") public Boolean enabled = false;
		@Opt(name = "Hard Mode Multiplier") public Boolean hardModeMultiplier = true;
		@Opt(name = "Never Sleep/Armor Gives Points", desc = "Off makes failing subtract instead") public Boolean positiveNeverSleepWearArmor = true;
		@Opt(name = "Score Mob Kills") public Boolean scoreMobKills = true;
	}

	// ================================================================ Fishing & Food

	/** From FishinFoodTweaks. */
	public static final class FishingAndFood {
		@Opt(name = "Random Fish Sizes")
		public Boolean randomFishSizes = true;

		@Opt(name = "Water Surface Affects Catch", desc = "Bigger water gives bigger fish")
		public Boolean calculateWaterSurfaceSize = true;

		@Opt(name = "Enable Oceanic Fish", desc = "Raises the max fish size from 70 to 110")
		public Boolean biggerFish = false;

		@Opt(name = "Fish Healing Tooltip")
		public Boolean fishHealingTooltip = false;

		@Opt(name = "Food Healing Tooltips")
		public Boolean foodHealingTooltips = false;

		@Opt(name = "Enable Non-vanilla Fish", desc = "Four extra fish species, caught alongside the vanilla two",
			requires = Mods.RETROAPI)
		public Boolean nonVanillaFish = false;
	}

	// ================================================================ Sounds

	/**
	 * From UniTweaks (More Sounds) and MiscTweaks.
	 *
	 * <p>Every sound in here is an ADDITION - b1.7.3 plays none of them - so every one of them starts
	 * off. A player who wants a chest to creak like a door can say so; a player who just wants the mod's
	 * fixes should not find the game making noises it never made.
	 */
	public static final class Sounds {
		@Opt(name = "More Sounds", desc = "Tool breaking, shearing, eating and more", source = Mods.UNITWEAKS)
		public Boolean moreSounds = false;

		@Opt(name = "Chest Sounds", source = Mods.UNITWEAKS)
		public Enums.ChestSounds chestSounds = Enums.ChestSounds.NONE;

		// Telvarost's own sounds. Each one used to play unconditionally as part of the feature that
		// causes it, with no way to silence it on its own - these are those switches.
		@Opt(name = "Trapdoor Glue Sound", desc = "Slime sound when a slimeball glues one shut")
		public Boolean trapdoorGlueSound = false;

		@Opt(name = "Trapdoor Refusal Sound", desc = "Thud when a glued trapdoor refuses to open")
		public Boolean trapdoorRefusalSound = false;

		@Opt(name = "Slab Merge Sound", desc = "Placement sound when two slabs merge into a double")
		public Boolean slabMergeSound = false;

		@Opt(name = "Explosion Sound Without Blocks", desc = "Keeps the boom when an explosion breaks nothing")
		public Boolean explosionSoundWithoutBlocks = true;
	}

	// ================================================================ Particles

	/** From UniTweaksTelsAddons. */
	public static final class Particles {
		@Opt(name = "Disable All Particles") public Boolean disableAll = false;
		@Opt(name = "Disable Water Bubbles") public Boolean bubble = false;
		@Opt(name = "Disable Smoke") public Boolean smoke = false;
		@Opt(name = "Disable Large Smoke") public Boolean largeSmoke = false;
		@Opt(name = "Disable Note") public Boolean note = false;
		@Opt(name = "Disable Portal") public Boolean portal = false;
		@Opt(name = "Disable Explosion") public Boolean explosion = false;
		@Opt(name = "Disable Flame") public Boolean flame = false;
		@Opt(name = "Disable Lava Embers") public Boolean lava = false;
		@Opt(name = "Disable Footsteps") public Boolean footstep = false;
		@Opt(name = "Disable Water Splash") public Boolean splash = false;
		@Opt(name = "Disable Redstone Dust") public Boolean redDust = false;
		@Opt(name = "Disable Snowballs") public Boolean snowball = false;
		@Opt(name = "Disable Snow Shovel") public Boolean snowShovel = false;
		@Opt(name = "Disable Slime") public Boolean slime = false;
		@Opt(name = "Disable Hearts") public Boolean heart = false;
	}

	// ================================================================ Multiplayer

	/** From MojangFix and UniTweaks. RetroAuth supersedes the skin and session options. */
	public static final class Multiplayer {
		@Opt(name = "TCP No Delay", desc = "Lower latency, slightly more packets", source = Mods.UNITWEAKS)
		public Boolean tcpNoDelay = true;

		@Opt(name = "Hide Server List IP Addresses", desc = "For screenshots and streaming")
		public Boolean hideServerListIps = false;

		@Opt(name = "Modern Session Authentication", desc = "RetroAuth does this better", restart = true, source = Mods.RETROAUTH)
		public Boolean modernAuthentication = true;

		@Opt(name = "Fetch Player Skins", restart = true, source = Mods.RETROAUTH)
		public Boolean fetchSkins = true;

		@Opt(name = "Render Player Capes", source = Mods.RETROAUTH)
		public Boolean renderCapes = true;

		@Opt(name = "Raise Slim Skin Shoulders", restart = true, source = Mods.RETROAUTH)
		public Boolean raiseSlimSkinShoulders = false;

		@Opt(name = "Download Resources", desc = "Sounds and music from a mirror", restart = true)
		public Boolean useResourcesDownloadUrl = true;

		@Opt(name = "Use The Alternate Resource URL", restart = true)
		public Boolean useAlternateResourcesUrl = false;

		@Opt(name = "Resource URL", restart = true)
		public String resourcesDownloadUrl = "http://s3.betacraft.uk:11705/MinecraftResources/";

		@Opt(name = "Alternate Resource URL", restart = true)
		public String alternateResourcesDownloadUrl = "http://mcresources.modification-station.net/MinecraftResources/";
	}
}
