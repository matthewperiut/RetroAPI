package com.periut.retroapi.commands.network;

import com.periut.retroapi.commands.RetroCommandsNetworking;
import com.periut.retroapi.text.Text;
import com.periut.retroapi.text.TextCodec;
import com.periut.retroapi.text.Texts;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

/**
 * Sends command output to a player as a component when their client can render one, and as a
 * {@code §}-coded chat line when it cannot.
 *
 * <p>Colour survives either way; hover text and click actions only reach clients running this mod,
 * which is the same trade modern Minecraft makes when talking to an old client.
 */
public final class RichMessages {
    private RichMessages() {
    }

    public static void send(final ServerPlayerEntity player, final Text message) {
        if (ServerPlayNetworking.isPlayReady(player, RetroCommandsNetworking.MESSAGE_CHANNEL)) {
            final String json = TextCodec.toJson(message);
            ServerPlayNetworking.send(player, RetroCommandsNetworking.MESSAGE_CHANNEL, buffer -> buffer.writeString(json));
            return;
        }

        player.sendMessage(Texts.toLegacy(message));
    }
}
