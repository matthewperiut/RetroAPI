package com.periut.retroapi.mixin.client;

import net.minecraft.world.World;
import net.minecraft.world.WorldRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the World behind the render-time {@link WorldRegion} BlockView wrapper, so
 * state/xmeta reads and dimension checks keep working during chunk rendering (which never
 * hands renderers the World itself). See {@code RetroWorlds.unwrap}.
 */
@Mixin(WorldRegion.class)
public interface WorldRegionAccessor {

	@Accessor("world")
	World retroapi$getWorld();
}
