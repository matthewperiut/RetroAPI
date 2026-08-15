package com.periut.retrotweaks.feature.options;

import com.periut.retrotweaks.compat.Mods;
import com.periut.retrotweaks.config.Config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.option.Option;

import org.lwjgl.input.Keyboard;

/**
 * The video settings b1.7.3 never had, added as real entries in vanilla's {@code Option} enum.
 * From UniTweaks.
 *
 * <p>They have to be enum constants rather than a parallel list because every part of the options
 * machinery - the buttons, the sliders, saving to options.txt - is keyed on {@code Option}. The
 * constants are appended from the enum's own class initialiser; see {@code OptionMixin}.
 *
 * <p>Values are held here rather than on {@code GameOptions} so that a RetroTweaks jar dropped in or
 * pulled out never leaves unreadable lines in options.txt: they are saved under their own keys and
 * anything missing falls back to the vanilla-equivalent default.
 */
@Environment(EnvType.CLIENT)
public final class ModOptions {

	private ModOptions() {}

	/**
	 * Forces vanilla's {@code Option} enum to initialise, which is what makes the eight settings below
	 * exist at all.
	 *
	 * <p>{@code OptionMixin} appends them from inside {@code Option.<clinit>}, and a Java class
	 * initialises lazily - on first active use. Nothing on the startup path touches {@code Option}, so
	 * until the player opened a screen that did (Video Settings, in practice), all eight handles stayed
	 * null, {@link #enabled} answered false for every one of them, and every override stood down: the
	 * saved render distance, fog, brightness, FOV, GUI scale and frame cap were all read from
	 * {@code retrotweaks-video.txt} at startup and then ignored, coming into force only once the options
	 * screen had been visited. Calling this from the client entry point moves that moment to before the
	 * first frame, where it belongs.
	 *
	 * <p>{@code values()} is used rather than reading a constant because it cannot be optimised away as
	 * an unused field read, and it needs no particular constant to still exist.
	 */
	public static void ensureRegistered() {
		Option.values();
	}

	// Filled in by OptionMixin as the enum initialises - see ensureRegistered().
	public static Option fogDensityOption;
	public static Option cloudsOption;
	public static Option cloudHeightOption;
	public static Option fpsLimitOption;
	public static Option renderDistanceOption;
	public static Option brightnessOption;
	public static Option guiScaleOption;
	public static Option fovOption;

	/**
	 * Where each slider starts on a fresh install, all 0..1 as the vanilla slider widget expects.
	 *
	 * <p>Two of these are deliberately not at the end of their track. Fog density sits where
	 * {@link #fogMultiplier()} reads 1.0 - i.e. vanilla fog - because the slider's own top end is 4.0
	 * ("400%"), and starting a fresh install four times past vanilla made the world look wrong out of
	 * the box. Render distance sits where {@link #renderDistanceChunks()} reads 12, vanilla's own "Far",
	 * rather than at the bottom of the track, which reads 2 and left new players in a fog bank.
	 *
	 * <p>Named constants rather than bare literals on the fields below because the config screen's
	 * "Defaults" has to be able to put them back - see {@link #restoreVideoDefaults}.
	 */
	public static final float DEFAULT_FOG_DENSITY = 0.2F;
	public static final float DEFAULT_CLOUD_HEIGHT = 0.0F;
	public static final float DEFAULT_FPS_LIMIT = 1.0F;
	public static final float DEFAULT_RENDER_DISTANCE = 10.0F / 30.0F;
	public static final float DEFAULT_BRIGHTNESS = 0.0F;
	public static final float DEFAULT_GUI_SCALE = 0.0F;
	public static final float DEFAULT_FOV = 0.0F;
	public static final boolean DEFAULT_CLOUDS = true;

	/** Live slider positions. Anything already in the options file wins over the defaults above at
	 *  startup - see {@code GameOptionsMixin.retrotweaks$load}. */
	public static float fogDensity = DEFAULT_FOG_DENSITY;
	public static float cloudHeight = DEFAULT_CLOUD_HEIGHT;
	public static float fpsLimit = DEFAULT_FPS_LIMIT;
	public static float renderDistance = DEFAULT_RENDER_DISTANCE;
	public static float brightness = DEFAULT_BRIGHTNESS;
	public static float guiScale = DEFAULT_GUI_SCALE;
	public static float fov = DEFAULT_FOV;

