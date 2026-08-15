package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;

/** {@code /heal [amount]} - full health, or a specific number of half-hearts. */
public final class HealCommand {
    private static final int FULL_HEALTH = 20;

    private HealCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("heal")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(HealCommand::healFully)
            .then(argument("amount", integer())
                .executes(context -> healBy(context, getInteger(context, "amount")))));
    }

    private static int healFully(final CommandContext<RetroCommandSource> context) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayerOrThrow();

        player.health = FULL_HEALTH + extraHealth(player);
        source.sendFeedback(Text.literal("Healed fully"));
        return Command.SINGLE_SUCCESS;
    }

    private static int healBy(final CommandContext<RetroCommandSource> context, final int amount) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final PlayerEntity player = source.getPlayerOrThrow();

        player.health = Math.min(player.health + amount, FULL_HEALTH + extraHealth(player));
        source.sendFeedback(Text.literal((amount > 0 ? "Healed " : "Damaged ") + Math.abs(amount) / 2.0f + " hearts"));
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Accessory API, if installed, can raise a player's maximum; healing to a flat twenty would
     * quietly remove the extra. Reached reflectively because the mod is optional.
     */
    private static int extraHealth(final PlayerEntity player) {
        if (!FabricLoader.getInstance().isModLoaded("accessoryapi")) {
            return 0;
        }
        try {
            final Class<?> extraHp = Class.forName("com.periut.accessoryapi.api.PlayerExtraHP");
            if (extraHp.isInstance(player)) {
                return ((Number) extraHp.getMethod("getExtraHP").invoke(player)).intValue();
            }
        } catch (final ReflectiveOperationException | RuntimeException ignored) {
            // An incompatible version is the same as not having it.
        }
        return 0;
    }
}
