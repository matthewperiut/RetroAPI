package com.periut.retroapi.mixin.achievement;

import net.minecraft.achievement.Achievement;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The icon stack of an achievement, so {@link com.periut.retroapi.registry.IdRemap} can repoint an icon
 * built from a modded block/item at its post-remap id (otherwise the achievements screen draws whatever
 * now occupies the old slot, or nothing).
 */
@Mixin(Achievement.class)
public interface AchievementAccessor {

	@Accessor("icon")
	ItemStack retroapi$getIcon();
}
