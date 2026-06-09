package com.periut.retroapi.testmod;

import com.periut.retroapi.entity.client.RetroEntityRenderers;
import com.periut.retroapi.testmod.conv.Scenario;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.ornithemc.osl.entrypoints.api.client.ClientModInitializer;

/**
 * Client init for the test mod's entity renderers. Registered flat (Fabric-API style) before the
 * {@code EntityRenderDispatcher} is first constructed; the dispatcher mixin flushes these in.
 * The zeve renders exactly like vanilla's generic biped fallback
 * ({@code LivingEntity.class -> new LivingEntityRenderer(new BipedEntityModel(), 0.5F)}) and pulls
 * its texture from the entity's {@code texture} field ({@code /mob/zombie.png}).
 */
public class TestModClient implements ClientModInitializer {
	@Override
	public void initClient() {
		RetroEntityRenderers.register(ZeveEntity.class, new LivingEntityRenderer(new BipedEntityModel(), 0.5F));

		// Headless conversion round-trip (StationAPI client only): once the client is ready, at the title
		// screen, forward/reverse-convert the populated world and assert content retention, then exit. The
		// runner is StationAPI-dependent and lives only in the :test:stapi source set, so it is invoked
		// reflectively - this shared source carries no StationAPI reference. The ROUNDTRIP scenario only ever
		// runs under :test:stapi, where the class is present.
		if (Scenario.is(Scenario.ROUNDTRIP)) {
			try {
				Class.forName("com.periut.retroapi.testmod.conv.ConversionRoundtrip")
					.getMethod("register").invoke(null);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("ROUNDTRIP scenario requires the :test:stapi build", e);
			}
		}
	}
}
