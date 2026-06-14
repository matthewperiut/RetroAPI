package com.periut.retroapi.entity.client;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

/**
 * Client-side factory for a non-living modded entity whose constructor takes only the {@link World}
 * (position is applied by RetroAPI after construction, from the spawn packet). This is the non-living
 * counterpart of {@link MobFactory} - same {@code (World)} shape, but it returns a plain {@link Entity}
 * instead of a {@code LivingEntity}, so entities that {@code extends Entity} register exactly as cleanly
 * as mobs do:
 *
 * <pre>{@code
 * RetroEntities.register(MyEntity.ID, MyEntity.class)
 *     .factory((world) -> new MyEntity(world));   // MyEntity(World)
 * }</pre>
 *
 * <p>Use {@link EntityFactory} instead when the entity's constructor itself takes the spawn coordinates
 * ({@code (World, x, y, z)}, e.g. arrow-like projectiles).
 *
 * <p>A lambda as above always resolves cleanly. A constructor reference ({@code MyEntity::new}) also works
 * when the class has a single {@code (World)} constructor, but if it declares several constructors the
 * reference is <em>inexact</em> and you must say which factory you mean - cast it the same way the living
 * overload is used: {@code .factory((SimpleEntityFactory) MyEntity::new)} (mirrors
 * {@code .factory((MobFactory) MyMob::new)}).
 */
@FunctionalInterface
public interface SimpleEntityFactory {
	Entity create(World world);
}
