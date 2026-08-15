package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;

import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;

/**
 * {@code /god [enabled]} - invincibility, checked by the damage mixin.
 *
 * <p>Keyed by name rather than by entity because a player who reconnects is a different object but
 * the same person.
 */
public final class GodCommand {
    private static final Map<String, Boolean> INVINCIBLE = new HashMap<>();

    private GodCommand() {
    }

    public static boolean isInvincible(final String playerName) {
        return Boolean.TRUE.equals(INVINCIBLE.get(playerName));
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("god")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(context -> set(context.getSource(), !isInvincible(context.getSource().getPlayerOrThrow().name)))
            .then(argument("enabled", bool())
                .executes(context -> set(context.getSource(), getBool(context, "enabled")))));
    }

    private static int set(final RetroCommandSource source, final boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final PlayerEntity player = source.getPlayerOrThrow();
        INVINCIBLE.put(player.name, enabled);
        source.sendFeedback(Text.literal("God mode " + (enabled ? "activated" : "deactivated")));
        return Command.SINGLE_SUCCESS;
    }
}
