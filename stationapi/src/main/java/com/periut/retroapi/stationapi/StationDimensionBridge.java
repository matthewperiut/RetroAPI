package com.periut.retroapi.stationapi;

import com.periut.retroapi.commands.dimension.BareTravelAgent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.modificationstation.stationapi.api.registry.DimensionRegistry;
import net.modificationstation.stationapi.api.util.Identifier;
import net.modificationstation.stationapi.api.world.dimension.DimensionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * StationAPI's dimension registry, seen through RetroAPI's {@code StationBridge}.
 *
 * <p>Every StationAPI dimension type stays inside this class, and every entry point checks that
 * {@code station-dimensions-v0} is actually loaded first: StationAPI itself can be installed without its
 * dimension module, and a class reference resolved in that install is a {@link NoClassDefFoundError}.
 */
final class StationDimensionBridge {
    private StationDimensionBridge() {
    }

    static boolean available() {
        return FabricLoader.getInstance().isModLoaded("station-dimensions-v0");
    }

    static List<String> ids() {
        if (!available()) {
            return List.of();
        }
        try {
            final List<String> ids = new ArrayList<>();
            for (final Identifier id : DimensionRegistry.INSTANCE.getIds()) {
                ids.add(id.namespace.toString() + ":" + id.path);
            }
            return ids;
        } catch (final RuntimeException | LinkageError ignored) {
            return List.of();
        }
    }

    /** @return false when nothing is registered under that identifier */
    static boolean switchTo(final PlayerEntity player, final String identifier) {
        if (!available()) {
            return false;
        }
        try {
            final Identifier id = Identifier.tryParse(identifier);
            if (id == null || DimensionRegistry.INSTANCE.get(id) == null) {
                return false;
            }
            DimensionHelper.switchDimension(player, id, 1, new BareTravelAgent());
            return true;
        } catch (final RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    static String identifierOf(final int serialId) {
        if (!available()) {
            return null;
        }
        try {
            return DimensionRegistry.INSTANCE.getId(serialId)
                .map(id -> id.namespace.toString() + ":" + id.path)
                .orElse(null);
        } catch (final RuntimeException | LinkageError ignored) {
            return null;
        }
    }
}
