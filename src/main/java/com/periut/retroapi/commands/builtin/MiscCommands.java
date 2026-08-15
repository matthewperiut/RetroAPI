package com.periut.retroapi.commands.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.periut.retroapi.commands.RegistrationEnvironment;
import com.periut.retroapi.commands.RetroCommandSource;
import com.periut.retroapi.commands.argument.EntitySummonArgumentType;
import com.periut.retroapi.commands.argument.ItemIds;
import com.periut.retroapi.commands.argument.ItemNames;
import com.periut.retroapi.commands.argument.ItemStackArgument;
import com.periut.retroapi.commands.optionaldep.cryonicconfig.CryonicConfigCompat;
import com.periut.retroapi.text.Formatting;
import com.periut.retroapi.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.periut.retroapi.commands.RetroCommandManager.argument;
import static com.periut.retroapi.commands.RetroCommandManager.literal;
import static com.periut.retroapi.commands.argument.ItemArgumentType.getItem;
import static com.periut.retroapi.commands.argument.ItemArgumentType.item;

/**
 * The small informational commands, which are one method each and not worth a file apiece:
 * {@code /whoami}, {@code /clock}, {@code /id}, {@code /mobs}, {@code /mods} and
 * {@code /reloadcryonicconfig}.
 */
public final class MiscCommands {
    private static final int LINE_WIDTH = 50;

    private MiscCommands() {
    }

    public static void registerWhoAmI(final CommandDispatcher<RetroCommandSource> dispatcher) {
        dispatcher.register(literal("whoami")
            .executes(context -> {
                context.getSource().sendFeedback(Text.literal(context.getSource().getName()));
                return Command.SINGLE_SUCCESS;
            }));
    }

    public static void registerClock(final CommandDispatcher<RetroCommandSource> dispatcher) {
        dispatcher.register(literal("clock")
            .executes(context -> {
                final RetroCommandSource source = context.getSource();
                final World world = source.getWorld();
                if (world == null) {
                    throw RetroCommandSource.REQUIRES_PLAYER.create();
                }

                final long time = world.getTime();
                source.sendFeedback(Text.literal("Time is " + time));
                source.sendFeedback(Text.literal("Days: " + time / 24000));
                return (int) (time % Integer.MAX_VALUE);
            }));
    }

    public static void registerId(final CommandDispatcher<RetroCommandSource> dispatcher) {
        dispatcher.register(literal("id")
            .then(argument("item", item())
                .executes(context -> {
                    final ItemStackArgument argument = getItem(context, "item");
                    context.getSource().sendFeedback(Text.literal(
                        ItemNames.displayName(argument.itemId(), argument.meta())
                            + " (" + ItemIds.nameOf(argument.itemId()) + ") has id " + argument.itemId()
                            + (argument.meta() == 0 ? "" : ", subtype " + argument.meta())));
                    return argument.itemId();
                })));
    }

    public static void registerMobs(final CommandDispatcher<RetroCommandSource> dispatcher) {
        dispatcher.register(literal("mobs")
            .executes(context -> {
                final RetroCommandSource source = context.getSource();
                final List<String> ids = EntitySummonArgumentType.summonableIds();

                source.sendFeedback(Text.literal("Summonable entities:").formatted(Formatting.YELLOW));
                final StringBuilder line = new StringBuilder();
                for (final String id : ids) {
                    if (line.length() + id.length() + 2 > LINE_WIDTH) {
                        source.sendFeedback(Text.literal(line.toString()));
                        line.setLength(0);
                    }
                    if (line.length() > 0) {
                        line.append(", ");
                    }
                    line.append(id);
                }
                if (line.length() > 0) {
                    source.sendFeedback(Text.literal(line.toString()));
                }

                return ids.size();
            }));
    }

    public static void registerMods(final CommandDispatcher<RetroCommandSource> dispatcher) {
        dispatcher.register(literal("mods")
            .executes(context -> listMods(context.getSource(), false))
            .then(argument("libraries", bool())
                .executes(context -> listMods(context.getSource(), getBool(context, "libraries")))));
    }

    private static int listMods(final RetroCommandSource source, final boolean libraries) {
        final List<String> names = new ArrayList<>();

        FabricLoader.getInstance().getAllMods().forEach(mod -> {
            if (!mod.getMetadata().getType().equals("fabric")) {
                return;
            }
            // Mod Menu's flag is the only marker beta-era mods use to say "I am a library".
            final CustomValue api = mod.getMetadata().getCustomValue("modmenu:api");
            final boolean isLibrary = api != null && api.getAsBoolean();
            if (isLibrary == libraries) {
                names.add(mod.getMetadata().getName());
            }
        });

        names.sort(String.CASE_INSENSITIVE_ORDER);
        source.sendFeedback(Text.literal((libraries ? "Libraries" : "Mods") + " (" + names.size() + "):").formatted(Formatting.YELLOW));
        source.sendFeedback(Text.literal(String.join(", ", names)));
        return names.size();
    }

    public static void registerReloadConfig(final CommandDispatcher<RetroCommandSource> dispatcher) {
        dispatcher.register(literal("reloadcryonicconfig")
            .requires(source -> source.hasPermissionLevel(RetroCommandSource.LEVEL_OWNER))
            .executes(context -> {
                if (CryonicConfigCompat.reload(System.getProperty("user.dir"))) {
                    context.getSource().sendFeedback(Text.literal("Cryonic Config has been refreshed"));
                    return Command.SINGLE_SUCCESS;
                }
                context.getSource().sendError(Text.literal("Cryonic Config is not available"));
                return 0;
            }));
    }
}
