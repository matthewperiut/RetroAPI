package com.periut.retroapi.commands.argument;

import com.periut.retroapi.commands.argument.VanillaIds.VanillaItem;
import com.periut.retroapi.compat.StationBridges;
import com.periut.retroapi.registry.RetroRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Turns what a player typed into an item id.
 *
 * <p>{@link VanillaIds} is RetroAPI's own table and always answers for the {@code minecraft}
 * namespace, so identifiers work on a plain b1.7.3 install. RetroAPI's registry and - when it is
 * installed - StationAPI's are consulted only for namespaces that table does not own, which is
 * exactly what they add. StationAPI is reached through the {@code StationBridge} seam, so nothing
 * here links against it.
 */
public final class ItemIds {
    private ItemIds() {
    }

    /** Resolves a token to an item and subtype, or null when nothing answers to that name. */
    public static VanillaItem resolve(final String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            return new VanillaItem(Integer.parseInt(token), 0);
        } catch (final NumberFormatException ignored) {
            // Not a raw id, so it must be a name.
        }

        final String lower = token.toLowerCase(Locale.ROOT);
        final int separator = lower.indexOf(':');
        final String namespace = separator < 0 ? VanillaIds.NAMESPACE : lower.substring(0, separator);
        final String path = separator < 0 ? lower : lower.substring(separator + 1);

        if (VanillaIds.NAMESPACE.equals(namespace)) {
            final VanillaIds.VanillaItem vanilla = VanillaIds.byName(path);
            if (vanilla != null) {
                return vanilla;
            }
            // NOT the end of the search. RetroAPI registers content under `minecraft:` too - the
            // command blocks are minecraft:command_block, because that is what they are - and
            // returning here made every one of them unreachable from /give. Fall through to the
            // registries, which is where anything the table does not know has to be looked up.
        }

        if (stationRegistryPresent()) {
            final int id = StationBridges.get().itemId(token);
            if (id != -1) {
                return new VanillaItem(id, 0);
            }
        }
        final net.minecraft.item.Item registered = RetroRegistry.getItemByStringId(namespace + ":" + path);
        if (registered != null) {
            return new VanillaItem(registered.id, 0);
        }
        // Blocks registered without an explicit item form still have one; ask for the block too.
        final net.minecraft.block.Block block = RetroRegistry.getBlockByStringId(namespace + ":" + path);
        if (block != null) {
            return new VanillaItem(block.id, 0);
        }

        return null;
    }

    /** The name to show a player for an id, always namespaced. */
    public static String nameOf(final int id) {
        final String identifier = identifierOf(id);
        return identifier == null ? String.valueOf(id) : identifier;
    }

    /**
     * The identifier an id is registered under - {@code minecraft:stone},
     * {@code somemod:copper_ingot} - or null if nothing claims it.
     *
     * <p>The mod's own table answers for vanilla; a modded id is looked up in whichever registry
     * put it there. This is what lets a modded item still be named on a dedicated server, where no
     * translation table exists to ask.
     */
    public static String identifierOf(final int id) {
        final String vanilla = VanillaIds.nameOf(id);
        if (vanilla != null) {
            return VanillaIds.NAMESPACE + ":" + vanilla;
        }

        final net.minecraft.item.Item item = itemAt(id);
        if (item == null) {
            return null;
        }

        if (stationRegistryPresent()) {
            final String identifier = StationBridges.get().itemIdentifier(item);
            if (identifier != null) {
                return identifier;
            }
        }
        final var registration = RetroRegistry.getItemRegistration(item);
        if (registration != null && registration.getId() != null) {
            return registration.getId().toString();
        }

        // A BLOCK's item is not in the item registry - the block is what was registered, and its item
        // came along with it - so an id in block range asks the block registry instead. Without this a
        // command block named itself "Item 573", identifier and all, because nothing claimed the id.
        if (id >= 0 && id < net.minecraft.block.Block.BLOCKS.length) {
            final net.minecraft.block.Block block = net.minecraft.block.Block.BLOCKS[id];
            if (block != null) {
                // No StationAPI branch: its bridge names items, and under StationAPI a block's item is
                // in its item registry anyway, so the lookup above has already answered.
                final var blockRegistration = RetroRegistry.getBlockRegistration(block);
                if (blockRegistration != null && blockRegistration.getId() != null) {
                    return blockRegistration.getId().toString();
                }
            }
        }

        return null;
    }

    /**
     * Reaching {@code Item.ITEMS} runs the item and block class initialisers, and those failing
     * raises an Error rather than an exception - never worth taking a command down for.
     */
    private static net.minecraft.item.Item itemAt(final int id) {
        try {
            if (id < 0 || id >= net.minecraft.item.Item.ITEMS.length) {
                return null;
            }
            return net.minecraft.item.Item.ITEMS[id];
        } catch (final RuntimeException | LinkageError ignored) {
            return null;
        }
    }


    /** Every identifier worth suggesting, namespaced as modern Minecraft writes them. */
    public static List<String> allIdentifiers() {
        final TreeSet<String> identifiers = new TreeSet<>();

        for (final String name : VanillaIds.names()) {
            identifiers.add(VanillaIds.NAMESPACE + ":" + name);
        }

        if (stationRegistryPresent()) {
            for (final String identifier : StationBridges.get().itemIdentifiers()) {
                if (!identifier.startsWith(VanillaIds.NAMESPACE + ":")) {
                    identifiers.add(identifier);
                }
            }
        }
        // Everything the registries can resolve, INCLUDING the minecraft: namespace: RetroAPI
        // registers vanilla-named content of its own (command blocks), and a suggestion list that
        // skipped the namespace would never offer them.
        identifiers.addAll(RetroRegistry.getItemIdentifierStrings());
        for (final var registration : RetroRegistry.getBlocks()) {
            if (registration.getId() != null) {
                identifiers.add(registration.getId().toString());
            }
        }

        return new ArrayList<>(identifiers);
    }

    private static boolean stationRegistryPresent() {
        return FabricLoader.getInstance().isModLoaded("station-registry-api-v0");
    }
}
