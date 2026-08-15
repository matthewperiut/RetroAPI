package com.periut.retroapi.testmod.smoke;

/**
 * The checks that need a running world, in their own class ON PURPOSE.
 *
 * <p>They hold locals of type {@code MinecraftServer} and {@code ServerWorld}, and the verifier
 * resolves the types named in a class's stack map frames when that class is LOADED - every method's,
 * not just the one being called. Left in {@link SmokeTest} they made the CLIENT refuse to load it at
 * all, exactly as the client-typed creative check did to the server. Sided code lives in a class only
 * that side ever names.
 */
final class WorldCommandSmoke {
	private WorldCommandSmoke() {
	}

	/**
	 * {@code /give} reaches everything: vanilla by name, RetroAPI's own content, and the content
	 * RetroAPI registers under the vanilla namespace - which is where command blocks live, and which
	 * the resolver used to stop at because it handed the whole {@code minecraft:} namespace to its
	 * vanilla table and gave up when the table said no.
	 */
	static void giveResolution() {
		String[] shouldResolve = {
			"minecraft:stone",
			"stone",
			"minecraft:command_block",
			"minecraft:chain_command_block",
			"minecraft:repeating_command_block",
			"retroapi_test:test_block",
			"retroapi_test:test_item",
		};
		for (String id : shouldResolve) {
			if (com.periut.retroapi.commands.argument.ItemIds.resolve(id) == null) {
				throw new IllegalStateException("/give cannot resolve " + id);
			}
		}

		// And they are offered by completion, not merely accepted when typed in full.
		java.util.List<String> suggestions = com.periut.retroapi.commands.argument.ItemIds.allIdentifiers();
		for (String id : new String[]{"minecraft:command_block", "retroapi_test:test_item"}) {
			if (!suggestions.contains(id)) {
				throw new IllegalStateException("completion does not offer " + id);
			}
		}

		if (com.periut.retroapi.commands.argument.ItemIds.resolve("minecraft:not_a_real_thing") != null) {
			throw new IllegalStateException("a nonsense id resolved to something");
		}
	}

	/** The world the dedicated server is running, or null before it has one. */
	private static net.minecraft.world.World serverWorld() {
		Object game = net.fabricmc.loader.api.FabricLoader.getInstance().getGameInstance();
		if (!(game instanceof net.minecraft.server.MinecraftServer server)) {
			return null;
		}
		return server.worlds != null && server.worlds.length > 0 ? server.worlds[0] : null;
	}

	private static com.periut.retroapi.commands.RetroCommandSource sourceAt(
			net.minecraft.world.World world, int x, int y, int z) {
		return com.periut.retroapi.commandblock.CommandBlockSources.forBlock(
			world, new com.periut.retroapi.commands.Position(x + 0.5, y, z + 0.5));
	}

