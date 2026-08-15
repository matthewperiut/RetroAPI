package com.periut.retrotweaks.client.gui.multiplayer;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * One entry in the server list: a name and an address. From MojangFix.
 *
 * <p>Ported from {@code pl.telvarost.mojangfixstationapi.client.gui.multiplayer.ServerData}, which
 * used Lombok's {@code @Getter}/{@code @Setter}/{@code @RequiredArgsConstructor}/{@code @NonNull}
 * for this class - RetroTweaks does not depend on Lombok, so those are spelled out by hand here.
 */
public class ServerData {

	private String name;
	private String ip;

	public ServerData(String name, String ip) {
		this.name = Objects.requireNonNull(name, "name is marked non-null but is null");
		this.ip = Objects.requireNonNull(ip, "ip is marked non-null but is null");
	}

	public ServerData(NbtCompound nbt) {
		this(nbt.getString("name"), nbt.getString("ip"));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = Objects.requireNonNull(name, "name is marked non-null but is null");
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = Objects.requireNonNull(ip, "ip is marked non-null but is null");
	}

	public NbtCompound save() {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("name", name);
		nbt.putString("ip", ip);
		return nbt;
	}

	public static NbtList save(List<ServerData> servers) {
		NbtList nbt = new NbtList();
		for (ServerData server : servers) {
			nbt.add(server.save());
		}
		return nbt;
	}

	public static List<ServerData> load(NbtList nbt) {
		ArrayList<ServerData> servers = new ArrayList<>();
		for (int i = 0; i < nbt.size(); i++) {
			servers.add(new ServerData((NbtCompound) nbt.get(i)));
		}
		return servers;
	}
}
