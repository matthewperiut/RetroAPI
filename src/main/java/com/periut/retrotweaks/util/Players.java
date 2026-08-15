package com.periut.retrotweaks.util;

import com.periut.retrotweaks.mixin.client.MinecraftAccessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Reaches the local player from code that has no player to hand.
 *
 * <p>Returns null on a dedicated server, where there is no such thing as "the" player - callers are
 * expected to treat that as "nothing to do" rather than as an error.
 *
 * <p>Common mixins call this (block scoring, crops, wool), so it has to LINK on a dedicated server,
 * not merely take the null branch there. {@code Minecraft.player} is typed {@code ClientPlayerEntity},
 * which the loader strips on a server; a method returning it as a {@code PlayerEntity} would make the
 * verifier load the stripped class the moment this class links, i.e. an immediate
 * {@code NoClassDefFoundError} on the first block break. Every client type therefore lives in the
 * nested holder, which is only loaded once the environment check has already passed.
 */
public final class Players {

	private Players() {}

	public static PlayerEntity local() {
		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return null;
		return ClientPlayer.get();
	}

	private static final class ClientPlayer {

		private ClientPlayer() {}

		static PlayerEntity get() {
			net.minecraft.client.Minecraft minecraft = MinecraftAccessor.retrotweaks$getInstance();
			return minecraft == null ? null : minecraft.player;
		}
	}
}