	/**
	 * Hotbar-scroll input accumulated for the current frame while {@code ModKeyBindings.ZOOM} is
	 * held; consumed and reset by {@code GameRendererFovMixin.getFovMultiplier} every call. From
	 * UniTweaks' {@code ModOptions.zoomFovOffset}.
	 */
	public static float zoomFovOffset = 0.0F;

	public static void addZoomFovOffset(float offset) {
		zoomFovOffset = offset;
	}

	/** The GUI scale actually in force. Separate so dragging the slider does not thrash the layout. */
	public static float realGuiScale = DEFAULT_GUI_SCALE;

	public static boolean clouds = DEFAULT_CLOUDS;

	/**
	 * Puts all eight video settings back to the values a fresh install starts with, and writes them out.
	 *
	 * <p>The fields are assigned directly rather than through {@code GameOptions.setFloat}: that path is
	 * cancelled for any option whose override flag is off (see {@code GameOptionsMixin}), which would
	 * quietly exempt exactly those settings from a reset the screen said covered everything. Saving goes
	 * through {@code GameOptions.save}, which the same mixin extends to write the RetroTweaks file
	 * alongside vanilla's, so this needs no access to that private writer.
	 */
	public static void restoreVideoDefaults(Minecraft minecraft) {
		fogDensity = DEFAULT_FOG_DENSITY;
		cloudHeight = DEFAULT_CLOUD_HEIGHT;
		fpsLimit = DEFAULT_FPS_LIMIT;
		renderDistance = DEFAULT_RENDER_DISTANCE;
		brightness = DEFAULT_BRIGHTNESS;
		guiScale = realGuiScale = DEFAULT_GUI_SCALE;
		fov = DEFAULT_FOV;
		clouds = DEFAULT_CLOUDS;
		if (minecraft == null || minecraft.options == null) return;
		minecraft.options.save();
		// The GUI scale just changed under the open screen; rebuild it at the new size, exactly as
		// releasing that slider does.
		refreshScreen(minecraft);
	}

	/** True while the third-person camera is looking back at the player. */
	public static boolean frontView = false;

	/**
	 * Whether the camera is currently front-facing, under either mode: the explicit Normal cycle,
	 * or Hide HUD's "third person with the HUD off means front view" shorthand.
	 */
	public static boolean isFrontView(Minecraft minecraft) {
		if (Config.INTERFACE.frontViewThirdPerson == com.periut.retrotweaks.config.Enums.FrontView.DISABLED) {
			return false;
		}
		if (frontView) return true;
		if (minecraft == null || minecraft.options == null) return false;
		return Config.INTERFACE.frontViewThirdPerson == com.periut.retrotweaks.config.Enums.FrontView.HIDE_HUD
			&& minecraft.options.thirdPerson && minecraft.options.hideHud;
	}

	/** Cloud height in blocks. Vanilla draws them at 108. */
	public static float cloudHeightBlocks() {
		return 108.0F + cloudHeight * 128.0F;
	}

	/** Fog distance multiplier. 1.0 is vanilla. */
	public static float fogMultiplier() {
		// Below 1 thickens the fog, above 1 pushes it back; the range is deliberately generous
		// because "how far can I see" is the whole point of the slider.
		return 0.25F + fogDensity * 3.75F;
	}

	/**
	 * The three things the FPS limit slider can select. Replaces the old convention of overloading
	 * {@code frameLimit() == 0} to mean "unlimited" - a third state (VSync) makes that ambiguous, so
	 * the mode is modelled explicitly instead of left for the next reader to infer from a magic int.
	 */
	public enum FrameLimitMode { VSYNC, CAPPED, UNLIMITED }

	/**
	 * Stops between the slider's two ends, exclusive of the ends themselves: VSync (index 0), then
	 * 10 fps through 250 fps in steps of ten (indices 1..25), then Unlimited (index 26) - 27 stops,
	 * 26 gaps. {@link #frameLimitStopIndex()} is {@code round(fpsLimit * FRAME_LIMIT_STEPS)}, so every
	 * index lands on a whole stop and nothing in between is ever selectable or displayed.
	 */
	private static final int FRAME_LIMIT_STEPS = 26;

	/** The slider's raw 0..1 position resolved onto its nearest stop, 0..{@link #FRAME_LIMIT_STEPS}. */
	public static int frameLimitStopIndex() {
		int index = Math.round(fpsLimit * FRAME_LIMIT_STEPS);
		return Math.max(0, Math.min(FRAME_LIMIT_STEPS, index));
	}

