package com.periut.retroapi.commands.api;

import com.periut.retroapi.commands.Position;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entity types with summon-time options, keyed by class.
 *
 * <p>Anything not registered here is still summonable - {@code /summon} falls back to beta's own
 * entity registry - so a factory is only needed for types that take arguments.
 */
public class SummonRegistry {
    private static final Map<Class<? extends Entity>, SummonFactory> FACTORIES = new HashMap<>();
    private static final Map<Class<? extends Entity>, String> USAGE = new LinkedHashMap<>();

    /**
     * @param usage how the arguments read in help, e.g. {@code "<charged (0 or 1)>"}
     */
    public static void add(final Class<? extends Entity> type, final SummonFactory factory, final String usage) {
        final SummonFactory previous = FACTORIES.put(type, factory);
        if (previous != null) {
            System.out.println("[retroapi] Overwrote " + previous + " with " + factory + " for summoning " + type);
        }
        USAGE.put(type, usage);
    }

    public static Entity create(final Class<? extends Entity> type, final World world, final Position position, final String[] arguments) {
        final SummonFactory factory = FACTORIES.get(type);
        return factory == null ? null : factory.create(world, position, arguments);
    }

    public static boolean hasFactory(final Class<? extends Entity> type) {
        return FACTORIES.containsKey(type);
    }

    public static String usageFor(final Class<? extends Entity> type) {
        return USAGE.get(type);
    }

    public static Map<Class<? extends Entity>, String> usages() {
        return USAGE;
    }
}
