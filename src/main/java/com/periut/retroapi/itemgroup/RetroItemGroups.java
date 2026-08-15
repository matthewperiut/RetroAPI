package com.periut.retroapi.itemgroup;

import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Every creative tab there is, in the order they appear.
 *
 * <p>RetroAPI's own vanilla-content tabs are registered first (see {@link VanillaItemGroups}), so a
 * mod's group lands after them, which is where modern puts modded groups too.
 *
 * <p>To add items to somebody else's group - including a vanilla one - use {@link #modifyEntries},
 * the equivalent of Fabric's {@code ItemGroupEvents.modifyEntriesEvent}.
 */
public final class RetroItemGroups {
    private RetroItemGroups() {
    }

    private static final List<RetroItemGroup> GROUPS = new ArrayList<>();

    static RetroItemGroup register(final RetroItemGroup group) {
        final RetroItemGroup existing = get(group.getId());
        if (existing != null) {
            return existing;
        }
        GROUPS.add(group);
        return group;
    }

    public static List<RetroItemGroup> all() {
        VanillaItemGroups.ensureRegistered();
        return GROUPS;
    }

    public static RetroItemGroup get(final NamespacedIdentifier id) {
        for (final RetroItemGroup group : GROUPS) {
            if (group.getId().equals(id)) {
                return group;
            }
        }
        return null;
    }

    /**
     * Adds entries to an existing group.
     *
     * <pre>{@code
     * RetroItemGroups.modifyEntries(VanillaItemGroups.INGREDIENTS, entries -> entries.add(MY_GEM));
     * }</pre>
     */
    public static void modifyEntries(final RetroItemGroup group, final RetroItemGroup.EntryCollector collector) {
        if (group != null && collector != null) {
            group.addModifier(collector);
        }
    }
}