	/** Which of the three states {@link #frameLimitStopIndex()} currently selects. */
	public static FrameLimitMode frameLimitMode() {
		int index = frameLimitStopIndex();
		if (index == 0) return FrameLimitMode.VSYNC;
		if (index == FRAME_LIMIT_STEPS) return FrameLimitMode.UNLIMITED;
		return FrameLimitMode.CAPPED;
	}

	/**
	 * The fps target in {@link FrameLimitMode#CAPPED}, 10..250 in steps of ten. Meaningless in either
	 * other mode (it evaluates to 0 at the VSync stop and 260 at the Unlimited stop) - callers must
	 * check {@link #frameLimitMode()} first, exactly as they must not read a paused-menu fps value
	 * without checking whether the menu is even open.
	 */
	public static int frameLimit() {
		return frameLimitStopIndex() * 10;
	}

	/**
	 * Rounds a raw 0..1 slider position onto the stop it is currently displaying, the FPS-limit
	 * counterpart of {@link #snapToGuiScaleStep}: same reasoning (release lands on the value the
	 * label showed while dragging, not a value between two stops) and the same rounding the label
	 * itself uses ({@link #frameLimitStopIndex()}'s {@code round}, not {@code floor} - unlike GUI
	 * scale, stop 0 is a real selectable state (VSync) rather than a distinct "auto" case, so there
	 * is no reason to bias away from it).
	 */
	public static float snapToFpsLimitStep(float value) {
		float clamped = Math.max(0.0F, Math.min(1.0F, value));
		int index = Math.max(0, Math.min(FRAME_LIMIT_STEPS, Math.round(clamped * FRAME_LIMIT_STEPS)));
		return index / (float) FRAME_LIMIT_STEPS;
	}

	/** True when the FPS limit override flag is on, whatever mode it is currently set to. */
	public static boolean fpsLimitEnabled() {
		return enabled(fpsLimitOption);
	}

	/**
	 * True when RetroTweaks' own numeric frame pacing - the vanilla Performance-tier override in
	 * {@code FpsLimitMixin} and the {@code Display.sync} call in {@code FpsLimitSyncMixin} - should
	 * govern the frame rate: the config toggle is on AND something can actually act on a numeric
	 * target.
	 *
	 * <p>That second half used to be simply "RetroDragon is not installed", because two sleep-based
	 * limiters fighting is worse than either. It is no longer the right test. RetroDragon 0.1.6 added
	 * a real frame limiter to its own pacer ({@code RetroSettings.setFrameLimit}), so under 0.1.6 or
	 * newer the cap is expressed THROUGH RetroDragon rather than against it, and standing down would
	 * just mean the cap silently does nothing - which is exactly what a user reported.
	 *
	 * <p>So the cap applies when RetroDragon is absent (vanilla LWJGL 2 {@code Display.sync} paces it)
	 * or when it is present and new enough to accept one. It still stands down for a RetroDragon older
	 * than 0.1.6, where nothing would act on the target.
	 *
	 * <p>VSync is deliberately NOT gated by this - see {@code FpsLimitSyncMixin}'s VSync block, which
	 * checks {@link #fpsLimitEnabled()} alone. VSync is a driver-level swap interval rather than a
	 * sleep, and RetroDragon 0.1.6 wired its own {@code Display.setVSyncEnabled} shim through to the
	 * same pacer, so it works on both backends regardless.
	 */
	public static boolean fpsLimitActive() {
		return fpsLimitEnabled()
			&& (!Mods.HAS_RETRODRAGON || com.periut.retrotweaks.compat.ApiBridge.hasFrameLimit());
	}

	/**
	 * True unless the slider sits at Unlimited. Vanilla's own Performance-tier bookkeeping in
	 * {@code FpsLimitMixin} only cares whether something is bounding the frame rate, not what - so
	 * both VSync and a numeric cap count as "limited" here. Do not use this where a numeric fps value
	 * is actually needed ({@link #frameLimit()}'s caller must check for {@link FrameLimitMode#CAPPED}
	 * specifically instead), since VSync passes this check but has no numeric target.
	 */
	public static boolean frameLimited() {
		return frameLimitMode() != FrameLimitMode.UNLIMITED;
	}

	/**
	 * Render distance in chunks, beyond vanilla's four fixed steps. Matches UniTweaks'
	 * {@code getRenderDistanceChunks()} (2 plus up to 30 more, floor there vs round here - an existing
	 * RetroTweaks choice, not changed by the render-distance mixins that read this).
	 *
	 * <p>{@link #MAX_RENDER_DISTANCE_CHUNKS} is the deliberate ceiling this always stays within - see
	 * that constant's own doc for what it costs. {@code renderDistance} is itself always 0..1 (the
	 * vanilla slider-widget convention every other option here follows), so this can never return
	 * less than 2 or more than {@link #MAX_RENDER_DISTANCE_CHUNKS}.
	 */
	public static int renderDistanceChunks() {
		return 2 + Math.round(renderDistance * 30.0F);
	}

