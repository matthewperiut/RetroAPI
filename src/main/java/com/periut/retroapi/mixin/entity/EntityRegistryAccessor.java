package com.periut.retroapi.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Exposes vanilla {@code EntityRegistry}'s private static string&lt;-&gt;class maps so RetroAPI can insert
 * modded entities keyed by their namespaced {@code id.toString()} (the same maps StationAPI populates).
 * This is the minimum-code path: no {@code <clinit>} timing puzzle, because {@code EntityRegistry} is
 * already class-loaded by the time any mod's {@code init()} runs.
 */
@Mixin(EntityRegistry.class)
public interface EntityRegistryAccessor {
	@Accessor("idToClass")
	static Map<String, Class<? extends Entity>> retroapi$getIdToClass() {
		throw new AssertionError();
	}

	@Accessor("classToId")
	static Map<Class<? extends Entity>, String> retroapi$getClassToId() {
		throw new AssertionError();
	}
}
