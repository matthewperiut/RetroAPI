package com.periut.retrotweaks.feature.scoring.achievement;

import com.periut.retrotweaks.RetroTweaks;
import com.periut.retrotweaks.compat.ApiBridge;
import com.periut.retrotweaks.compat.Mods;
import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.feature.scoring.Score;
import com.periut.retrotweaks.mixin.achievement.AchievementDescriptionAccessor;
import com.periut.retrotweaks.util.Players;

import net.minecraft.achievement.Achievement;
import net.minecraft.entity.player.PlayerEntity;

/**
 * WhatAreYouScoring's three "WAYS" achievement pages (Basic, Days, 404 Challenge), rebuilt on
 * RetroAPI's achievement registry (see {@link ApiBridge}) in place of the StationAPI one the source
 * mod used. Vanilla b1.7.3 has one flat achievement list and no notion of a page at all, so without
 * RetroAPI this whole feature does nothing: {@link #init()} checks {@link Mods#HAS_RETROAPI} once
 * and returns immediately, and every {@code Achievement} field in the three page classes stays
 * {@code null} - see {@code Config.SCORING.showScoreOnAchievement}, which is greyed out on the config
 * screen for the same reason ({@code Opt#requires()}).
 *
 * <p>Reads the EXISTING per-player score ({@link Score#of}). The counters are already tracked by the
 * scoring mixins for the HUD and death-screen displays - this only adds a third place that shows
 * them; nothing here recomputes a score or changes what gets saved.
 */
public final class ScoreAchievements {

	private ScoreAchievements() {}

	/**
	 * Called once, on both sides, from {@link
	 * com.periut.retrotweaks.compat.retroapi.RetroTweaksRetroInitializer#initRetro()} - achievement/
	 * stat ids must agree between client and server, so registration cannot be client-only even though
	 * the page screen is. RetroAPI documents that achievements are to be registered from inside
	 * {@code AchievementRegistrationCallback}, reached here through {@link
	 * ApiBridge#onAchievementsRegister}; that hook only sees the event if it is installed before
	 * RetroAPI fires it, which is exactly what calling this from {@code initRetro()} (rather than the
	 * bare {@code main} entrypoint, whose ordering relative to RetroAPI is unspecified) guarantees.
	 */
	public static void init() {
		if (!Mods.HAS_RETROAPI) return;
		// Whether the three pages belong on the achievements screen follows a setting, so it is
		// answered fresh every time a screen is about to be built rather than once at registration.
		ApiBridge.onAchievementPagesPrepared(ScoreAchievements::applyPageVisibility);
		ApiBridge.onAchievementsRegister(() -> {
			try {
				WaysBasicAchievements.register();
				WaysDaysAchievements.register();
				Ways404Achievements.register();
				RetroTweaks.LOGGER.info("RetroAPI detected - registered WhatAreYouScoring's three WAYS achievement pages.");
			} catch (Throwable t) {
				RetroTweaks.LOGGER.warn("Could not register the WAYS achievement pages with RetroAPI", t);
			}
		});
	}

	/**
	 * Refreshes every page's live descriptions (and grants newly-met ones), from the local player's
	 * own score. Called from the achievements screen, client side only. A no-op when RetroAPI never
	 * built the pages, or there is no local player to read (a dedicated server never opens a screen).
	 */
	/**
	 * Puts the three pages where the setting says they belong: beside the vanilla page on the
	 * achievements screen, or hidden and reachable only through the game menu's WAYS button.
	 *
	 * <p>Applied on every screen open rather than once at registration, because it follows a config
	 * toggle a player can flip while the game is running.
	 */
	public static void applyPageVisibility() {
		if (!Mods.HAS_RETROAPI) return;
		boolean hidden = !Boolean.TRUE.equals(Config.SCORING.waysAchievementPages);
		ApiBridge.setAchievementPageHidden(WaysBasicAchievements.page, hidden);
		ApiBridge.setAchievementPageHidden(WaysDaysAchievements.page, hidden);
		ApiBridge.setAchievementPageHidden(Ways404Achievements.page, hidden);
	}

	public static void refresh() {
		if (!Mods.HAS_RETROAPI) return;
		applyPageVisibility();
		PlayerEntity player = Players.local();
		if (player == null) return;
		Score.Fields f = Score.of(player);
		boolean showScore = Boolean.TRUE.equals(Config.SCORING.showScoreOnAchievement);

		if (Boolean.TRUE.equals(Config.SCORING.basic.enabled)) WaysBasicAchievements.refresh(player, f, showScore);
		if (Boolean.TRUE.equals(Config.SCORING.days.enabled)) WaysDaysAchievements.refresh(player, f, showScore);
		if (Boolean.TRUE.equals(Config.SCORING.challenge404.enabled)) {
			Ways404Achievements.refresh(player, f, showScore);
		}
	}

	/**
	 * One score's total the same way {@code ScoreHudMixin}/{@code DeathScreenScoreMixin} already
	 * compute it: the live current score, plus the previous life's cumulative score when the
	 * difficulty death multiplier carries it forward. Not a second copy of that arithmetic - just the
	 * one addition both of those mixins also do, so the achievement page never disagrees with the HUD.
	 */
	static int total(int current, int prevCumulative) {
		return current + (Boolean.TRUE.equals(Config.SCORING.difficultyDeathMultiplier) ? prevCumulative : 0);
	}

	/** Marks an achievement as a challenge (the diamond-shaped icon border), or does nothing for a
	 *  null one - a registration that failed is already logged once and stays plain-shaped. */
	static Achievement challenge(Achievement achievement) {
		return achievement == null ? null : achievement.challenge();
	}

	/**
	 * Sets an achievement's live description text directly - not a translation key, exactly like
	 * WhatAreYouScoring's own accessor against StationAPI's copy of {@code Achievement}. A no-op for
	 * a null achievement (its registration failed and was already logged once).
	 */
	static void setDescription(Achievement achievement, String description) {
		if (achievement == null) return;
		((AchievementDescriptionAccessor) achievement).retrotweaks$setDescription(description);
	}

	/**
	 * Grants (vanilla {@code incrementStat}) an achievement once its condition is seen true; calling
	 * it again after that is harmless (the achievement is already lit). A no-op for a null
	 * achievement. Two of the 404 challenge achievements ("never sleep", "never wear armour") grant
	 * the moment the tracked event fires at all - the same as WhatAreYouScoring's own
	 * {@code BedBlockMixin}/{@code PlayerInventoryMixin} did - even though the event is what LOSES
	 * the challenge; the icon marks "this was tracked", the description text carries success/failure.
	 */
	static void grantIf(PlayerEntity player, Achievement achievement, boolean unlocked) {
		if (achievement == null || !unlocked) return;
		player.incrementStat(achievement);
	}
}
