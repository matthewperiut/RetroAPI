package com.periut.retroapi.testmod.conv;

import com.periut.retroapi.testmod.TestMod;
import net.minecraft.client.Minecraft;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * The client side of the conversion round-trip, run once at the title screen (no world entered, no GUI
 * driven): forward-convert the populated McRegion world to StationAPI's flattened format and assert the
 * modded content survived, then reverse-convert it back to McRegion + sidecar and assert again. Writes a
 * full per-category audit trail to the world's result file and exits, so the Gradle gate can decide
 * pass/fail.
 *
 * <p>Only ever runs on the StationAPI client (the {@code roundtrip} scenario), so the StationAPI-only
 * {@link StationApiConvert} reference is safe.
 */
public final class ConversionRoundtrip {
	private ConversionRoundtrip() {}

	/**
	 * Registers the round-trip to run when the client is ready. Invoked reflectively from {@code TestModClient}
	 * (shared source) so the shared test mod carries no StationAPI reference - this class is compiled only into
	 * the {@code :test:stapi} variant.
	 */
	public static void register() {
		MinecraftClientEvents.READY.register(ConversionRoundtrip::run);
	}

	public static void run(Minecraft mc) {
		String folder = System.getProperty("retroapi.test.worldname", "convtest");
		// Dev client saves live under run/saves/<folder>; the run task's workdir is the project run dir.
		File worldDir = new File("saves", folder);
		List<String> log = new ArrayList<>();
		boolean overall = true;

		// Runtime component self-check: proves the data-component holder + deep-copy + NBT round-trip are
		// ACTIVE under StationAPI (the whole point of splitting them out of the disabled ItemStackMixin into
		// component.ItemStackComponentMixin). If the holder weren't applied, the cast inside RetroComponents
		// would throw here under StationAPI.
		boolean compRuntime = runtimeComponentCheck();
		log.add("componentRuntime(StationAPI): " + (compRuntime ? "PASS" : "FAIL"));
		overall &= compRuntime;

		// Model-block render path: under StationAPI a RetroAPI block texture slot is a StationAPI sprite
		// index, so RetroAtlas must return that sprite's real UV (via StationAtlasUv), NOT the old 16/256
		// grid math (which addressed a 256px atlas that doesn't exist under StationAPI). Assert RetroAtlas's
		// output equals the StationAPI sprite's UV - this fails if the StationAPI branch/split regressed.
		boolean modelUv = modelRenderUvCheck();
		log.add("modelRenderUV(StationAPI): " + (modelUv ? "PASS" : "FAIL"));
		overall &= modelUv;

		try {
			ConvManifest m = ConvManifest.load(worldDir);
			log.add("== conversion round-trip for '" + folder + "' ==");
			log.add("manifest: " + m.blocks.size() + " blocks, " + m.chestItems.size() + " chest items, "
				+ m.itemEntities.size() + " item entities, " + m.entities.size() + " entities, dim="
				+ (m.dimBlock != null));

			boolean needs = StationApiConvert.needsConversion(mc, folder);
			log.add("needsConversion(pre-forward) = " + needs);

			// ---- forward: McRegion + sidecar -> flattened ----
			try {
				StationApiConvert.forward(mc, folder);
				DiskVerifier.Report fwd = DiskVerifier.verifyFlattened(worldDir, m);
				log.add("-- forward (flattened) " + (fwd.pass() ? "PASS" : "FAIL") + " --");
				log.addAll(fwd.lines);
				overall &= fwd.pass();
			} catch (Throwable t) {
				overall = false;
				log.add("-- forward THREW: " + t);
				TestMod.LOGGER.error("[conv-roundtrip] forward conversion threw", t);
			}

			// ---- reverse: flattened -> McRegion + sidecar ----
			try {
				StationApiConvert.reverse(mc, folder);
				DiskVerifier.Report rev = DiskVerifier.verifyMcRegion(worldDir, m);
				log.add("-- reverse (mcregion) " + (rev.pass() ? "PASS" : "FAIL") + " --");
				log.addAll(rev.lines);
				overall &= rev.pass();
			} catch (Throwable t) {
				overall = false;
				log.add("-- reverse THREW: " + t);
				TestMod.LOGGER.error("[conv-roundtrip] reverse conversion threw", t);
			}
		} catch (Throwable t) {
			overall = false;
			log.add("roundtrip setup THREW: " + t);
			TestMod.LOGGER.error("[conv-roundtrip] setup threw", t);
		}

		log.add("roundtrip: " + (overall ? "PASS" : "FAIL"));

		for (String line : log) TestMod.LOGGER.info("[conv-roundtrip] {}", line);
		try (FileWriter w = new FileWriter(new File(worldDir, Scenario.RESULT_FILE), true)) {
			for (String line : log) w.write(line + System.lineSeparator());
		} catch (Exception e) {
			TestMod.LOGGER.error("[conv-roundtrip] failed to write result file", e);
		}

		// Always exit cleanly; the Gradle gate task interprets the result file.
		System.exit(0);
	}

	/** Set a component, deep-copy, and NBT round-trip a stack - all through the live (StationAPI) holder. */
	private static boolean runtimeComponentCheck() {
		try {
			net.minecraft.item.ItemStack s = new net.minecraft.item.ItemStack(net.minecraft.item.Item.STICK);
			com.periut.retroapi.component.RetroComponents.set(s, TestMod.TEST_COUNT, 42);
			net.minecraft.item.ItemStack copy = s.copy();
			net.minecraft.nbt.NbtCompound nbt = new net.minecraft.nbt.NbtCompound();
			s.writeNbt(nbt);
			net.minecraft.item.ItemStack restored = new net.minecraft.item.ItemStack(0, 0, 0);
			restored.readNbt(nbt);
			Integer c = com.periut.retroapi.component.RetroComponents.get(copy, TestMod.TEST_COUNT);
			Integer r = com.periut.retroapi.component.RetroComponents.get(restored, TestMod.TEST_COUNT);
			return c != null && c == 42 && r != null && r == 42;
		} catch (Throwable t) {
			TestMod.LOGGER.error("[conv-roundtrip] runtime component check threw (holder not active under StationAPI?)", t);
			return false;
		}
	}

	/** RetroAtlas must map a model-block slot to its StationAPI sprite UV (not the 16/256 grid). */
	private static boolean modelRenderUvCheck() {
		try {
			int slot = TestMod.TEST_BLOCK.getTexture(0, 0);
			double got = com.periut.retroapi.client.model.RetroAtlas.terrainU(slot, 0);
			double expected = net.modificationstation.stationapi.api.client.texture.atlas.Atlases
				.getTerrain().getTexture(slot).getStartU();
			boolean ok = expected >= 0 && expected <= 1 && Math.abs(got - expected) < 1e-9;
			if (!ok) {
				TestMod.LOGGER.error("[conv-roundtrip] model UV: RetroAtlas={} but StationAPI sprite startU={} (slot {})",
					got, expected, slot);
			}
			return ok;
		} catch (Throwable t) {
			TestMod.LOGGER.error("[conv-roundtrip] model render UV check threw", t);
			return false;
		}
	}
}
