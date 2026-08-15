package com.periut.retroapi.mixin.commands.access;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the HUD's transient centre-screen message, the one vanilla only ever uses for "Now playing".
 *
 * <p>Beta has no action bar, so this is the only place a short status line can go without spamming chat -
 * which matters for anything that reports repeatedly, like a scroll wheel changing flight speed.
 * {@code setRecordPlayingOverlay} is public but hardcodes its own prefix, hence reaching the fields.
 */
@Mixin(InGameHud.class)
public interface InGameHudAccessor {

    @Accessor("overlayMessage")
    void spc$setOverlayMessage(String message);

    @Accessor("overlayRemaining")
    void spc$setOverlayRemaining(int ticks);

    @Accessor("overlayTinted")
    void spc$setOverlayTinted(boolean tinted);
}
