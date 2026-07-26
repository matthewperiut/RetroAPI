package com.periut.retroapi.testmod;

import com.periut.retroapi.client.particle.RetroParticleRegistry;
import com.periut.retroapi.client.particle.RetroSpriteParticle;
import com.periut.retroapi.entity.client.RetroEntityRenderers;
import com.periut.retroapi.testmod.conv.Scenario;
import com.periut.retroapi.testmod.smoke.SmokeTest;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import com.periut.retroapi.entrypoint.RetroClientModInitializer;

/**
 * Client init for the test mod's entity renderers and particles. Registered flat (Fabric-API style)
 * before the {@code EntityRenderDispatcher} is first constructed; the dispatcher mixin flushes these in.
 * The zeve renders exactly like vanilla's generic biped fallback
 * ({@code LivingEntity.class -> new LivingEntityRenderer(new BipedEntityModel(), 0.5F)}) and pulls
 * its texture from the entity's {@code texture} field ({@code /mob/zombie.png}).
 */
public class TestModClient implements RetroClientModInitializer {
	@Override
	public void initRetroClient() {
		RetroEntityRenderers.register(ZeveEntity.class, new LivingEntityRenderer(new BipedEntityModel(), 0.5F));

		// A particle, so the client smoke test can prove the world-renderer hook resolves a namespaced
		// name to a mod factory (and that vanilla names still fall through untouched).
		RetroParticleRegistry.register(TestMod.SPARK_PARTICLE, (world, x, y, z, vx, vy, vz) ->
			new RetroSpriteParticle(world, x, y, z, vx, vy, vz, TestMod.SPARK_TEXTURE)
				.lifetime(20).scale(0.8F).gravity(0.02F).tint(0xFFD070));

		// Launch smoke test: once the client is up at the title screen, force every RetroAPI mixin to
		// apply and assert the client hooks are live, then exit. Catches mapping drift that the
		// conversion round-trip (server-side data) never touches.
		if (Scenario.is(Scenario.SMOKE)) {
			MinecraftClientEvents.READY.register(mc -> SmokeTest.runClient());
		}

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
