package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.CommandUtil;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.api.ItemInstanceStr;
import com.periut.retroapi.commands.argument.ItemStackArgument;
import com.periut.retroapi.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.EntitySummonArgumentType.entitySummon;
import static com.periut.retroapi.commands.argument.EntitySummonArgumentType.getEntitySummon;
import static com.periut.retroapi.commands.argument.EntityArgumentType.getPlayers;
import static com.periut.retroapi.commands.argument.EntityArgumentType.players;
import static com.periut.retroapi.commands.argument.ItemArgumentType.getItem;
import static com.periut.retroapi.commands.argument.ItemArgumentType.item;

/**
 * {@code /give <targets> <item> [count]}, in modern Minecraft's shape.
 *
 * <p>The targets argument is required, as it is in modern - the old {@code /give <item>} form is
 * gone. Items are named ({@code minecraft:stone}, or {@code minecraft:wool:14} for a subtype), and a
 * count larger than a stack is split across as many slots as it takes, again as modern does.
 */
public final class GiveCommand {
    /** Beta's monster spawner, the one item that takes an entity argument. */
    private static final int SPAWNER_ID = 52;

    private GiveCommand() {
    }

    public static void register(final CommandDispatcher<RetroCommandSource> dispatcher, final RegistrationEnvironment environment) {
        dispatcher.register(literal("give")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_MODERATOR))
            .then(argument("targets", players())
                .then(argument("item", item())
                    .executes(context -> give(context, 1, null))
                    .then(argument("count", integer(1))
                        .executes(context -> give(context, getInteger(context, "count"), null))
                        .then(argument("entity", entitySummon())
                            .executes(context -> give(context, getInteger(context, "count"), getEntitySummon(context, "entity"))))))));
    }

    /**
     * @param entity the mob to put inside a spawner, or null.
     *               <p>Modern would carry this as NBT on the stack. Beta has no NBT argument and no
     *               parser for one, so a spawner takes the entity as a plain trailing word; every
     *               other item rejects it rather than silently ignoring it.
     */
    private static int give(final CommandContext<RetroCommandSource> context, final int count, final String entity) throws CommandSyntaxException {
        final RetroCommandSource source = context.getSource();
        final ItemStackArgument item = getItem(context, "item");
        final List<PlayerEntity> targets = getPlayers(context, "targets");
        final int maxCount = item.getMaxCount();

        if (entity != null && item.itemId() != SPAWNER_ID) {
            source.sendError(Text.literal("Only a spawner can be given an entity"));
            return 0;
        }

        int given = 0;
        int lastGiven = 0;
        for (final PlayerEntity target : targets) {
            int remaining = count;
            boolean full = false;

            while (remaining > 0 && !full) {
                final int stackSize = Math.min(remaining, maxCount);
                final ItemStack stack = item.createStack(stackSize);
                if (entity != null) {
                    ((ItemInstanceStr) (Object) stack).spc$setStr(entity);
                }
                // Merging means a stack can go in PARTLY - topped onto what was already there until
                // the inventory ran out - so count what actually went in rather than what was asked.
                final int inserted = CommandUtil.give(source, target, stack);
                if (inserted > 0) {
                    remaining -= inserted;
                } else {
                    full = true;
                }
            }

            if (remaining < count) {
                given++;
                lastGiven = count - remaining;
            }
        }

        if (given == 0) {
            return 0;
        }

        final Text description = CommandUtil.describeItem(item);
        if (targets.size() == 1) {
            // What went in, not what was asked for: a full inventory takes part of it and says so.
            source.sendFeedback(Text.literal("Gave " + lastGiven + " ").append(description).append(" to " + targets.get(0).name));
        } else {
            source.sendFeedback(Text.literal("Gave " + count + " ").append(description).append(" to " + given + " players"));
        }

        return Command.SINGLE_SUCCESS;
    }
}