	/**
	 * The highest {@link #renderDistanceChunks()} can return - mirrors UniTweaks' own
	 * {@code maxRenderDistance}. {@code WorldRendererMixin} sizes the chunk grid at
	 * {@code chunks * 2 + 1} per axis, so this is also this mod's ceiling on
	 * {@code chunkCountX}/{@code chunkCountZ}: {@code 32*2+1 = 65}, against vanilla's own worst case of
	 * 26 (`WorldRenderer.reload()`'s 400-block clamp, {@code 400/16+1}). That is roughly 6x the
	 * {@code ChunkBuilder} objects vanilla ever allocates ({@code 65*8*65 = 33800} vs
	 * {@code 26*8*26 = 5408}) - the entire point of the slider, so it is not raised past what
	 * UniTweaks itself shipped.
	 */
	public static final int MAX_RENDER_DISTANCE_CHUNKS = 32;

	/** Inverse of {@link #renderDistanceChunks()}: the slider position that displays as {@code chunks}. */
	public static void setRenderDistanceChunks(int chunks) {
		renderDistance = (chunks - 2) / 30.0F;
	}

	/**
	 * Steps the render distance through the same four stops - 2 (Tiny), 4 (Short), 8 (Normal), 12
	 * (Far) - vanilla's own four tiers land near, cycling forward or, with either Shift held,
	 * backward. Bound to the vanilla render-distance key ("F", {@code GameOptions.fogKey}) by
	 * {@code MinecraftRenderDistanceMixin} so that key still does something once the slider has
	 * replaced vanilla's own four-tier option. From UniTweaks' {@code ModOptions.cycleRenderDistance()}.
	 */
	public static void cycleRenderDistance() {
		boolean inverted = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
		int chunks = renderDistanceChunks();
		if (chunks <= 3) {
			setRenderDistanceChunks(inverted ? 12 : 4);
		} else if (chunks <= 7) {
			setRenderDistanceChunks(inverted ? 2 : 8);
		} else if (chunks <= 11) {
			setRenderDistanceChunks(inverted ? 4 : 12);
		} else {
			setRenderDistanceChunks(inverted ? 8 : 2);
		}
		renderDistance = Math.max(0.0F, Math.min(1.0F, renderDistance));
	}

	/** Extra brightness added to the world light table, 0 for vanilla. */
	public static float brightnessBoost() {
		return brightness;
	}

	/**
	 * Approximate FOV in degrees for the slider's own label: vanilla's 70 plus up to +-40 from the
	 * slider position. Matches UniTweaks' {@code getFovInDegrees()}; the actual FOV applied to the
	 * render also accounts for water and low health, which this ignores same as UniTweaks does.
	 */
	public static int fovDegrees() {
		return Math.round(70.0F + fov * 40.0F);
	}

	/**
	 * The offset {@code GameRendererFovMixin} adds onto whatever vanilla already computed for the
	 * frame (water, low health, etc.), rather than replacing it outright. Matches UniTweaks'
	 * {@code getFovOffsetInDegrees()}.
	 */
	public static int fovOffsetDegrees() {
		return Math.round(fov * 40.0F);
	}

	/**
	 * Highest GUI scale this window can actually reach, so the slider never offers a step that does
	 * nothing. Kept up to date by {@code ScreenScalerMixin}, which is handed the framebuffer size
	 * every frame; 8 until the first frame builds one, and it self-corrects immediately after.
	 *
	 * <p>UniTweaks hardcodes 8. That is fine until you shrink the window, at which point the top of
	 * the slider is inert and lies about what it will do.
	 */
	private static int guiScaleMaxSteps = 8;

	/**
	 * Recomputes the reachable ceiling from a framebuffer size. This is vanilla's own limit, read off
	 * {@code ScreenScaler}'s loop: it stops raising the factor once {@code width/(factor+1) < 320} or
	 * {@code height/(factor+1) < 240}, so the largest usable factor is {@code min(w/320, h/240)}.
	 */
	public static void updateGuiScaleCeiling(int framebufferWidth, int framebufferHeight) {
		guiScaleMaxSteps = Math.max(1, Math.min(framebufferWidth / 320, framebufferHeight / 240));
	}

