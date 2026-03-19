package com.periut.retroapi.mixin.client;

import net.minecraft.locale.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Properties;

@Mixin(Language.class)
public interface LanguageAccessor {
	@Accessor("translations")
	Properties retroapi$getTranslations();
}
