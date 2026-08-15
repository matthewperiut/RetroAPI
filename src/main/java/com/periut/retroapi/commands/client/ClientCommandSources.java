package com.periut.retroapi.commands.client;

import com.periut.retroapi.commands.RetroCommands;
import com.periut.retroapi.commands.client.gui.RetroChatHud;
import com.periut.retroapi.commands.Position;
import com.periut.retroapi.commands.RetroCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResultType;

/**
 * Builds the command source a client runs commands as.
 *
 * <p>In singleplayer that is the whole story - beta has no integrated server, so the client is the
 * authority and holds every permission. Connected to a server, this source exists only to parse and
 * suggest; the server builds its own when the command actually arrives.
 */
public final class ClientCommandSources {
    private ClientCommandSources() {
    }

    public static RetroCommandSource create(final Minecraft minecraft) {
        final ClientPlayerEntity player = minecraft.player;

        // Fully privileged, on both sides of the wire, because this source only ever parses, colours and
        // completes - it never runs anything.
        //
        // Filtering here as well as on the server was worse than useless: the tree a server sends has
        // already had everything this player may not use removed, so a second filter can only take away
        // MORE, and it did - a client whose op status had not arrived (or had arrived wrong) parsed
        // /gamerule against a tree it had quietly pruned and reported "Unknown command" for a command
        // the server was cheerfully suggesting. Modern does not second-guess the tree either.
        //
        // Nothing is granted by this: a command still travels to the server, which builds its own source
        // and re-checks the rights before running a thing.
        final int level = RetroCommandSource.LEVEL_OWNER;

        return new RetroCommandSource(
            message -> RetroChatHud.getInstance().addMessage(message),
            player,
            minecraft.world,
            player == null ? Position.ORIGIN : new Position(player.x, player.y, player.z),
            player == null ? 0.0f : player.yaw,
            player == null ? 0.0f : player.pitch,
            player == null ? "client" : player.name,
            level,
            null,
            true).withLookedAtBlock(lookedAtBlock(minecraft));
    }

    /**
     * The block under the crosshair, which is what coordinate arguments complete to. Null unless the
     * player is actually looking at one - an entity under the crosshair is not a position, and
     * neither is thin air, and in both cases the completions fall back to {@code ~ ~ ~}.
     */
    private static Position lookedAtBlock(final Minecraft minecraft) {
        final HitResult target = minecraft.crosshairTarget;
        if (target == null || target.type != HitResultType.BLOCK) {
            return null;
        }
        return new Position(target.blockX, target.blockY, target.blockZ);
    }
}
