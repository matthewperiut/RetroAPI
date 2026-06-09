package com.periut.retroapi.mixin.client.achievement;

import com.periut.retroapi.achievement.AchievementPage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.achievement.Achievement;
import net.minecraft.achievement.Achievements;
import net.minecraft.client.gui.screen.AchievementsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds StationAPI-style multi-page navigation to the vanilla achievements screen for the
 * non-StationAPI path. Disabled by {@link com.periut.retroapi.mixin.RetroAPIMixinPlugin}
 * when StationAPI is present (StationAPI owns the screen then).
 *
 * <p>This mixin is intentionally SEPARATE from the atlas {@code AchievementsScreenMixin}'s
 * {@code drawTexture @Redirect} and uses only COARSE injection points (method HEAD/TAIL),
 * avoiding the brittle {@code @Local}/{@code @ModifyConstant}/ordinal capture StationAPI relies
 * on - RetroAPI's {@code renderIcons} signature is {@code (IIF)V} (extra float) so those would
 * not transfer. Per-page icon/line filtering is achieved by temporarily swapping
 * {@link Achievements#ACHIEVEMENTS} (which {@code renderIcons} reads directly) to the current
 * page's list for the duration of the method, then restoring it.
 */
@Mixin(AchievementsScreen.class)
@Environment(EnvType.CLIENT)
public abstract class AchievementsPageScreenMixin extends Screen {

	// textRenderer (and buttons/width/height) are inherited protected members of Screen, which this
	// mixin extends - so they are accessed directly, NOT via @Shadow. Shadowing textRenderer fails
	// because Mixin looks for it on the target AchievementsScreen, where it is not declared.

	@Unique
	private static final int RETROAPI$PREV_BUTTON_ID = "retroapi:achievement_prev".hashCode();
	@Unique
	private static final int RETROAPI$NEXT_BUTTON_ID = "retroapi:achievement_next".hashCode();

	@Unique
	private List<Achievement> retroapi$savedAchievements;

	@SuppressWarnings("unchecked")
	@Inject(method = "init", at = @At("TAIL"))
	private void retroapi$addPageButtons(CallbackInfo ci) {
		if (AchievementPage.getPageCount() <= 1) return;
		this.buttons.add(new ButtonWidget(RETROAPI$PREV_BUTTON_ID, this.width / 2 - 113, this.height / 2 + 74, 20, 20, "<"));
		this.buttons.add(new ButtonWidget(RETROAPI$NEXT_BUTTON_ID, this.width / 2 - 93, this.height / 2 + 74, 20, 20, ">"));
	}

	@Inject(method = "buttonClicked", at = @At("HEAD"))
	private void retroapi$handlePageButtons(ButtonWidget button, CallbackInfo ci) {
		if (AchievementPage.getPageCount() <= 1) return;
		if (button.id == RETROAPI$PREV_BUTTON_ID) AchievementPage.prevPage();
		else if (button.id == RETROAPI$NEXT_BUTTON_ID) AchievementPage.nextPage();
	}

	@Inject(method = "setTitle", at = @At("TAIL"))
	private void retroapi$drawPageTitle(CallbackInfo ci) {
		if (AchievementPage.getPageCount() <= 1) return;
		// Keep the vanilla "Achievements" header on the implicit default page.
		if (AchievementPage.isCurrentPageDefault()) return;
		String key = "gui.retroapi.achievementPage." + AchievementPage.getCurrentPageName();
		String text = I18n.getTranslation(key);
		this.textRenderer.draw(text, this.width / 2 - 69, this.height / 2 + 80, 0xFFFFFF);
	}

	// --- Per-page filtering: swap the static list renderIcons reads, then restore. ---

	@Inject(method = "renderIcons", at = @At("HEAD"))
	private void retroapi$filterToCurrentPage(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (AchievementPage.getPageCount() <= 1) return;
		AchievementPage page = AchievementPage.getCurrentPage();
		if (page == null) return;
		retroapi$savedAchievements = Achievements.ACHIEVEMENTS;
		Achievements.ACHIEVEMENTS = new ArrayList<>(page.getAchievements());
	}

	@Inject(method = "renderIcons", at = @At("RETURN"))
	private void retroapi$restoreAllAchievements(int mouseX, int mouseY, float delta, CallbackInfo ci) {
		if (retroapi$savedAchievements != null) {
			Achievements.ACHIEVEMENTS = retroapi$savedAchievements;
			retroapi$savedAchievements = null;
		}
	}
}
