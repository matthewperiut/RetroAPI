package com.periut.retroapi.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * The file behind {@link RetroData}: {@code retroapi/data.dat} in the world save.
 *
 * <p>One compound tree, read once when a world opens and written through {@link SidecarIo} (atomic +
 * async) whenever the world saves. Its shape is:
 *
 * <pre>
 * root
 *   world     -&gt; the world compound
 *   players
 *     &lt;name&gt;
 *       persistent -&gt; kept across death
 *       life       -&gt; dropped on respawn
 * </pre>
 *
 * <p>Sections are looked up by known key rather than iterated: b1.7.3's {@code NbtCompound} has no
 * key-iteration or removal API, and every access here already knows the name it wants.
 *
 * <p>Without a world folder - a client on a remote server - the tree is a scratch one that is never
 * written, so callers never have to null-check.
 */
final class RetroDataSidecar {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/Data");

	private static final String FILE_NAME = "data.dat";

	private static NbtCompound root;
	private static File loadedFrom;

	private RetroDataSidecar() {
	}

	/** Drop the RAM copy (world switch - called from {@code SidecarManager.flush}). */
	static void reset() {
		root = null;
		loadedFrom = null;
	}

	static NbtCompound section(String name) {
		return getOrCreate(root(), name);
	}

	static NbtCompound playerSection(String playerName, String section) {
		NbtCompound players = getOrCreate(root(), "players");
		NbtCompound player = getOrCreate(players, playerName);
		return getOrCreate(player, section);
	}

	static void clearPlayerSection(String playerName, String section) {
		NbtCompound players = getOrCreate(root(), "players");
		NbtCompound player = getOrCreate(players, playerName);
		player.put(section, new NbtCompound());
	}

	/**
	 * b1.7.3's {@code getCompound} hands back a fresh compound for a missing key WITHOUT storing it,
	 * so writes into the result would vanish. Store it.
	 */
	private static NbtCompound getOrCreate(NbtCompound parent, String key) {
		NbtCompound existing = parent.getCompound(key);
		if (!parent.contains(key)) {
			parent.put(key, existing);
		}
		return existing;
	}

	private static NbtCompound root() {
		File worldDir = SidecarManager.getWorldDir();

		// A world switch replaces the folder under us; reload rather than keep serving the old world's
		// data. (A null folder - a client on a server - keeps whatever scratch tree is already here.)
		if (root != null && worldDir != null && !worldDir.equals(loadedFrom)) {
			root = null;
		}

		if (root == null) {
			root = load(worldDir);
			loadedFrom = worldDir;
		}
		return root;
	}

	private static NbtCompound load(File worldDir) {
		if (worldDir == null) {
			return new NbtCompound();
		}

		File file = new File(worldDir, "retroapi/" + FILE_NAME);
		if (!file.exists()) {
			return new NbtCompound();
		}

		try (FileInputStream in = new FileInputStream(file)) {
			return NbtIo.readCompressed(in);
		} catch (IOException e) {
			// A torn file must not take the world down with it: an empty tree loses the extras, a
			// crash loses the save. The file is rewritten on the next save either way.
			LOGGER.error("Failed to read {}; starting from empty world/player data", file, e);
			return new NbtCompound();
		}
	}

	/** Called from {@code SidecarManager.saveAll}. */
	static void save() {
		File worldDir = SidecarManager.getWorldDir();
		if (root == null || worldDir == null) {
			return;
		}

		File file = new File(worldDir, "retroapi/" + FILE_NAME);
		try {
			SidecarIo.writeAsync(file, SidecarIo.snapshot(root));
		} catch (IOException e) {
			LOGGER.error("Failed to snapshot {}", file, e);
		}
	}
}
