package com.periut.retroapi.mixin.entity.client;

import com.periut.retroapi.entity.client.RetroEntityRenderers;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.Map;

/**
 * Wires modded entity renderers into the dispatcher's renderer map. Redirects the single
 * {@code this.renderers.values()} call in {@code <init>} (one call site, so no brittle ordinal like
 * StationAPI's): flush RetroAPI-registered renderers into the map, then return {@code values()} - so
 * vanilla's following {@code setDispatcher(this)} loop iterates and wires vanilla + modded renderers
 * alike. Disabled when StationAPI is present (its EntityRendererRegisterEvent owns this).
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
	@Redirect(
		method = "<init>",
		at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;")
	)
	private Collection<EntityRenderer<? extends Entity>> retroapi$registerModdedRenderers(
			Map<Class<? extends Entity>, EntityRenderer<? extends Entity>> renderers) {
		RetroEntityRenderers.applyTo(renderers);
		return renderers.values();
	}
}
