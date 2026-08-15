package com.periut.retroapi.itemgroup;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.registry.BlockRegistration;
import com.periut.retroapi.registry.ItemRegistration;
import com.periut.retroapi.registry.RetroRegistry;
import com.periut.retroapi.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;
import net.ornithemc.osl.core.api.util.NamespacedIdentifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A creative tab for a mod that registered content but no tab of its own.
 *
 * <p>Content with nowhere to be found is content nobody finds. So a mod that adds blocks or items and
 * never says where they go gets a tab named after itself - the name out of its own
 * {@code fabric.mod.json} - holding everything it registered, in the order it registered it, with the
 * first of those as the icon.
 *
 * <p><b>It steps aside.</b> Registering any tab under your own namespace is enough to be left alone;
 * this only fills a gap, it never competes. And it can be turned off outright:
 *
 * <pre>{@code
 * AutoItemGroups.exclude("mymod");
 * }</pre>
 *
 * <p>Nothing happens under StationAPI, where blocks and items are registered through StationAPI's own
 * registries rather than RetroAPI's, so there is nothing here to collect.
 */
public final class AutoItemGroups {
    private AutoItemGroups() {
    }

    /** Namespaces RetroAPI's own content uses, which have tabs already. */
    private static final Set<String> RESERVED = Set.of("minecraft", "retroapi");

    private static final Set<String> EXCLUDED = new LinkedHashSet<>();

    /** No automatic tab for this mod, even if it registers content and no tab. */
    public static void exclude(final String namespace) {
        if (namespace != null && !namespace.isEmpty()) {
            EXCLUDED.add(namespace);
        }
    }

    /**
     * Called from RetroAPI's init, after every mod has had its chance to register content AND its own
     * tabs - so "did this mod make a tab" has its final answer by the time it is asked.
     */
    public static void registerAll() {
        final Map<String, List<Object>> byMod = new LinkedHashMap<>();

        for (final Object registration : RetroRegistry.getRegistrationOrder()) {
            final NamespacedIdentifier id = idOf(registration);
            if (id == null || RESERVED.contains(id.namespace()) || EXCLUDED.contains(id.namespace())) {
                continue;
            }
            byMod.computeIfAbsent(id.namespace(), key -> new ArrayList<>()).add(registration);
        }

        byMod.forEach((namespace, content) -> {
            if (hasOwnGroup(namespace)) {
                return;
            }
            register(namespace, content);
            explain(namespace, content);
        });
    }

    private static void register(final String namespace, final List<Object> content) {
        final String name = modName(namespace);

        RetroItemGroup.builder(NamespacedIdentifiers.from(namespace, "items"))
            .displayName(Text.literal(name))
            .icon(() -> stackOf(content.get(0)))
            .entries(entries -> {
                for (final Object registration : content) {
                    if (registration instanceof BlockRegistration block) {
                        entries.add(block.getBlock());
                    } else if (registration instanceof ItemRegistration item) {
                        entries.add(item.getItem());
                    }
                }
            })
            .build();
    }

    /** Whether this mod already put something on the tab strip itself. */
    private static boolean hasOwnGroup(final String namespace) {
        for (final RetroItemGroup group : RetroItemGroups.all()) {
            if (group.getId() != null && namespace.equals(group.getId().namespace())) {
                return true;
            }
        }
        return false;
    }

    /** The mod's own display name, falling back to its id when it has none. */
    private static String modName(final String namespace) {
        return FabricLoader.getInstance().getModContainer(namespace)
            .map(container -> container.getMetadata().getName())
            .filter(name -> name != null && !name.isEmpty())
            .orElse(namespace);
    }

    private static void explain(final String namespace, final List<Object> content) {
        final NamespacedIdentifier first = idOf(content.get(0));
        final String firstName = first == null ? "MY_BLOCK" : first.identifier().toUpperCase(java.util.Locale.ROOT);

        RetroAPI.LOGGER.info("[RetroAPI] {} notice: no creative tab of your own was found, so one named \"{}\" "
                + "was generated for you, holding all {} of your blocks and items in registration order."
                + "\nTo build your own instead - registering ANY tab under your namespace turns this off:"
                + "\n    RetroItemGroup.builder(NamespacedIdentifiers.from(\"{}\", \"items\"))"
                + "\n        .displayName(Text.literal(\"{}\"))"
                + "\n        .icon(() -> new ItemStack({}))"
                + "\n        .entries(entries -> {{"
                + "\n            entries.add({});"
                + "\n        }})"
                + "\n        .build();"
                + "\nTo add to a tab that already exists:"
                + "\n    RetroItemGroups.modifyEntries(VanillaItemGroups.INGREDIENTS, entries -> entries.add({}));"
                + "\nTo have no tab at all:"
                + "\n    AutoItemGroups.exclude(\"{}\");",
            namespace, modName(namespace), content.size(), namespace, modName(namespace),
            firstName, firstName, firstName, namespace);
    }

    private static NamespacedIdentifier idOf(final Object registration) {
        if (registration instanceof BlockRegistration block) {
            return block.getId();
        }
        if (registration instanceof ItemRegistration item) {
            return item.getId();
        }
        return null;
    }

    private static ItemStack stackOf(final Object registration) {
        if (registration instanceof BlockRegistration block) {
            return new ItemStack(block.getBlock());
        }
        if (registration instanceof ItemRegistration item) {
            return new ItemStack(item.getItem());
        }
        return new ItemStack(net.minecraft.block.Block.STONE);
    }
}