	/** {@code /setblock} and {@code /fill} actually change the world they are pointed at. */
	static void commands() {
		net.minecraft.world.World world = serverWorld();
		com.periut.retroapi.commands.RetroCommandManager manager =
			com.periut.retroapi.commands.RetroCommandManager.getInstance();
		if (world == null || manager == null) {
			throw new IllegalStateException("no world or no command manager on the server");
		}

		final int baseX = 64;
		final int baseY = 70;
		final int baseZ = 64;
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 4; z++) {
					world.setBlock(baseX + x, baseY + y, baseZ + z, 0, 0);
				}
			}
		}

		manager.execute(sourceAt(world, baseX, baseY, baseZ),
			"setblock " + baseX + " " + baseY + " " + baseZ + " minecraft:gold_block");
		if (world.getBlockId(baseX, baseY, baseZ) != net.minecraft.block.Block.GOLD_BLOCK.id) {
			throw new IllegalStateException("/setblock did not place the block");
		}

		// keep must refuse an occupied space, and leave what was there alone.
		manager.execute(sourceAt(world, baseX, baseY, baseZ),
			"setblock " + baseX + " " + baseY + " " + baseZ + " minecraft:diamond_block keep");
		if (world.getBlockId(baseX, baseY, baseZ) != net.minecraft.block.Block.GOLD_BLOCK.id) {
			throw new IllegalStateException("/setblock keep overwrote an occupied block");
		}

		manager.execute(sourceAt(world, baseX, baseY, baseZ),
			"fill " + (baseX + 1) + " " + baseY + " " + baseZ + " "
				+ (baseX + 3) + " " + (baseY + 2) + " " + (baseZ + 3) + " minecraft:stone");
		if (world.getBlockId(baseX + 2, baseY + 1, baseZ + 2) != net.minecraft.block.Block.STONE.id) {
			throw new IllegalStateException("/fill did not fill the middle of the region");
		}

		// hollow clears the inside and leaves the shell.
		manager.execute(sourceAt(world, baseX, baseY, baseZ),
			"fill " + (baseX + 1) + " " + baseY + " " + baseZ + " "
				+ (baseX + 3) + " " + (baseY + 2) + " " + (baseZ + 3) + " minecraft:bricks hollow");
		if (world.getBlockId(baseX + 2, baseY + 1, baseZ + 2) != 0) {
			throw new IllegalStateException("/fill hollow did not clear the inside");
		}
		if (world.getBlockId(baseX + 1, baseY, baseZ) != net.minecraft.block.Block.BRICKS.id) {
			throw new IllegalStateException("/fill hollow did not fill the shell");
		}
	}

	/**
	 * A command block runs its command, reports success, and drives the chain block it points at -
	 * and a conditional block whose predecessor did nothing stays quiet.
	 */
	static void commandBlocks() {
		net.minecraft.world.World world = serverWorld();
		if (world == null || com.periut.retroapi.commandblock.CommandBlocks.IMPULSE == null) {
			throw new IllegalStateException("no world, or command blocks never registered");
		}

		final int x = 80;
		final int y = 70;
		final int z = 80;
		final int targetY = y + 3;

		for (int i = 0; i < 4; i++) {
			world.setBlock(x + i, y, z, 0, 0);
		}
		world.setBlock(x, targetY, z, 0, 0);
		world.setBlock(x + 1, targetY, z, 0, 0);

		// An impulse block that places a block above itself.
		world.setBlock(x, y, z, com.periut.retroapi.commandblock.CommandBlocks.IMPULSE.id, 0);
		if (!(world.getBlockEntity(x, y, z) instanceof com.periut.retroapi.commandblock.CommandBlockEntity impulse)) {
			throw new IllegalStateException("placing a command block made no block entity");
		}
		impulse.setCommand("setblock " + x + " " + targetY + " " + z + " minecraft:gold_block");

		if (!com.periut.retroapi.commandblock.CommandBlockExecutor.performCommand(world, x, y, z, impulse)) {
			throw new IllegalStateException("the command block reported failure");
		}
		if (world.getBlockId(x, targetY, z) != net.minecraft.block.Block.GOLD_BLOCK.id) {
			throw new IllegalStateException("the command block's command did not run");
		}
		if (impulse.getSuccessCount() <= 0) {
			throw new IllegalStateException("a successful command left the success count at zero");
		}

		// Modern's once-per-tick guard: the same block may not run twice in one tick.
		if (com.periut.retroapi.commandblock.CommandBlockExecutor.performCommand(world, x, y, z, impulse)) {
			throw new IllegalStateException("a command block ran twice in one tick");
		}

		// A conditional block behind nothing that succeeded must refuse.
		world.setBlock(x + 1, y, z, com.periut.retroapi.commandblock.CommandBlocks.CHAIN.id, 0);
		if (!(world.getBlockEntity(x + 1, y, z) instanceof com.periut.retroapi.commandblock.CommandBlockEntity chain)) {
			throw new IllegalStateException("placing a chain command block made no block entity");
		}
		chain.setCommand("setblock " + (x + 1) + " " + targetY + " " + z + " minecraft:diamond_block");
		com.periut.retroapi.commandblock.CommandBlocks.setConditional(world, x + 1, y, z, true);
		if (chain.markConditionMet()) {
			throw new IllegalStateException("a conditional block with nothing behind it agreed to run");
		}

		// Unconditional, and it runs.
		com.periut.retroapi.commandblock.CommandBlocks.setConditional(world, x + 1, y, z, false);
		if (!chain.markConditionMet()) {
			throw new IllegalStateException("an unconditional block refused to run");
		}
		if (!com.periut.retroapi.commandblock.CommandBlockExecutor.performCommand(world, x + 1, y, z, chain)) {
			throw new IllegalStateException("the chain block's command failed");
		}
		if (world.getBlockId(x + 1, targetY, z) != net.minecraft.block.Block.DIAMOND_BLOCK.id) {
			throw new IllegalStateException("the chain block's command did not run");
		}

		// The mode is the block, which is what the whole chain walk depends on.
		if (chain.getMode() != com.periut.retroapi.commandblock.CommandBlockMode.SEQUENCE
			|| impulse.getMode() != com.periut.retroapi.commandblock.CommandBlockMode.REDSTONE) {
			throw new IllegalStateException("a command block reported the wrong mode for its block");
		}
	}
}
