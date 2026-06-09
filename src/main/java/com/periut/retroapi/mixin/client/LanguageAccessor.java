package com.periut.retroapi.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Properties;
import net.minecraft.client.resource.language.TranslationStorage;

@Mixin(TranslationStorage.class)
public interface LanguageAccessor {
	@Accessor("translations")
	Properties retroapi$getTranslations();
}
