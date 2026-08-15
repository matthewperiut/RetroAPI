package com.periut.retroapi.commands.dimension;

import com.periut.retroapi.compat.StationBridges;
import com.periut.retroapi.dimension.DimensionHelper;
import com.periut.retroapi.dimension.DimensionRegistration;
import com.periut.retroapi.dimension.RetroDimensionRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Dimension identifiers as a command sees them.
 *
 * <p>Beta has no dimension registry: it has three hard-coded serial ids. RetroAPI registers modded
 * dimensions in {@link RetroDimensionRegistry} and leaves {@code -1}/{@code 0}/{@code 1} reserved for
 * vanilla, so this table names those three and defers to the registry for everything else.
 *
 * <p>StationAPI dimensions live in StationAPI's own registry, not RetroAPI's, so they are reached
 * through the {@code StationBridge} seam - which means a StationAPI dimension is suggested, resolved
 * and teleported into exactly like a RetroAPI one, and neither registry has to know about the other.
 */
public final class DimensionIds {
    private DimensionIds() {
    }

    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String SKYLANDS = "minecraft:the_skylands";

    /** @return the dimension's serial id, or null when nothing is registered under that identifier */
    public static Integer resolve(final String identifier) {
        if (identifier == null) {
            return null;
        }
        final String id = identifier.toLowerCase(Locale.ROOT).indexOf(':') < 0
            ? "minecraft:" + identifier.toLowerCase(Locale.ROOT)
            : identifier.toLowerCase(Locale.ROOT);

        switch (id) {
            case OVERWORLD -> {
                return RetroDimensionRegistry.OVERWORLD_ID;
            }
            case NETHER -> {
                return RetroDimensionRegistry.NETHER_ID;
            }
            case SKYLANDS -> {
                return RetroDimensionRegistry.SKYLANDS_ID;
            }
            default -> {
            }
        }

        for (final DimensionRegistration registration : RetroDimensionRegistry.getAll()) {
            if (registration.getId().toString().equals(id)) {
                return registration.getSerialId();
            }
        }
        return null;
    }

    /**
     * Moves a player into whichever dimension an identifier names, whoever registered it.
     *
     * @return false when nothing answers to that identifier
     */
    public static boolean switchTo(final PlayerEntity player, final String identifier) {
        final Integer serialId = resolve(identifier);
        if (serialId != null) {
            DimensionHelper.switchToDimensionId(player, serialId, 1.0, new BareTravelAgent());
            return true;
        }
        // Not one of ours: StationAPI keeps its dimensions in its own registry and does its own
        // transfer (portal placement, world provider), so hand the whole move over to it.
        return stationApiPresent() && StationBridges.get().switchDimension(player, identifier);
    }

    /** The identifier a serial id answers to, for feedback messages. */
    public static String nameOf(final int serialId) {
        switch (serialId) {
            case RetroDimensionRegistry.OVERWORLD_ID -> {
                return OVERWORLD;
            }
            case RetroDimensionRegistry.NETHER_ID -> {
                return NETHER;
            }
            case RetroDimensionRegistry.SKYLANDS_ID -> {
                return SKYLANDS;
            }
            default -> {
            }
        }
        final DimensionRegistration registration = RetroDimensionRegistry.getBySerialId(serialId);
        if (registration != null) {
            return registration.getId().toString();
        }
        if (stationApiPresent()) {
            final String stationId = StationBridges.get().dimensionIdentifier(serialId);
            if (stationId != null) {
                return stationId;
            }
        }
        return String.valueOf(serialId);
    }

    /** Everything worth suggesting: the three vanilla ids, then whatever any registry added. */
    public static List<String> all() {
        final LinkedHashSet<String> ids = new LinkedHashSet<>(List.of(OVERWORLD, NETHER, SKYLANDS));
        for (final DimensionRegistration registration : RetroDimensionRegistry.getAll()) {
            ids.add(registration.getId().toString());
        }
        if (stationApiPresent()) {
            ids.addAll(StationBridges.get().dimensionIds());
        }
        return new ArrayList<>(ids);
    }

    private static boolean stationApiPresent() {
        return FabricLoader.getInstance().isModLoaded("stationapi");
    }
}
