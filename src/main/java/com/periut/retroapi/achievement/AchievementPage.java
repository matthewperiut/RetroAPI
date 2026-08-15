package com.periut.retroapi.achievement;

import net.minecraft.achievement.Achievement;
import net.minecraft.achievement.Achievements;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Multi-page achievements-screen model, a trimmed RetroAPI-native copy of StationAPI's
 * {@code AchievementPage}. Holds the static page registry ({@link #PAGES} + {@link #currentPage})
 * and per-page achievement lists. The non-StationAPI path drives
 * {@link com.periut.retroapi.mixin.client.achievement.AchievementsPageScreenMixin}; when StationAPI
 * is present that mixin is disabled and pages are forwarded to StationAPI via
 * the RetroAPI StationAPI bridge.
 */
public class AchievementPage {
	private static final List<AchievementPage> PAGES = new ArrayList<>();
	private static int currentPage = 0;

	/** The implicit page 0 holding the vanilla (un-paged) achievements, mirroring StationAPI's
	 *  default {@code minecraft} page. Lazily created so the first custom page makes the page count
	 *  2 and the {@code </>} navigation appears (without it a single custom page can never be reached). */
	private static AchievementPage defaultPage;

	/** Translation key, built {@code namespace.path} from the page identifier (StationAPI parity). */
	private final String translationKey;
	private final NamespacedIdentifier id;
	private final List<Achievement> achievements = new ArrayList<>();
	/** True only for the implicit default page; its effective list is computed dynamically. */
	private final boolean isDefault;

	public AchievementPage(NamespacedIdentifier id) {
		this(id, false);
	}

	private AchievementPage(NamespacedIdentifier id, boolean isDefault) {
		this.id = id;
		this.isDefault = isDefault;
		this.translationKey = id.namespace() + "." + id.identifier();
		addPage(this);
	}

	public static void addPage(AchievementPage page) {
		// Ensure the implicit default (vanilla) page exists at index 0 before any custom page,
		// so navigation works and custom pages don't subsume the vanilla achievements.
		if (!page.isDefault && defaultPage == null) {
			defaultPage = new AchievementPage(NamespacedIdentifiers.from("minecraft", "minecraft"), true);
		}
		PAGES.add(page);
	}

	/**
	 * Whether this page is kept off the achievements screen's normal navigation.
	 *
	 * <p>For a set of pages that should exist - their achievements registered, their ids agreed
	 * between client and server, their progress tracked - without being in everyone's face on the main
	 * screen. A hidden page is reachable through {@link #setViewingHidden}, which is what a mod's own
	 * "viewer" button opens.
	 */
	private boolean hidden;

	/** True while navigation is showing the hidden pages INSTEAD of the ordinary ones. */
	private static boolean viewingHidden;

	public AchievementPage setHidden(final boolean hidden) {
		this.hidden = hidden;
		return this;
	}

	public boolean isHidden() {
		return hidden;
	}

	/** Run before an achievements screen is built - see {@link #onBeforeScreen}. */
	private static final List<Runnable> BEFORE_SCREEN = new ArrayList<>();

	/**
	 * Registers work to do just before an achievements screen is built.
	 *
	 * <p>For anything deciding what belongs on the screen from state this class cannot see - a mod's
	 * own setting, most obviously, which is what {@link #setHidden} is usually driven from. Without
	 * it the pages carry whatever visibility they were last given, which on the first screen of a
	 * session is none at all: every page shows, however the setting reads.
	 */
	public static void onBeforeScreen(final Runnable action) {
		if (action != null) BEFORE_SCREEN.add(action);
	}

	/** Runs everything {@link #onBeforeScreen} registered. Called from the screen's constructor. */
	public static void prepare() {
		for (final Runnable action : BEFORE_SCREEN) {
			action.run();
		}
	}

	/** Every registered page, in registration order. For a bridge that mirrors them elsewhere. */
	public static List<AchievementPage> all() {
		return Collections.unmodifiableList(PAGES);
	}

	/** True for the implicit page 0 holding the vanilla achievements. */
	public boolean isDefaultPage() {
		return isDefault;
	}

	/** True when at least one page is hidden, i.e. when there is anything for a viewer to show. */
	public static boolean hasHiddenPages() {
		for (final AchievementPage page : PAGES) {
			if (page.hidden) return true;
		}
		return false;
	}

	/**
	 * Switches navigation between the ordinary pages and the hidden ones. Resets the current page, so
	 * entering or leaving the viewer always starts at that set's first page rather than at whatever
	 * index the other set happened to be on.
	 */
	public static void setViewingHidden(final boolean viewing) {
		if (viewingHidden == viewing) return;
		viewingHidden = viewing;
		currentPage = 0;
	}

	public static boolean isViewingHidden() {
		return viewingHidden;
	}

	/** The pages navigation may currently reach: the hidden set in the viewer, the rest otherwise. */
	private static List<AchievementPage> visible() {
		final List<AchievementPage> out = new ArrayList<>();
		for (final AchievementPage page : PAGES) {
			if (page.hidden == viewingHidden) out.add(page);
		}
		return out;
	}

	public static AchievementPage nextPage() {
		final List<AchievementPage> pages = visible();
		if (pages.isEmpty()) return null;
		currentPage += 1;
		if (currentPage > pages.size() - 1) currentPage = 0;
		return pages.get(currentPage);
	}

	public static AchievementPage prevPage() {
		final List<AchievementPage> pages = visible();
		if (pages.isEmpty()) return null;
		currentPage -= 1;
		if (currentPage < 0) currentPage = pages.size() - 1;
		return pages.get(currentPage);
	}

	public static AchievementPage getCurrentPage() {
		final List<AchievementPage> pages = visible();
		if (pages.isEmpty()) return null;
		if (currentPage >= pages.size()) currentPage = 0;
		return pages.get(currentPage);
	}

	public static String getCurrentPageName() {
		AchievementPage page = getCurrentPage();
		return page == null ? "" : page.name();
	}

	/** True when the current page is the implicit vanilla page (so the screen keeps its default
	 *  title instead of drawing a raw {@code minecraft.minecraft} page key). */
	public static boolean isCurrentPageDefault() {
		AchievementPage page = getCurrentPage();
		return page != null && page.isDefault;
	}

	public static int getCurrentPageIndex() {
		return currentPage;
	}

	/** How many pages navigation can currently reach - see {@link #visible()}. */
	public static int getPageCount() {
		return visible().size();
	}

	/**
	 * Adds the given (already configured + registered) achievements to this page.
	 * Must be called after the achievements have been registered through
	 * {@link RetroAchievements}.
	 */
	public void addAchievements(Achievement... achievements) {
		Collections.addAll(this.achievements, achievements);
	}

	/**
	 * Per-page background-tile hook. Default returns the unchanged vanilla texture id;
	 * pages override to tile their own background (e.g. Aether's ore-tiled page).
	 */
	public int getBackgroundTexture(Random random, int column, int row, int randomizedRow, int currentTexture) {
		return currentTexture;
	}

	public String name() {
		return translationKey;
	}

	public NamespacedIdentifier getId() {
		return id;
	}

	@SuppressWarnings("unchecked")
	public List<Achievement> getAchievements() {
		if (isDefault) {
			// Vanilla page: everything currently in Achievements.ACHIEVEMENTS that is not claimed by a
			// custom page. Computed at call time so it reflects late registrations and avoids snapshot
			// ordering bugs (it always excludes achievements explicitly placed on another page).
			List<Achievement> all = new ArrayList<>((List<Achievement>) (List<?>) Achievements.ACHIEVEMENTS);
			all.removeIf(AchievementPage::retroapi$isOnCustomPage);
			return Collections.unmodifiableList(all);
		}
		return Collections.unmodifiableList(achievements);
	}

	private static boolean retroapi$isOnCustomPage(Achievement achievement) {
		for (AchievementPage page : PAGES) {
			if (!page.isDefault && page.achievements.contains(achievement)) return true;
		}
		return false;
	}
}
