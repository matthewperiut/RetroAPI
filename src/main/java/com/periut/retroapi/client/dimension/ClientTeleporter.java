package com.periut.retroapi.client.dimension;

import com.periut.retroapi.dimension.DimensionTeleporter;
import com.periut.retroapi.dimension.TravelMessageProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.PortalForcer;

/**
 * Client-side dimension teleport - RetroAPI's own generalisation of vanilla
 * {@code Minecraft.changeDimension} over an arbitrary destination + scale (single-player and the
 * connected-client view). Touches the client-only {@code Minecraft}, so it is only ever
 * referenced/loaded from the client entrypoint ({@code RetroAPIClient}); the dedicated server never
 * loads it.
 */
public final class ClientTeleporter implements DimensionTeleporter {
	@Override
	public void teleport(PlayerEntity player, int target, int moddedDim, double coordFactor, PortalForcer agent) {
		Minecraft minecraft = (Minecraft) FabricLoader.getInstance().getGameInstance();
		World oldWorld = minecraft.world;
		player.dimensionId = target;
		oldWorld.remove(player);
		player.dead = false;

		double x = player.x * coordFactor;
		double z = player.z * coordFactor;
		player.setPositionAndAnglesKeepPrevAngles(x, player.y, z, player.yaw, player.pitch);
		if (player.isAlive()) {
			oldWorld.updateEntity(player, false);
		}

		World newWorld = new World(oldWorld, Dimension.fromId(target));
		minecraft.setWorld(newWorld, travelMessage(moddedDim, target), player);
		player.world = newWorld;

		if (player.isAlive()) {
			player.setPositionAndAnglesKeepPrevAngles(x, player.y, z, player.yaw, player.pitch);
			newWorld.updateEntity(player, false);
			agent.moveToPortal(newWorld, player);
		}
	}

	/** The loading-screen message: the modded dimension supplies a TRANSLATION KEY via {@link TravelMessageProvider}. */
	private static String travelMessage(int moddedDim, int target) {
		Dimension modded = Dimension.fromId(moddedDim);
		if (modded instanceof TravelMessageProvider) {
			TravelMessageProvider provider = (TravelMessageProvider) modded;
			boolean entering = (target == moddedDim);
			String key = entering ? provider.getEnteringTranslationKey() : provider.getLeavingTranslationKey();
			// Translate here - setWorld displays the string verbatim. Untranslated keys come back as-is.
			return I18n.getTranslation(key);
		}
		return "";
	}
}
