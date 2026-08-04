package com.periut.retroapi.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.InteractionManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reaches {@code InteractionManager.minecraft} from a mixin on one of its SUBCLASSES.
 *
 * <p>The field is declared on the base class, so a mixin targeting {@code SingleplayerInteractionManager}
 * or {@code MultiplayerInteractionManager} cannot {@code @Shadow} it: shadowing resolves against the
 * target class itself. Everything that needs the world while handling a break lives on the subclasses,
 * because that is where the interesting methods are overridden, so it goes through here instead.
 */
@Environment(EnvType.CLIENT)
@Mixin(InteractionManager.class)
public interface InteractionManagerAccessor {

	@Accessor("minecraft")
	Minecraft retroapi$minecraft();
}
