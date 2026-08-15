package com.periut.retroapi.commands.api;

import com.periut.retroapi.commands.Position;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Builds an entity for {@code /summon}.
 *
 * <p>Register one with {@link SummonRegistry} to give an entity type summon-time options - the
 * charge on a creeper, the colour of a sheep. Beta has no NBT argument to carry them, so they arrive
 * as the words typed after the position.
 */
@FunctionalInterface
public interface SummonFactory {
    /**
     * @param arguments the extra words typed after the position, never null and possibly empty
     * @return the entity to spawn, already configured; null to report the arguments as invalid
     */
    Entity create(World world, Position position, String[] arguments);
}
