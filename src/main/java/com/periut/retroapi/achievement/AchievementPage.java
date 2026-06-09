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

	public static AchievementPage nextPage() {
		currentPage += 1;
		if (currentPage > PAGES.size() - 1) currentPage = 0;
		return PAGES.get(currentPage);
	}

	public static AchievementPage prevPage() {
		currentPage -= 1;
		if (currentPage < 0) currentPage = PAGES.size() - 1;
		return PAGES.get(currentPage);
	}

	public static AchievementPage getCurrentPage() {
		return PAGES.isEmpty() ? null : PAGES.get(currentPage);
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

	public static int getPageCount() {
		return PAGES.size();
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
