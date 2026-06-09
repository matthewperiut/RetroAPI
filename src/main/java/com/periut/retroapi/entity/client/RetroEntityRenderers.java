package com.periut.retroapi.entity.client;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Client-side facade + registry for modded entity renderers. Mods register flat in their client
 * init (Fabric-API style - no lifecycle ordering constraint, unlike entity registration):
 *
 * <pre>{@code
 * RetroEntityRenderers.register(EntityMoa.class, new MoaRenderer());
 * }</pre>
 *
 * <p>The {@code EntityRenderDispatcher} mixin flushes these into the dispatcher's renderer map at
 * construction (before vanilla's {@code setDispatcher} loop), so modded renderers are wired exactly
 * like vanilla ones. Renderers must be registered before the dispatcher is first constructed
 * (i.e. in client init); late registrations are not picked up. Disabled under StationAPI (its
 * {@code EntityRendererRegisterEvent} owns this path - see the StationAPI entity forwarder).
 */
public final class RetroEntityRenderers {
	private RetroEntityRenderers() {}

	private static final Map<Class<? extends Entity>, EntityRenderer<? extends Entity>> RENDERERS = new LinkedHashMap<>();

	public static void register(Class<? extends Entity> entityClass, EntityRenderer<? extends Entity> renderer) {
		RENDERERS.put(entityClass, renderer);
	}

	/** Copy all registered modded renderers into the dispatcher's map. Called from the mixin (no StationAPI). */
	public static void applyTo(Map<Class<? extends Entity>, EntityRenderer<? extends Entity>> dispatcherMap) {
		dispatcherMap.putAll(RENDERERS);
	}

	/** Forward each (class, renderer) pair - used by the StationAPI renderer-event bridge under StationAPI. */
	public static void forEach(BiConsumer<Class<? extends Entity>, EntityRenderer<? extends Entity>> consumer) {
		RENDERERS.forEach(consumer);
	}
}
