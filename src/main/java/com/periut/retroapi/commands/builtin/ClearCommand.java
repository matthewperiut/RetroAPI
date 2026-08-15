package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.argument.ItemStackArgument;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntityArgumentType.getPlayers;
import static com.periut.retroapi.commands.argument.EntityArgumentType.players;
import static com.periut.retroapi.commands.argument.ItemArgumentType.getItem;
import static com.periut.retroapi.commands.argument.ItemArgumentType.item;

/**
 * {@code /clear [targets] [item] [maxCount]} - modern Minecraft's inventory clear.
 *
 * <p>This name used to mean "clear the chat window" in this mod. Modern wins the name; the chat
 * version now lives at {@code /clearchat}.
 */
public final class ClearCommand {
    private ClearCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("clear")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .executes(context -> clear(context, Collections.singletonList(context.getSource().getPlayerOrThrow()), null, Integer.MAX_VALUE))
            .then(argument("targets", players())
                .executes(context -> clear(context, getPlayers(context, "targets"), null, Integer.MAX_VALUE))
                .then(argument("item", item())
                    .executes(context -> clear(context, getPlayers(context, "targets"), getItem(context, "item"), Integer.MAX_VALUE))
                    .then(argument("maxCount", integer(0))
                        .executes(context -> clear(context, getPlayers(context, "targets"), getItem(context, "item"), getInteger(context, "maxCount")))))));
    }

    private static int clear(final CommandContext<RetroCommandSource> context, final List<PlayerEntity> targets,
                             final ItemStackArgument filter, final int maxCount) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        int cleared = 0;

        for (final PlayerEntity target : targets) {
            cleared += clearInventory(target, filter, maxCount - cleared);
        }

        if (cleared == 0) {
            source.sendError(Text.literal("No items were found on " + (targets.size() == 1 ? targets.get(0).name : targets.size() + " players")));
            return 0;
        }

        if (targets.size() == 1) {
            source.sendFeedback(Text.literal("Removed " + cleared + " items from " + targets.get(0).name));
        } else {
            source.sendFeedback(Text.literal("Removed " + cleared + " items from " + targets.size() + " players"));
        }
        return Command.SINGLE_SUCCESS;
    }

    /** @param remaining how many more items may be removed; {@link Integer#MAX_VALUE} means all */
    private static int clearInventory(final PlayerEntity player, final ItemStackArgument filter, final int remaining) {
        if (remaining <= 0) {
            return 0;
        }

        int cleared = 0;
        cleared += clearArray(player.inventory.main, filter, remaining - cleared);
        cleared += clearArray(player.inventory.armor, filter, remaining - cleared);
        return cleared;
    }

    private static int clearArray(final ItemStack[] slots, final ItemStackArgument filter, final int remaining) {
        int budget = remaining;
        int cleared = 0;

        for (int slot = 0; slot < slots.length && budget > 0; slot++) {
            final ItemStack stack = slots[slot];
            if (stack == null || !matches(stack, filter)) {
                continue;
            }

            if (stack.count <= budget) {
                cleared += stack.count;
                budget -= stack.count;
                slots[slot] = null;
            } else {
                cleared += budget;
                stack.count -= budget;
                budget = 0;
            }
        }

        return cleared;
    }

    private static boolean matches(final ItemStack stack, final ItemStackArgument filter) {
        if (filter == null) {
            return true;
        }
        // A subtype of 0 means "any subtype", matching how a bare item name behaves elsewhere.
        return stack.itemId == filter.itemId() && (filter.meta() == 0 || stack.getDamage() == filter.meta());
    }
}
