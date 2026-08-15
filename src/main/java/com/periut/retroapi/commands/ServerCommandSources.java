package com.periut.retroapi.commands;

import com.periut.retroapi.text.Texts;
import com.periut.retroapi.commands.util.ServerUtil;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.world.World;

/**
 * Builds command sources on the server side.
 *
 * <p>Feedback goes out as {@code §}-coded text for now; the rich-component channel upgrades that for
 * clients running the mod (see {@code com.periut.retroapi.commands.network}).
 */
public final class ServerCommandSources {
    private ServerCommandSources() {
    }

    public static RetroCommandSource forPlayer(final ServerPlayerEntity player) {
        final int level = ServerUtil.isOp(player.name) ? RetroCommandSource.LEVEL_OWNER : RetroCommandSource.LEVEL_ALL;

        return new RetroCommandSource(
            message -> ServerFeedback.send(player, message),
            player,
            player.world,
            new Position(player.x, player.y, player.z),
            player.yaw,
            player.pitch,
            player.name,
            level,
            ServerUtil.getServer(),
            false);
    }

    /** The console, or anything else beta hands to its command handler. Always fully privileged. */
    public static RetroCommandSource forConsole(final CommandOutput output) {
        final World world = ServerUtil.getServer().worlds.length > 0 ? ServerUtil.getServer().worlds[0] : null;
        final Position position = world == null
            ? Position.ORIGIN
            : new Position(world.getSpawnPos().x, world.getSpawnPos().y, world.getSpawnPos().z);

        return new RetroCommandSource(
            message -> output.sendMessage(Texts.toLegacy(message)),
            null,
            world,
            position,
            0.0f,
            0.0f,
            output.getName(),
            RetroCommandSource.LEVEL_OWNER,
            ServerUtil.getServer(),
            false);
    }

    /** Split out so the rich-message path has one place to hook into. */
    static final class ServerFeedback {
        private ServerFeedback() {
        }

        static void send(final ServerPlayerEntity player, final com.periut.retroapi.text.Text message) {
            com.periut.retroapi.commands.network.RichMessages.send(player, message);
        }
    }
}
