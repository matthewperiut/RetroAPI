package com.periut.retroapi.storage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

/**
 * Somewhere to keep things, for mods and for RetroAPI itself.
 *
 * <p>Beta gives a mod exactly two places to persist anything: entity NBT and block entities. Neither
 * answers "how many times has anyone slept in this world", "does this player own the ability, even
 * after dying" or "how long has this player been on fire this life". This is those three places:
 *
 * <ul>
 *   <li>{@link #world()} - one compound per world. Survives everything short of deleting the save.</li>
 *   <li>{@link #player(PlayerEntity)} - one compound per player, kept across death and dimension
 *       changes. This is where a permanent unlock, a home position or a stat total belongs.</li>
 *   <li>{@link #life(PlayerEntity)} - one compound per player per <em>life</em>, wiped the moment
 *       they respawn. Anything that should not survive dying goes here and needs no cleanup code.</li>
 *   <li>{@link #chunk(net.minecraft.world.World, int, int)} - one compound per chunk, saved in that
 *       chunk's region sidecar so it comes and goes with the region rather than with the world.</li>
 * </ul>
 *
 * <p>Every accessor returns a live {@link NbtCompound}: write into it and the value is saved with the
 * world, no store call and no dirty flag.
 *
 * <pre>{@code
 * RetroData.world().putInt("mymod:portals_lit", count);
 * RetroData.player(player).putBoolean("mymod:knows_recipe", true);
 * RetroData.life(player).putInt("mymod:kills_this_life", kills + 1);
 * }</pre>
 *
 * <p><b>Where this data lives.</b> One file, {@code retroapi/data.dat}, inside the world save - so it
 * is authoritative on whoever owns the world: a dedicated server, or the client in singleplayer. A
 * client connected to a server has no world folder, and there these accessors hand back a scratch
 * compound that is never written; ask the server for anything a client needs to see.
 *
 * <p>Keys are yours to namespace. RetroAPI uses {@code retroapi:...} for its own (gamerules,
 * command blocks); a mod prefixing with its own id will never collide with another's.
 */
public final class RetroData {
	private RetroData() {
	}

	/** Permanent, world-wide. */
	public static NbtCompound world() {
		return RetroDataSidecar.section("world");
	}

	/** Permanent per player: survives death, respawn and dimension changes. */
	public static NbtCompound player(PlayerEntity player) {
		return player(player.name);
	}

	public static NbtCompound player(String playerName) {
		return RetroDataSidecar.playerSection(playerName, "persistent");
	}

	/**
	 * Arbitrary data for one chunk, live: write into it and it is saved with that chunk's region.
	 *
	 * <pre>{@code
	 * RetroData.chunk(world, chunkX, chunkZ).putInt("mymod:ritual_stage", 2);
	 * }</pre>
	 *
	 * <p>Kept in the region's own sidecar rather than in {@code data.dat}, so it loads and unloads with
	 * the region the way chunk data should - a world with data on a hundred thousand chunks does not
	 * read all of it to answer a question about one.
	 *
	 * <p>The compound is created on first ask, so it is never null and a caller never has to check. On
	 * a client connected to a server there is no world folder and this hands back a scratch compound
	 * that is never written, exactly as the other accessors do - ask the server for anything a client
	 * needs to see.
	 */
	public static NbtCompound chunk(net.minecraft.world.World world, int chunkX, int chunkZ) {
		return chunk(world == null || world.dimension == null ? 0 : world.dimension.id, chunkX, chunkZ);
	}

	public static NbtCompound chunk(int dimensionId, int chunkX, int chunkZ) {
		RegionSidecar region = SidecarManager.getRegion(dimensionId, chunkX, chunkZ);
		return region == null ? new NbtCompound() : region.customChunkData(chunkX, chunkZ);
	}

	/** One life only: cleared when this player dies. */
	public static NbtCompound life(PlayerEntity player) {
		return life(player.name);
	}

	public static NbtCompound life(String playerName) {
		return RetroDataSidecar.playerSection(playerName, "life");
	}

	/**
	 * Throws away a player's one-life data. Called for you when they die
	 * ({@code PlayerLifeDataMixin}); a mod would only call this to end a "life" early on its own
	 * terms.
	 */
	public static void clearLife(String playerName) {
		RetroDataSidecar.clearPlayerSection(playerName, "life");
	}
}
