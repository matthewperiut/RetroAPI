package com.periut.retroapi.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sidecar for modded items in PLAYER inventories ({@code retroapi/player_items.dat}) - the same
 * architecture as chest inventories: modded stacks are stripped OUT of the vanilla player NBT on
 * save and injected back on load (same slot if free, else next free slot). Vanilla never sees the
 * entries at all, so a vanilla open/save round-trip cannot cull or strip them (vanilla deletes
 * count-0 ghost stacks and drops unknown NBT keys from item compounds - which is exactly how the
 * old in-place {@code retroapi:id} stamps died).
 *
 * <p>Per player: a {@code players} {@link NbtList} of {@code {name, items}} where {@code items} is
 * a list of {@code {Slot, id (string identifier), Count, Damage}}. Items whose mod is currently
 * missing stay recorded (via the {@code leftover} RAM map, merged back in at the next save) and
 * restore when the mod returns. Root is RAM-cached; persisted through {@link SidecarIo}.
 */
public class PlayerItemSidecar {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/PlayerItemSidecar");

	private static NbtCompound cachedRoot;
	private static File cachedFile;

	/**
	 * Items that could not be restored at load time (mod removed, or no free slot). PEEKED - never
	 * removed - by {@link #recordItems} so they survive every subsequent save until restorable.
	 */
	private static final Map<String, NbtList> leftover = new HashMap<>();

	/** Drop all RAM state (world switch - called from {@code SidecarManager.flush}). */
	public static void reset() {
		cachedRoot = null;
		cachedFile = null;
		leftover.clear();
	}

	private static File file() {
		File worldDir = SidecarManager.getWorldDir();
		return worldDir == null ? null : new File(worldDir, "retroapi/player_items.dat");
	}

	/** Whether the sidecar can be used right now (a RetroAPI world is open). */
	public static boolean isAvailable() {
		return file() != null;
	}

	private static NbtCompound root(File f) {
		if (cachedRoot != null && f.equals(cachedFile)) {
			return cachedRoot;
		}
		NbtCompound root = new NbtCompound();
		if (f.exists()) {
			try (FileInputStream fis = new FileInputStream(f)) {
				root = NbtIo.readCompressed(fis);
			} catch (IOException e) {
				LOGGER.error("Failed to read {}", f, e);
			}
		}
		cachedRoot = root;
		cachedFile = f;
		return root;
	}

	/**
	 * Record (replacing any prior record) the modded items stripped from a player's inventory NBT.
	 * Unrestorable leftovers noted at load time are merged in so they are never dropped.
	 */
	public static void recordItems(String name, NbtList items) {
		File f = file();
		if (f == null) return;
		NbtList pending = leftover.get(name);
		if (pending != null) {
			for (int i = 0; i < pending.size(); i++) {
				items.add(pending.get(i));
			}
		}
		NbtCompound root = root(f);
		NbtList players = without(root, name);
		if (items.size() > 0) {
			NbtCompound entry = new NbtCompound();
			entry.putString("name", name);
			entry.put("items", items);
			players.add(entry);
		}
		root.put("players", players);
		persist(f, root);
	}

	/** The recorded modded items for a player, or {@code null} if none. */
	public static NbtList getItems(String name) {
		File f = file();
		if (f == null) return null;
		NbtCompound root = root(f);
		if (!root.contains("players")) return null;
		NbtList players = root.getList("players");
		for (int i = 0; i < players.size(); i++) {
			NbtCompound entry = (NbtCompound) players.get(i);
			if (name.equals(entry.getString("name"))) {
				return entry.getList("items");
			}
		}
		return null;
	}

	/** Note items that could not be restored this load; kept until a future load can place them. */
	public static void setLeftover(String name, NbtList items) {
		if (items == null || items.size() == 0) {
			leftover.remove(name);
		} else {
			leftover.put(name, items);
		}
	}

	private static NbtList without(NbtCompound root, String excludeName) {
		NbtList result = new NbtList();
		if (root.contains("players")) {
			NbtList players = root.getList("players");
			for (int i = 0; i < players.size(); i++) {
				NbtCompound entry = (NbtCompound) players.get(i);
				if (!excludeName.equals(entry.getString("name"))) {
					result.add(entry);
				}
			}
		}
		return result;
	}

	private static void persist(File f, NbtCompound root) {
		try {
			SidecarIo.writeAsync(f, SidecarIo.snapshot(root));
		} catch (IOException e) {
			LOGGER.error("Failed to snapshot {}", f, e);
		}
	}
}
