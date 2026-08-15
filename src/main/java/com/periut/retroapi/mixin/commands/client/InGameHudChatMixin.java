package com.periut.retroapi.mixin.commands.client;

import com.periut.retroapi.commands.client.gui.RetroChatHud;
import com.periut.retroapi.commands.client.gui.RetroChatScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.InGameHud;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;

/**
 * Hands chat over from beta's HUD to {@link RetroChatHud}.
 *
 * <p>Vanilla's chat drawing is silenced by handing its render pass an empty message list rather than
 * by cancelling the whole method - the HUD also draws the hotbar, health, the crosshair and the
 * scoreboard, all of which must keep working. Everything else is a redirect of the four entry
 * points beta uses to add, clear, age and draw messages.
 */
@Mixin(InGameHud.class)
public class InGameHudChatMixin {
    @Shadow private Minecraft minecraft;

    /**
     * Every read of the message list inside {@code render} sees nothing, so vanilla's chat block
     * draws nothing and computes no background for it.
     */
    @Redirect(
        method = "render",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;messages:Ljava/util/List;", opcode = Opcodes.GETFIELD),
        require = 0
    )
    private List<ChatHudLine> retroapi$hideVanillaChat(final InGameHud hud) {
        return Collections.emptyList();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void retroapi$renderChat(final float delta, final boolean menuOpen, final int mouseX, final int mouseY, final CallbackInfo ci) {
        final boolean focused = minecraft != null && minecraft.currentScreen instanceof RetroChatScreen;
        RetroChatHud.getInstance().render(focused);
    }

    @Inject(method = "addChatMessage", at = @At("HEAD"), cancellable = true)
    private void retroapi$addChatMessage(final String message, final CallbackInfo ci) {
        RetroChatHud.getInstance().addLegacyMessage(message);
        ci.cancel();
    }

    @Inject(method = "clearChat", at = @At("HEAD"), cancellable = true)
    private void retroapi$clearChat(final CallbackInfo ci) {
        RetroChatHud.getInstance().clear();
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void retroapi$tickChat(final CallbackInfo ci) {
        RetroChatHud.getInstance().tick();
    }
}
