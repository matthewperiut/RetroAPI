package com.periut.retroapi.testmod;

import com.periut.retroapi.achievement.AchievementPage;
import com.periut.retroapi.achievement.RetroAchievements;
import net.minecraft.achievement.Achievement;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

/**
 * Test-mod achievement registration exercising the RetroAPI achievement subsystem:
 * a root achievement, a child-with-parent, and a custom {@link AchievementPage}.
 */
public final class TestAchievements {
	private TestAchievements() {}

	public static Achievement ROOT;
	public static Achievement CHILD;

	private static NamespacedIdentifier id(String name) {
		return NamespacedIdentifiers.from("retroapi_test", name);
	}

	public static void register() {
		// Root achievement (no parent), icon = a vanilla block.
		// The SHORT name is passed; the vanilla ctor prepends "achievement.", so the title lang key is
		// "achievement.retroapi_test.test_root" and the description key is "...test_root.desc".
		ROOT = RetroAchievements.register(
			id("test_root"),
			"retroapi_test.test_root",
			0, 0,
			Block.STONE,
			null
		);

		// Child achievement with the root as its parent, icon = a vanilla item.
		CHILD = RetroAchievements.register(
			id("test_child"),
			"retroapi_test.test_child",
			2, 1,
			Item.DIAMOND,
			ROOT
		);

		// Custom page holding the test achievements (drives the <, > page buttons).
		AchievementPage page = new AchievementPage(id("test_page"));
		page.addAchievements(ROOT, CHILD);

		TestMod.LOGGER.info("Registered test achievements: root={} child={} (page count now {})",
			ROOT.id, CHILD.id, AchievementPage.getPageCount());
	}
}
