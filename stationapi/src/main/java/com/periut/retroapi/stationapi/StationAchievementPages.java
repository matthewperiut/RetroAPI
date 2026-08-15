package com.periut.retroapi.stationapi;

import net.minecraft.achievement.Achievement;
import net.modificationstation.stationapi.api.client.gui.screen.achievement.AchievementPage;
import net.modificationstation.stationapi.api.util.Identifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts RetroAPI's achievement pages on StationAPI's achievements screen, as StationAPI's own pages.
 *
 * <h2>Why mirroring rather than a second implementation</h2>
 *
 * <p>RetroAPI has its own paged achievements screen, and the obvious move is to let it run here too.
 * That was tried and reverted: it filters a page by swapping the static {@code Achievements.ACHIEVEMENTS}
 * list for the duration of {@code renderIcons}, while StationAPI filters by CANCELLING per-icon events
 * ({@code AchievementPageImpl}) from injectors whose local indices were computed against the original
 * list. Two mechanisms over one method, and the shorter list desynchronised the other one: icons drew
 * white and the ordinary screen stayed broken afterwards.
 *
 * <p>So nothing here draws or filters anything. Each RetroAPI page gets a StationAPI page holding the
 * same achievements, and StationAPI's screen does all of it - navigation, titles, icon filtering,
 * backgrounds - exactly as it does for a StationAPI-native mod. One implementation, the one that owns
 * the screen.
 *
 * <h2>Hidden pages</h2>
 *
 * <p>RetroAPI can keep a page off the screen and show it in a viewer instead (see
 * {@code AchievementPage.setHidden}). StationAPI has no such notion and no way to remove a page once
 * added, so the mirror is reconciled instead: {@link #sync()} makes StationAPI's page list hold
 * exactly the pages that should be reachable right now, and runs whenever the screen opens.
 */
public final class StationAchievementPages {

	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/StationAPI");

	private StationAchievementPages() {
	}

	/** RetroAPI page -> the StationAPI page standing in for it. Built once, reused for the session. */
	private static final Map<com.periut.retroapi.achievement.AchievementPage, AchievementPage> MIRRORS =
		new LinkedHashMap<>();

	/**
	 * Every page that is not one of ours - StationAPI's own "Minecraft" page, and any other mod's -
	 * remembered the first time it is seen.
	 *
	 * <p>Remembered rather than re-read, because the viewer takes them OUT of the live list: reading
	 * that list to rebuild the ordinary set therefore found only our own mirrors and rebuilt the
	 * viewer, so once the viewer had been opened the ordinary achievements screen never came back.
	 * The list is the working copy; this is what it is restored from.
	 */
	private static final List<AchievementPage> FOREIGN = new ArrayList<>();

	/** StationAPI's own page list, which has an add but no remove. */
	private static Field pagesField;
	/** StationAPI's current-page index, reset whenever the reachable set changes under it. */
	private static Field currentPageField;
	private static boolean unavailable;

	/**
	 * Reconciles StationAPI's page list with RetroAPI's, for the mode the screen is about to open in.
	 *
	 * <p>Called from {@code AchievementsScreenInitMixin} rather than at registration, because what
	 * belongs on the screen depends on a setting and on whether the WAYS viewer is what opened it -
	 * both of which can change between one screen and the next.
	 */
	public static void sync() {
		final List<AchievementPage> pages = stationPages();
		if (pages == null) {
			return;
		}

		final boolean viewingHidden = com.periut.retroapi.achievement.AchievementPage.isViewingHidden();

		// Anything that is not ours is noted before anything is removed, so it can always be put back.
		for (final AchievementPage page : pages) {
			if (!MIRRORS.containsValue(page) && !FOREIGN.contains(page)) {
				FOREIGN.add(page);
			}
		}

		// Everything StationAPI or another mod registered stays; only our own mirrors are managed here,
		// and only they are ever removed.
		final List<AchievementPage> wanted = new ArrayList<>();
		if (!viewingHidden) {
			wanted.addAll(FOREIGN);
		}

		for (final com.periut.retroapi.achievement.AchievementPage source
				: com.periut.retroapi.achievement.AchievementPage.all()) {
			if (source.isDefaultPage()) {
				continue;
			}
			// The viewer shows OUR pages - all of them, whether or not they are currently hidden.
			// Asking for hidden ones specifically was wrong: with the setting turned on nothing is
			// hidden, so the viewer selected nothing and left the screen with no pages at all, which
			// is a crash rather than an empty screen (StationAPI reads PAGES.get(index) unguarded).
			// Outside the viewer it is the setting that decides, which is what hidden means.
			if (viewingHidden || !source.isHidden()) {
				wanted.add(mirror(source));
			}
		}

		// Never hand StationAPI an empty list. Every route here should leave at least one page, but a
		// list it indexes without checking is not the place to find out otherwise.
		if (wanted.isEmpty()) {
			LOGGER.debug("Achievement page sync produced no pages; leaving StationAPI's list alone");
			return;
		}

		if (pages.equals(wanted)) {
			return;
		}
		pages.clear();
		pages.addAll(wanted);
		setCurrentPage(0);
	}

	/** The StationAPI page standing in for a RetroAPI one, created and filled on first use. */
	private static AchievementPage mirror(final com.periut.retroapi.achievement.AchievementPage source) {
		AchievementPage mirrored = MIRRORS.get(source);
		if (mirrored != null) {
			return mirrored;
		}

		// The constructor adds itself to StationAPI's list; sync() rewrites that list wholesale
		// immediately afterwards, so the self-registration is harmless either way.
		mirrored = new AchievementPage(Identifier.of(source.getId().toString()));
		final List<Achievement> achievements = source.getAchievements();
		mirrored.addAchievements(achievements.toArray(new Achievement[0]));
		MIRRORS.put(source, mirrored);
		return mirrored;
	}

	@SuppressWarnings("unchecked")
	private static List<AchievementPage> stationPages() {
		if (unavailable) {
			return null;
		}
		try {
			if (pagesField == null) {
				pagesField = AchievementPage.class.getDeclaredField("PAGES");
				pagesField.setAccessible(true);
			}
			return (List<AchievementPage>) pagesField.get(null);
		} catch (final ReflectiveOperationException | RuntimeException e) {
			unavailable = true;
			LOGGER.warn("StationAPI's achievement page list was not where it was expected; RetroAPI pages "
				+ "will not appear on the achievements screen", e);
			return null;
		}
	}

	private static void setCurrentPage(final int index) {
		try {
			if (currentPageField == null) {
				currentPageField = AchievementPage.class.getDeclaredField("currentPage");
				currentPageField.setAccessible(true);
			}
			currentPageField.setInt(null, index);
		} catch (final ReflectiveOperationException | RuntimeException e) {
			// Only costs the screen opening on whichever page it was last left on.
			LOGGER.debug("Could not reset StationAPI's current achievement page", e);
		}
	}
}
