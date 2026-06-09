package com.periut.retroapi.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Safe-reset persistence for a player's current dimension.
 *
 * <p>b1.7.3 saves {@code player.dimensionId} (and position) as raw values in player NBT; a modded
 * {@code Dimension} (&ge; 2) would crash vanilla's {@code Dimension.fromId} on load, and the modded
 * coordinates would drop the player into the void of the overworld. So on save we clamp the vanilla
 * NBT to a safe overworld state - {@code Dimension = 0} and position = the world spawn - and record the
 * real modded dimension + the EXACT position/rotation doubles vanilla wrote, here in
 * {@code retroapi/dimensions.dat}. On a RetroAPI load, {@code PlayerDimensionMixin} injects these values
 * back into the NBT compound at {@code readNbt} HEAD, so vanilla's own loading path parses them as if
 * they had always been in the file - no post-load repositioning.
 *
 * <p>Entries are stored as a {@code players} {@link NbtList} of {@code {name, dim, x, y, z, yaw, pitch}}
 * compounds - avoids b1.7.3's lack of an NBT-compound key-iteration/removal API. The root compound is
 * cached in RAM (one disk read per world) and persisted through {@link SidecarIo} (atomic + async).
 */
public class PlayerDimensionSidecar {
	private static final Logger LOGGER = LogManager.getLogger("RetroAPI/DimensionSidecar");

	private static NbtCompound cachedRoot;
	private static File cachedFile;

	/** A player's recorded modded dimension and real position/rotation. */
	public static final class Entry {
		public final int dimensionId;
		public final double x;
		public final double y;
		public final double z;
		public final boolean hasRotation;
		public final float yaw;
		public final float pitch;

		public Entry(int dimensionId, double x, double y, double z, boolean hasRotation, float yaw, float pitch) {
			this.dimensionId = dimensionId;
			this.x = x;
			this.y = y;
			this.z = z;
			this.hasRotation = hasRotation;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	/** Drop the RAM cache (world switch - called from {@code SidecarManager.flush}). */
	public static void reset() {
		cachedRoot = null;
		cachedFile = null;
	}

	private static File file() {
		File worldDir = SidecarManager.getWorldDir();
		return worldDir == null ? null : new File(worldDir, "retroapi/dimensions.dat");
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

	/** Record (replacing any prior entry) the real modded dimension + exact saved position/rotation. */
	public static void recordPlayer(String name, int dimensionId, double x, double y, double z, float yaw, float pitch) {
		File f = file();
		if (f == null) return;
		NbtCompound root = root(f);
		NbtList players = without(root, name);
		NbtCompound entry = new NbtCompound();
		entry.putString("name", name);
		entry.putInt("dim", dimensionId);
		entry.putDouble("x", x);
		entry.putDouble("y", y);
		entry.putDouble("z", z);
		entry.putFloat("yaw", yaw);
		entry.putFloat("pitch", pitch);
		players.add(entry);
		root.put("players", players);
		persist(f, root);
	}

	/** Drop any recorded modded dimension for a player (they are back in a vanilla dimension). */
	public static void removePlayer(String name) {
		File f = file();
		if (f == null) return;
		NbtCompound root = root(f);
		if (!root.contains("players")) return;
		root.put("players", without(root, name));
		persist(f, root);
	}

	/** The recorded modded dimension + position for a player, or {@code null} if none. */
	public static Entry getPlayer(String name) {
		File f = file();
		if (f == null) return null;
		NbtCompound root = root(f);
		if (!root.contains("players")) return null;
		NbtList players = root.getList("players");
		for (int i = 0; i < players.size(); i++) {
			NbtCompound entry = (NbtCompound) players.get(i);
			if (name.equals(entry.getString("name"))) {
				// Rotation was added later; entries from older saves fall back to "leave NBT as-is".
				boolean hasRotation = entry.contains("yaw");
				return new Entry(entry.getInt("dim"), entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"),
						hasRotation, entry.getFloat("yaw"), entry.getFloat("pitch"));
			}
		}
		return null;
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
