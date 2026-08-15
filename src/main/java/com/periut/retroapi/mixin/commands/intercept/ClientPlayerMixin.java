package com.periut.retroapi.mixin.commands.intercept;

import com.periut.retroapi.commands.RetroCommands;
import com.periut.retroapi.commands.client.gui.RetroChatHud;
import com.periut.retroapi.text.Text;
import net.minecraft.client.network.MultiplayerClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;

/**
 * Client-only commands that never reach a server.
 *
 * <p>{@code /clearchat} clears a window the server knows nothing about, and {@code /perm} reports
 * what this client believes about its own permissions - both are answered here rather than being
 * sent off to be rejected.
 */
@Mixin(MultiplayerClientPlayerEntity.class)
public class ClientPlayerMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void retroapi$handleClientOnlyCommands(final String message, final CallbackInfo ci) {
        if (message.startsWith("/clearchat")) {
            RetroChatHud.getInstance().clear();
            ci.cancel();
            return;
        }

        if (message.startsWith("/perm")) {
            final RetroChatHud chat = RetroChatHud.getInstance();
            chat.addMessage(Text.literal("Server runs Retro Commands: " + RetroCommands.mp_rc));
            chat.addMessage(Text.literal("Operator: " + RetroCommands.mp_op));
            chat.addMessage(Text.literal("Known players: " + Arrays.toString(RetroCommands.player_names)));
            ci.cancel();
        }
    }
}