	/** Highest step the slider currently offers. 0 ("auto") sits below 1..this. */
	public static int guiScaleCeiling() {
		return guiScaleMaxSteps;
	}

	/**
	 * The scale actually applied to the render, e.g. by {@code ScreenScalerMixin}.
	 *
	 * <p>{@code floor(x * ceiling)}. UniTweaks uses {@code floor(x * 8)}; the ceiling is the one
	 * deliberate change, so the range matches what the window can do. The port before this used
	 * {@code round(x * 5)}, a different top step and a different rounding rule again.
	 *
	 * <p>0 means "auto" - vanilla reads it as 1000 and takes the largest that fits.
	 */
	public static int guiScaleSteps() {
		return (int) Math.floor(realGuiScale * guiScaleMaxSteps);
	}

	/**
	 * The scale the slider is currently showing, updated every frame while dragging. Separate from
	 * {@link #guiScaleSteps()} so the label tracks the drag live even though the render is only
	 * re-applied on release - see {@code GameOptionsMixin.retrotweaks$setFloat}. Matches UniTweaks'
	 * {@code getGuiScaleDisplayValue()} apart from the dynamic ceiling.
	 */
	public static int guiScaleDisplaySteps() {
		return (int) Math.floor(guiScale * guiScaleMaxSteps);
	}

	/**
	 * Rounds a raw 0..1 slider position onto the step it is currently displaying, so releasing the
	 * mouse lands the handle on a whole scale rather than between two. Uses the same {@code floor}
	 * and the same ceiling as {@link #guiScaleDisplaySteps()} on purpose - snapping to a different
	 * step than the label showed while dragging would make the value jump under the cursor at the
	 * moment of release.
	 *
	 * <p>Not something UniTweaks does; requested for RetroTweaks.
	 */
	public static float snapToGuiScaleStep(float value) {
		float clamped = Math.max(0.0F, Math.min(1.0F, value));
		return (int) Math.floor(clamped * guiScaleMaxSteps) / (float) guiScaleMaxSteps;
	}

	/** True when the option exists and its config override flag is on. */
	public static boolean enabled(Option option) {
		Config.VideoSettings video = Config.GRAPHICS.video;
		if (option == null) return false;
		if (option == fogDensityOption) return video.fogDensityOverride;
		if (option == cloudsOption) return video.cloudsToggle;
		if (option == cloudHeightOption) return video.cloudHeightOverride;
		if (option == fpsLimitOption) return video.fpsLimitOverride;
		if (option == renderDistanceOption) return video.renderDistanceOverride;
		if (option == brightnessOption) return video.brightnessOverride;
		if (option == guiScaleOption) return video.guiScaleOverride;
		if (option == fovOption) return video.fovOverride;
		return false;
	}

	/** True when this is one of ours at all, whatever the config says. */
	public static boolean isModOption(Option option) {
		return option != null && (option == fogDensityOption || option == cloudsOption
			|| option == cloudHeightOption || option == fpsLimitOption
			|| option == renderDistanceOption || option == brightnessOption
			|| option == guiScaleOption || option == fovOption);
	}

	/**
	 * The "Label: " prefix {@code GameOptionsMixin.retrotweaks$getString} prepends to one of our
	 * options' value text - shared out to a single place so
	 * {@link com.periut.retrotweaks.client.gui.ConfigScreen}'s Graphics-tab row can show the
	 * same prefix in front of a suppression reason when the option's override flag is off, instead of
	 * inventing a second copy of this mapping that could drift from the one the vanilla screen uses.
	 */
	public static String optionLabel(Option option) {
		if (option == fogDensityOption) return "Fog Density: ";
		if (option == cloudsOption) return "Clouds: ";
		if (option == cloudHeightOption) return "Cloud Height: ";
		if (option == fpsLimitOption) return "FPS Limit: ";
		if (option == renderDistanceOption) return "Render Distance: ";
		if (option == brightnessOption) return "Brightness: ";
		if (option == fovOption) return "FOV: ";
		return "GUI Scale: ";
	}

	/** Redraws the current screen after a GUI scale change so it does not sit at the old size. */
	public static void refreshScreen(Minecraft minecraft) {
		if (minecraft == null || minecraft.currentScreen == null) return;
		net.minecraft.client.util.ScreenScaler scaler =
			new net.minecraft.client.util.ScreenScaler(minecraft.options, minecraft.displayWidth, minecraft.displayHeight);
		minecraft.currentScreen.init(minecraft, scaler.getScaledWidth(), scaler.getScaledHeight());
	}
}
