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

	/**
	 * Writes a FAIL verdict for a run that threw before it could produce one.
	 *
	 * <p>Without this the gate finds no result file and can only say "the game never reached the
	 * check", which is true and useless; with it, the reason is in the report.
	 */
	public static void writeCrashResult(String side, Throwable cause) {
		List<String> log = new ArrayList<>();
		log.add("scenario crashed before finishing: " + cause);
		finish(side, false, log);
	}

	/** Client side: sweep the mixins, check the client-only hooks, write the verdict and exit. */
	public static void runClient() {
		try {
			runClientChecks();
		} catch (Throwable t) {
			// The client's exceptions land on the AWT thread, where nothing stops the window: without
			// this the run sits at the title screen until the build's timeout kills it.
			TestMod.LOGGER.error("[smoke] the client scenario threw", t);
			writeCrashResult("client", t);
			Runtime.getRuntime().halt(1);
		}
	}

	private static void runClientChecks() {
		List<String> log = new ArrayList<>();
		boolean ok = sweep(log, "client");
		ok &= check(log, "particleRegistry", SmokeTest::particleRegistryCheck);
		ok &= check(log, "particleHookLive", SmokeTest::particleHookCheck);
		ok &= check(log, "entityRenderer", SmokeTest::entityRendererCheck);
		ok &= check(log, "staticItemTintDraws", SmokeTest::staticTintDrawCheck);
		ok &= check(log, "creativeScreen", CreativeScreenSmoke::run);
		ok &= check(log, "spectatorHooks", CreativeScreenSmoke::spectatorHooks);
		ok &= check(log, "itemSprites", CreativeScreenSmoke::itemSprites);
		ok &= shared(log);
		ok &= check(log, "blockUseHookLive",
			() -> hookApplied(net.minecraft.client.InteractionManager.class, "retroapi$blockUse"));
		finish("client", ok, log);
		System.exit(ok ? 0 : 1);
	}

	/** Server side: sweep the mixins and write the verdict. The caller stops the server. */
	public static boolean runServer() {
		List<String> log = new ArrayList<>();
		boolean ok = sweep(log, "server");
		ok &= shared(log);
		ok &= check(log, "blockUseHookLive", () -> hookApplied(
			net.minecraft.server.network.ServerPlayerInteractionManager.class, "retroapi$blockUse"));
		ok &= check(log, "blockEntitySyncHookLive", () -> hookApplied(
			net.minecraft.entity.player.ServerPlayerEntity.class, "retroapi$syncBlockEntity"));
		ok &= check(log, "worldCommands", WorldCommandSmoke::commands);
		ok &= check(log, "giveResolution", WorldCommandSmoke::giveResolution);
		ok &= check(log, "commandBlocks", WorldCommandSmoke::commandBlocks);
		finish("server", ok, log);
		return ok;
	}

	/** The checks that mean the same thing on both sides. */
	private static boolean shared(List<String> log) {
		boolean ok = check(log, "blockAutoId", SmokeTest::blockAutoIdCheck);
		ok &= check(log, "vanillaMiningUnchanged", SmokeTest::vanillaMiningCheck);
		ok &= check(log, "blockUseEventComposes", SmokeTest::blockUseEventCheck);
		ok &= check(log, "blockEntitySyncRoundtrip", SmokeTest::blockEntitySyncCheck);
		ok &= check(log, "multiblockAnywhere", SmokeTest::multiblockAnywhereCheck);
		ok &= check(log, "customToolTier", SmokeTest::customToolTierCheck);
		ok &= check(log, "positionalToolTier", SmokeTest::positionalToolTierCheck);
		ok &= check(log, "retroData", SmokeTest::retroDataCheck);
		ok &= check(log, "gameRules", SmokeTest::gameRulesCheck);
		ok &= check(log, "gameModes", SmokeTest::gameModesCheck);
		ok &= check(log, "itemGroups", SmokeTest::itemGroupsCheck);
		return ok;
	}

	// --- checks -------------------------------------------------------------------------------------

	/** A mode is remembered per player, and decides flight and whether the world may be changed. */
	private static void gameModesCheck() {
		final String player = "SmokeModePlayer";

		if (com.periut.retroapi.gamemode.RetroGameModes.get(player)
			!= com.periut.retroapi.gamemode.RetroGameMode.SURVIVAL) {
			throw new IllegalStateException("a player nobody has touched must be in survival");
		}

		com.periut.retroapi.gamemode.RetroGameModes.set(player,
			com.periut.retroapi.gamemode.RetroGameMode.CREATIVE);
		if (com.periut.retroapi.gamemode.RetroGameModes.get(player)
			!= com.periut.retroapi.gamemode.RetroGameMode.CREATIVE) {
			throw new IllegalStateException("the mode did not stick");
		}
		if (com.periut.retroapi.gamemode.RetroFlight.isFlying(player)) {
			throw new IllegalStateException("creative must not start airborne");
		}
		if (!com.periut.retroapi.gamemode.RetroGameModes.toggleFlying(player)
			|| !com.periut.retroapi.gamemode.RetroFlight.isFlying(player)) {
			throw new IllegalStateException("creative flight would not turn on");
		}
		if (com.periut.retroapi.gamemode.RetroFlight.ignoresBlocks(player)) {
			throw new IllegalStateException("creative flight must still collide with the world");
		}

		com.periut.retroapi.gamemode.RetroGameModes.set(player,
			com.periut.retroapi.gamemode.RetroGameMode.SPECTATOR);
		if (!com.periut.retroapi.gamemode.RetroFlight.isFlying(player)
			|| !com.periut.retroapi.gamemode.RetroFlight.ignoresBlocks(player)) {
			throw new IllegalStateException("a spectator must fly and pass through blocks");
		}

		com.periut.retroapi.gamemode.RetroGameModes.set(player,
			com.periut.retroapi.gamemode.RetroGameMode.SURVIVAL);
		if (com.periut.retroapi.gamemode.RetroFlight.isFlying(player)) {
			throw new IllegalStateException("leaving a flying mode must put the player back on the ground");
		}
		if (!com.periut.retroapi.gamemode.RetroGameMode.ADVENTURE.isReadOnly()
			|| com.periut.retroapi.gamemode.RetroGameMode.CREATIVE.isReadOnly()) {
			throw new IllegalStateException("read-only is the wrong way round");
		}
	}

	/** The vanilla tabs exist, have contents, and another mod can add to one. */
	private static void itemGroupsCheck() {
		java.util.List<com.periut.retroapi.itemgroup.RetroItemGroup> groups =
			com.periut.retroapi.itemgroup.RetroItemGroups.all();
		if (groups.isEmpty()) {
			throw new IllegalStateException("no creative tabs were registered at all");
		}

		com.periut.retroapi.itemgroup.RetroItemGroup building =
			com.periut.retroapi.itemgroup.VanillaItemGroups.BUILDING_BLOCKS;
		if (building == null || building.collect().isEmpty()) {
			throw new IllegalStateException("the building blocks tab is empty");
		}
		if (building.getIcon() == null) {
			throw new IllegalStateException("a tab with no icon cannot be drawn");
		}

		// The test mod's own tab, registered through the public builder, must be in the list.
		if (com.periut.retroapi.itemgroup.RetroItemGroups.get(TestMod.TEST_GROUP.getId()) == null) {
			throw new IllegalStateException("a mod's own tab did not register");
		}
		if (TestMod.TEST_GROUP.collect().size() < 5) {
			throw new IllegalStateException("the test mod's tab is missing its entries");
		}
		boolean addedElsewhere = false;
		for (net.minecraft.item.ItemStack stack
				: com.periut.retroapi.itemgroup.VanillaItemGroups.INGREDIENTS.collect()) {
			if (TestMod.TEST_ITEM != null && stack.itemId == TestMod.TEST_ITEM.id) {
				addedElsewhere = true;
				break;
			}
		}
		if (!addedElsewhere) {
			throw new IllegalStateException("modifyEntries did not reach a vanilla tab");
		}

		int before = building.collect().size();
		com.periut.retroapi.itemgroup.RetroItemGroups.modifyEntries(building,
			entries -> entries.add(net.minecraft.block.Block.SPONGE));
		if (building.collect().size() != before + 1) {
			throw new IllegalStateException("modifyEntries did not add to the tab");
		}

		// The search tab is built from the registries, so a modded block has to appear in it.
		boolean foundModded = false;
		for (net.minecraft.item.ItemStack stack
				: com.periut.retroapi.itemgroup.VanillaItemGroups.SEARCH.collect()) {
			if (TestMod.AUTO_ID_BLOCK != null && stack.itemId == TestMod.AUTO_ID_BLOCK.id) {
				foundModded = true;
				break;
			}
		}
		if (!foundModded) {
			throw new IllegalStateException("the search tab does not list modded content");
		}
	}

	/** A rule reads back what was set, refuses nonsense, and the two new ones are off out of the box. */
	private static void gameRulesCheck() {
		if (com.periut.retroapi.gamerule.RetroGameRules.getBoolean(
				com.periut.retroapi.gamerule.RetroGameRules.SPRINTING)
			|| com.periut.retroapi.gamerule.RetroGameRules.getBoolean(
				com.periut.retroapi.gamerule.RetroGameRules.SWIMMING)) {
			throw new IllegalStateException("sprinting/swimming must be off in a world that never set them");
		}
		if (!com.periut.retroapi.gamerule.RetroGameRules.getBoolean(
				com.periut.retroapi.gamerule.RetroGameRules.DO_FIRE_TICK)) {
			throw new IllegalStateException("doFireTick must default to on, as it does in vanilla");
		}

		if (!com.periut.retroapi.gamerule.RetroGameRules.set("keepInventory", "true")) {
			throw new IllegalStateException("setting a known boolean rule was refused");
		}
		if (!com.periut.retroapi.gamerule.RetroGameRules.getBoolean(
				com.periut.retroapi.gamerule.RetroGameRules.KEEP_INVENTORY)) {
			throw new IllegalStateException("keepInventory did not read back as set");
		}
		// Normalised on the way in, so a query never reports what the player typed verbatim.
		com.periut.retroapi.gamerule.RetroGameRules.set("keepInventory", "FALSE");
		if (!"false".equals(com.periut.retroapi.gamerule.RetroGameRules.getString(
				com.periut.retroapi.gamerule.RetroGameRules.KEEP_INVENTORY))) {
			throw new IllegalStateException("boolean values are not normalised");
		}

		if (com.periut.retroapi.gamerule.RetroGameRules.set("keepInventory", "yes")
			|| com.periut.retroapi.gamerule.RetroGameRules.set("randomTickSpeed", "quickly")
			|| com.periut.retroapi.gamerule.RetroGameRules.set("noSuchRule", "true")) {
			throw new IllegalStateException("a bad rule or value was accepted");
		}

		if (!com.periut.retroapi.gamerule.RetroGameRules.snapshot()
				.containsKey("doMobSpawning")) {
			throw new IllegalStateException("the snapshot sent to clients is missing a registered rule");
		}
	}

	/**
	 * World data, permanent player data and one-life player data are three separate compounds, and a
	 * death drops only the third.
	 */
	private static void retroDataCheck() {
		final String player = "SmokeTestPlayer";

		com.periut.retroapi.storage.RetroData.world().putInt("retroapi:smoke_world", 7);
		com.periut.retroapi.storage.RetroData.player(player).putString("retroapi:smoke_kept", "yes");
		com.periut.retroapi.storage.RetroData.life(player).putInt("retroapi:smoke_life", 3);

		if (com.periut.retroapi.storage.RetroData.world().getInt("retroapi:smoke_world") != 7) {
			throw new IllegalStateException("world data did not survive being written");
		}
		if (com.periut.retroapi.storage.RetroData.life(player).getInt("retroapi:smoke_life") != 3) {
			throw new IllegalStateException("life data did not survive being written");
		}
		// Different sections, so a key written into one must not be visible in the others.
		if (com.periut.retroapi.storage.RetroData.player(player).contains("retroapi:smoke_life")
			|| com.periut.retroapi.storage.RetroData.world().contains("retroapi:smoke_kept")) {
			throw new IllegalStateException("the three data sections are not separate");
		}

		com.periut.retroapi.storage.RetroData.clearLife(player);
		if (com.periut.retroapi.storage.RetroData.life(player).contains("retroapi:smoke_life")) {
			throw new IllegalStateException("one-life data outlived the life it belongs to");
		}
		if (!"yes".equals(com.periut.retroapi.storage.RetroData.player(player).getString("retroapi:smoke_kept"))) {
			throw new IllegalStateException("permanent player data was cleared along with the life data");
		}
	}

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

	// --- 0.3.5 checks -------------------------------------------------------------------------------

	/**
	 * The block registered with the AUTO_ID sentinel (TestMod, registration time - StationAPI refuses to
	 * construct a Block outside its registration flow, so this cannot be done here) must have landed in a
	 * real, free, modded slot.
	 */
	private static void blockAutoIdCheck() {
		net.minecraft.block.Block block = TestMod.AUTO_ID_BLOCK;
		if (block == null) {
			throw new IllegalStateException("the AUTO_ID block was never registered");
		}
		if (block.id < 256 || block.id >= net.minecraft.block.Block.BLOCKS.length) {
			throw new IllegalStateException("AUTO_ID resolved to a bad slot: " + block.id);
		}
		if (net.minecraft.block.Block.BLOCKS[block.id] != block) {
			throw new IllegalStateException("AUTO_ID block is not the one stored at its own id");
		}
		// The reservation must hold: a fresh allocation cannot hand out a slot already in use.
		int next = com.periut.retroapi.register.block.RetroBlockAccess.allocateId();
		if (next == block.id || net.minecraft.block.Block.BLOCKS[next] != null) {
			throw new IllegalStateException("allocateId handed out an occupied slot: " + next);
		}
		if (com.periut.retroapi.register.block.RetroBlockAccess.allocateId() == next) {
			throw new IllegalStateException("allocateId handed out the same slot twice");
		}
		// A modded stone-material block still infers its tool, which is what the inference is for.
		if (!com.periut.retroapi.tag.RetroTags.mineableTools(block)
				.contains(com.periut.retroapi.tag.RetroTool.PICKAXE)) {
			throw new IllegalStateException("a modded stone block lost its inferred mineable/pickaxe");
		}
	}

	/**
	 * RetroAPI must not silently rewrite how vanilla plays. Leaves are the LEAVES material and modern
	 * Minecraft files them under mineable/hoe, so material inference used to hand every beta hoe a
	 * leaf-cutting speed bonus just by installing the library.
	 */
	private static void vanillaMiningCheck() {
		// Material inference must not reach a vanilla block at all. Asked directly, so the check means
		// the same thing under StationAPI, which ships its own real tags (leaves ARE mineable/shears
		// there, and that is a declaration, not an inference).
		java.util.Set<com.periut.retroapi.tag.RetroTool> inferred =
			com.periut.retroapi.tag.RetroTags.defaultMineableTools(net.minecraft.block.Block.LEAVES);
		if (!inferred.isEmpty()) {
			throw new IllegalStateException("vanilla leaves still infer tools from their material: " + inferred);
		}
		java.util.Set<com.periut.retroapi.tag.RetroTool> leaves =
			com.periut.retroapi.tag.RetroTags.mineableTools(net.minecraft.block.Block.LEAVES);
		if (leaves.contains(com.periut.retroapi.tag.RetroTool.HOE)) {
			throw new IllegalStateException("vanilla leaves are tagged mineable/hoe: " + leaves);
		}
		// The explicit beta-accurate list is untouched: stone still wants a pickaxe.
		if (!com.periut.retroapi.tag.RetroTags.mineableTools(net.minecraft.block.Block.STONE)
				.contains(com.periut.retroapi.tag.RetroTool.PICKAXE)) {
			throw new IllegalStateException("vanilla stone lost mineable/pickaxe");
		}
	}

	/** Listeners run in order and the first non-PASS wins; PASS falls through to vanilla. */
	private static void blockUseEventCheck() {
		final int[] calls = {0};
		// Only ever claims a y coordinate no real block can have, so registering these is inert in game.
		com.periut.retroapi.register.block.event.BlockUseCallback.EVENT.register(
			(player, world, held, x, y, z, face) -> {
				calls[0]++;
				return com.periut.retroapi.register.block.event.BlockUseCallback.Result.PASS;
			});
		com.periut.retroapi.register.block.event.BlockUseCallback.EVENT.register(
			(player, world, held, x, y, z, face) -> {
				calls[0]++;
				return y == SENTINEL_Y
					? com.periut.retroapi.register.block.event.BlockUseCallback.Result.SUCCESS
					: com.periut.retroapi.register.block.event.BlockUseCallback.Result.PASS;
			});
		com.periut.retroapi.register.block.event.BlockUseCallback.EVENT.register(
			(player, world, held, x, y, z, face) -> {
				calls[0]++;
				return com.periut.retroapi.register.block.event.BlockUseCallback.Result.PASS;
			});

		var result = com.periut.retroapi.register.block.event.BlockUseCallback.EVENT.invoker()
			.onUseBlock(null, null, null, 0, SENTINEL_Y, 0, 1);
		if (result != com.periut.retroapi.register.block.event.BlockUseCallback.Result.SUCCESS) {
			throw new IllegalStateException("a SUCCESS listener did not claim the click: " + result);
		}
		if (calls[0] != 2) {
			throw new IllegalStateException("listeners after the claim still ran: " + calls[0] + " calls");
		}
		var pass = com.periut.retroapi.register.block.event.BlockUseCallback.EVENT.invoker()
			.onUseBlock(null, null, null, 0, 64, 0, 1);
		if (pass != com.periut.retroapi.register.block.event.BlockUseCallback.Result.PASS) {
			throw new IllegalStateException("an unclaimed click did not fall through: " + pass);
		}
	}

	/** The block-entity sync wire format has to survive a round trip, or the client shows stale data. */
	private static void blockEntitySyncCheck() {
		// The wire form is NBT, and vanilla's writeNbt refuses to write an unregistered class at all, so
		// sync is only ever available to a block entity that went through RetroBlockEntities.register.
		com.periut.retroapi.register.blockentity.RetroBlockEntities.register(
			"retroapi_test:smoke_sync", SyncedTestBlockEntity.class);
		SyncedTestBlockEntity source = new SyncedTestBlockEntity();
		source.x = 12;
		source.y = 70;
		source.z = -34;
		source.counter = 4711;
		byte[] data = com.periut.retroapi.register.blockentity.BlockEntitySyncCodec.encode(source);
		if (data == null || data.length == 0) {
			throw new IllegalStateException("a synced block entity encoded to nothing");
		}
		SyncedTestBlockEntity target = new SyncedTestBlockEntity();
		com.periut.retroapi.register.blockentity.BlockEntitySyncCodec.decode(target, data);
		if (target.counter != 4711 || target.x != 12 || target.y != 70 || target.z != -34) {
			throw new IllegalStateException("sync round trip lost data: counter=" + target.counter
				+ " at " + target.x + "," + target.y + "," + target.z);
		}
		if (!target.synced) {
			throw new IllegalStateException("onSynced was never called on the receiving side");
		}
		// A block entity that does not opt in must produce nothing at all.
		if (com.periut.retroapi.register.blockentity.BlockEntitySyncCodec.encode(
				new net.minecraft.block.entity.SignBlockEntity()) != null) {
			throw new IllegalStateException("a non-synced block entity was put on the wire anyway");
		}
	}

	/** matchAnywhere has to find the structure from a cell that is NOT the anchor, in every rotation. */
	private static void multiblockAnywhereCheck() {
		com.periut.retroapi.world.multiblock.RetroMultiblock pattern =
			com.periut.retroapi.world.multiblock.RetroMultiblock.builder()
				.layer("BBB",
					   "BCB",
					   "BBB")
				.where('B', net.minecraft.block.Block.BRICKS)
				.where('C', net.minecraft.block.Block.GOLD_BLOCK)
				.anchor('C')
				.build();

		// Build the 3x3 around (100, 64, 100) with the gold core in the middle.
		StubBlockView world = new StubBlockView();
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				world.set(100 + dx, 64, 100 + dz, net.minecraft.block.Block.BRICKS.id);
			}
		}
		world.set(100, 64, 100, net.minecraft.block.Block.GOLD_BLOCK.id);

		if (pattern.matchAnyRotation(world, 100, 64, 100) == null) {
			throw new IllegalStateException("matchAnyRotation missed the structure at its own anchor");
		}
		// Every brick in the ring must resolve back to the same anchor.
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				var match = pattern.matchAnywhere(world, 100 + dx, 64, 100 + dz);
				if (match == null) {
					throw new IllegalStateException("matchAnywhere missed the structure from offset "
						+ dx + "," + dz);
				}
				var anchor = match.anchor();
				if (anchor.x != 100 || anchor.y != 64 || anchor.z != 100) {
					throw new IllegalStateException("matchAnywhere found the wrong anchor: " + anchor);
				}
			}
		}
		if (pattern.matchAnywhere(world, 200, 64, 200) != null) {
			throw new IllegalStateException("matchAnywhere invented a structure out of empty air");
		}

		// Freeform: the whole brick ring is one connected region, and the limit is honored.
		var region = com.periut.retroapi.world.multiblock.RetroBlockRegion.flood(
			world, 100, 64, 101, 64, net.minecraft.block.Block.BRICKS, net.minecraft.block.Block.GOLD_BLOCK);
		if (!region.complete() || region.size() != 9) {
			throw new IllegalStateException("flood found " + region.size()
				+ " blocks (complete=" + region.complete() + "), expected 9");
		}
		var capped = com.periut.retroapi.world.multiblock.RetroBlockRegion.flood(
			world, 100, 64, 101, 4, net.minecraft.block.Block.BRICKS, net.minecraft.block.Block.GOLD_BLOCK);
		if (capped.complete() || capped.size() != 4) {
			throw new IllegalStateException("flood ignored its limit: " + capped.size()
				+ " complete=" + capped.complete());
		}
	}

	/** A mod-registered tool tier must slot in between the built-ins and behave like one. */
	private static void customToolTierCheck() {
		com.periut.retroapi.tag.RetroToolTier bronze =
			com.periut.retroapi.tag.RetroToolTier.register("bronze", 1, 5.0F);
		if (com.periut.retroapi.tag.RetroToolTier.get("bronze") != bronze) {
			throw new IllegalStateException("a registered tier did not come back out of the registry");
		}
		if (com.periut.retroapi.tag.RetroToolTier.register("bronze", 9, 99.0F) != bronze) {
			throw new IllegalStateException("registering the same name twice made a second tier");
		}
		if (!bronze.isAtLeast(com.periut.retroapi.tag.RetroToolTier.STONE)
			|| bronze.isAtLeast(com.periut.retroapi.tag.RetroToolTier.IRON)) {
			throw new IllegalStateException("custom tier sits in the wrong place: level " + bronze.getLevel());
		}
		if (bronze.getMiningSpeed() != 5.0F) {
			throw new IllegalStateException("custom tier lost its mining speed: " + bronze.getMiningSpeed());
		}
		if (!"needs_bronze_tool".equals(bronze.getTagName())) {
			throw new IllegalStateException("custom tier got the wrong tag name: " + bronze.getTagName());
		}
		boolean listed = false;
		for (com.periut.retroapi.tag.RetroToolTier tier : com.periut.retroapi.tag.RetroToolTier.values()) {
			if (tier == bronze) listed = true;
		}
		if (!listed) {
			throw new IllegalStateException("custom tier is missing from values(), so tags will skip it");
		}
		// The built-ins must be untouched: they are ordinary registry entries now, and if registration
		// order or level lookup shifted, every vanilla harvest rule shifts with it.
		if (com.periut.retroapi.tag.RetroToolTier.fromLevel(2) != com.periut.retroapi.tag.RetroToolTier.IRON
			|| com.periut.retroapi.tag.RetroToolTier.fromLevel(-1) != com.periut.retroapi.tag.RetroToolTier.NONE
			|| com.periut.retroapi.tag.RetroToolTier.DIAMOND.getMiningSpeed() != 8.0F) {
			throw new IllegalStateException("the built-in tiers changed meaning");
		}
	}

	/** A positional tier must be consulted, and must get "no context" rather than a stale position. */
	private static void positionalToolTierCheck() {
		net.minecraft.item.Item item = TestMod.STAT_TOOL;
		com.periut.retroapi.register.item.RetroItemAccess access =
			(com.periut.retroapi.register.item.RetroItemAccess) item;
		com.periut.retroapi.tag.RetroToolTier.Positional previous = access.getToolTierPositional();
		final Object[] seen = new Object[1];
		try {
			access.tier((com.periut.retroapi.tag.RetroToolTier.Positional)
				(stack, block, player, world, x, y, z) -> {
					seen[0] = world;
					return com.periut.retroapi.tag.RetroToolTier.DIAMOND;
				});
			com.periut.retroapi.tag.RetroToolTier tier = com.periut.retroapi.tag.RetroToolTier.of(
				new net.minecraft.item.ItemStack(item), net.minecraft.block.Block.STONE, null);
			if (tier != com.periut.retroapi.tag.RetroToolTier.DIAMOND) {
				throw new IllegalStateException("the positional tier was never consulted: " + tier);
			}
			if (seen[0] != null) {
				throw new IllegalStateException("no break is in progress, so the world must be null");
			}
		} finally {
			access.tier(previous);
		}
		if (com.periut.retroapi.tag.RetroBreakTarget.current(null) != null) {
			throw new IllegalStateException("a null player produced a break target");
		}
	}

	/** A y coordinate no world can hold, so the smoke test's own listeners never fire in game. */
	private static final int SENTINEL_Y = Integer.MIN_VALUE;

	/** Mixin names an {@code @Inject} handler after the method in the mixin source. */
	private static void hookApplied(Class<?> target, String handler) {
		for (java.lang.reflect.Method m : target.getDeclaredMethods()) {
			if (m.getName().contains(handler)) {
				return;
			}
		}
		throw new IllegalStateException(target.getSimpleName() + " has no " + handler
			+ " handler - the injection did not apply");
	}

	/** A block entity that opts into RetroAPI sync and carries one value across the wire. */
	private static final class SyncedTestBlockEntity extends net.minecraft.block.entity.BlockEntity
			implements com.periut.retroapi.register.blockentity.RetroSyncedBlockEntity {
		int counter;
		boolean synced;

		@Override
		public void writeNbt(net.minecraft.nbt.NbtCompound nbt) {
			super.writeNbt(nbt);
			nbt.putInt("counter", counter);
		}

		@Override
		public void readNbt(net.minecraft.nbt.NbtCompound nbt) {
			super.readNbt(nbt);
			counter = nbt.getInt("counter");
		}

		@Override
		public void onSynced() {
			synced = true;
		}
	}

	/** A block-id map standing in for a world, so the multiblock checks need no loaded chunks. */
	private static final class StubBlockView implements net.minecraft.world.BlockView {
		private final java.util.Map<Long, Integer> ids = new java.util.HashMap<>();

		void set(int x, int y, int z, int id) {
			ids.put(key(x, y, z), id);
		}

		private static long key(int x, int y, int z) {
			return ((long) x & 0x3FFFFFF) << 38 | ((long) y & 0xFFF) << 26 | ((long) z & 0x3FFFFFF);
		}

		@Override public int getBlockId(int x, int y, int z) { return ids.getOrDefault(key(x, y, z), 0); }
		@Override public net.minecraft.block.entity.BlockEntity getBlockEntity(int x, int y, int z) { return null; }
		@Override public float getNaturalBrightness(int x, int y, int z, int min) { return 1.0F; }
		@Override public float getLuminance(int x, int y, int z) { return 1.0F; }
		@Override public int getBlockMeta(int x, int y, int z) { return 0; }
		@Override public boolean isOpaque(int x, int y, int z) { return false; }
		@Override public boolean shouldSuffocate(int x, int y, int z) { return false; }
		@Override public net.minecraft.world.biome.source.BiomeSource getBiomeSource() { return null; }

		@Override
		public net.minecraft.block.material.Material getMaterial(int x, int y, int z) {
			int id = getBlockId(x, y, z);
			return id == 0 ? net.minecraft.block.material.Material.AIR : net.minecraft.block.Block.BLOCKS[id].material;
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
