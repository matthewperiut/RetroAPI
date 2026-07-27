package com.periut.retroapi.testmod.smoke;

import com.periut.retroapi.testmod.TestMod;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The launch smoke test: boot the game, apply every RetroAPI mixin, assert the hooks are live, write a
 * verdict, exit.
 *
 * <p>Selected with {@code -Dretroapi.test.scenario=smoke} and run by the {@code clientSmokeTest} /
 * {@code serverSmokeTest} Gradle tasks. Its job is the failure mode the conversion round-trip cannot
 * catch: a mixin that no longer matches the mappings. That is not a compile error and not a data
 * problem - it is a hard crash the first time the game loads the targeted class, which for a
 * render-side mixin can be long after startup.
 *
 * <p>The verdict lands in {@code retroapi_smoke_result.txt} in the run directory; the Gradle task fails
 * the build unless it says PASS.
 */
public final class SmokeTest {
	private SmokeTest() {}

	/** Verdict file for one side, in the run directory. The Gradle gate reads exactly this name. */
	public static String resultFile(String side) {
		return "retroapi_smoke_" + side + ".txt";
	}

	// --- entry points -------------------------------------------------------------------------------

	/** Client side: sweep the mixins, check the client-only hooks, write the verdict and exit. */
	public static void runClient() {
		List<String> log = new ArrayList<>();
		boolean ok = sweep(log, "client");
		ok &= check(log, "particleRegistry", SmokeTest::particleRegistryCheck);
		ok &= check(log, "particleHookLive", SmokeTest::particleHookCheck);
		ok &= check(log, "entityRenderer", SmokeTest::entityRendererCheck);
		ok &= check(log, "staticItemTintDraws", SmokeTest::staticTintDrawCheck);
		finish("client", ok, log);
		System.exit(ok ? 0 : 1);
	}

	/** Server side: sweep the mixins and write the verdict. The caller stops the server. */
	public static boolean runServer() {
		List<String> log = new ArrayList<>();
		boolean ok = sweep(log, "server");
		finish("server", ok, log);
		return ok;
	}

	// --- checks -------------------------------------------------------------------------------------

	private static boolean sweep(List<String> log, String side) {
		MixinSweep.Report report = MixinSweep.run(MixinSweep.RETROAPI_MODS);
		log.add("== mixin sweep (" + side + ") ==");
		log.addAll(report.lines());
		return report.pass();
	}

	/** The test mod's particle must be resolvable by the name the world renderer will look up. */
	private static void particleRegistryCheck() {
		String id = TestMod.SPARK_PARTICLE.toString();
		if (!com.periut.retroapi.client.particle.RetroParticleRegistry.isRegistered(id)) {
			throw new IllegalStateException("particle '" + id + "' is not registered");
		}
		if (com.periut.retroapi.client.particle.RetroParticleRegistry.get("smoke") != null) {
			throw new IllegalStateException("un-namespaced vanilla particle name must not resolve to a mod particle");
		}
	}

	/**
	 * The registry only matters if the world renderer actually consults it. Mixin names an {@code @Inject}
	 * handler after the method in the mixin source, so finding {@code retroapi$customParticle} on
	 * {@code WorldRenderer} proves the hook made it into the class - the exact thing that was silently
	 * broken when the mixin shadowed a field name that does not exist.
	 */
	private static void particleHookCheck() {
		Class<?> wr = net.minecraft.client.render.WorldRenderer.class;
		for (java.lang.reflect.Method m : wr.getDeclaredMethods()) {
			if (m.getName().contains("retroapi$customParticle")) {
				return;
			}
		}
		throw new IllegalStateException("WorldRenderer has no retroapi$customParticle handler - the "
			+ "particle injection did not apply");
	}

	/**
	 * Declaring tinted layers is only half of it - the renderer has to actually choose them. This asks the
	 * draw path the same question it asks every frame, so a static declaration that never reaches the
	 * screen fails here instead of looking fine in the builder and rendering untinted in game.
	 */
	private static void staticTintDrawCheck() {
		java.util.List<com.periut.retroapi.component.RetroTextureLayer> layers =
			com.periut.retroapi.client.texture.LayeredItemDraw.layersOf(
				new net.minecraft.item.ItemStack(TestMod.TINT_ITEM));
		if (layers == null || layers.size() != 2) {
			throw new IllegalStateException("declared layers did not reach the draw path: " + layers);
		}
		if (layers.get(1).tint() != 0x3355FF) {
			throw new IllegalStateException("overlay lost its tint: 0x" + Integer.toHexString(layers.get(1).tint()));
		}
		if (layers.get(1).resolvedSpriteId() <= 0) {
			throw new IllegalStateException("overlay sprite did not resolve against the atlas: "
				+ layers.get(1).resolvedSpriteId());
		}
		// A plain vanilla item must still take the ordinary single-sprite path.
		if (com.periut.retroapi.client.texture.LayeredItemDraw.layersOf(
				new net.minecraft.item.ItemStack(net.minecraft.item.Item.STICK)) != null) {
			throw new IllegalStateException("a vanilla item was wrongly routed through the layered draw");
		}
	}

	/** The test mob's renderer must have reached the dispatcher. */
	private static void entityRendererCheck() {
		Object renderer = net.minecraft.client.render.entity.EntityRenderDispatcher.INSTANCE
			.get(TestMod.ZEVE.getEntityClass());
		if (renderer == null) {
			throw new IllegalStateException("no renderer registered for the test entity");
		}
	}

	// --- plumbing -----------------------------------------------------------------------------------

	@FunctionalInterface
	private interface Check {
		void run() throws Throwable;
	}

	private static boolean check(List<String> log, String name, Check check) {
		try {
			check.run();
			log.add(name + ": PASS");
			return true;
		} catch (Throwable t) {
			log.add(name + ": FAIL - " + t);
			TestMod.LOGGER.error("[smoke] {} failed", name, t);
			return false;
		}
	}

	private static void finish(String side, boolean ok, List<String> log) {
		log.add("smoke(" + side + "): " + (ok ? "PASS" : "FAIL"));
		for (String line : log) {
			TestMod.LOGGER.info("[smoke] {}", line);
		}
		try (FileWriter w = new FileWriter(new File(resultFile(side)))) {
			for (String line : log) {
				w.write(line + System.lineSeparator());
			}
		} catch (Exception e) {
			TestMod.LOGGER.error("[smoke] failed to write result file", e);
		}
	}
}
