package com.periut.retroapi.commands.util;

import com.periut.retroapi.commands.RetroCommands;
import com.periut.retroapi.commands.RetroCommandsNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.ornithemc.osl.networking.api.server.ServerPlayNetworking;

import java.util.logging.Logger;

/** Server-side odds and ends the commands and the networking code share. */
public class ServerUtil {
    public static Logger LOGGER = Logger.getLogger("Minecraft");

    public static MinecraftServer getServer() {
        return (MinecraftServer) FabricLoader.getInstance().getGameInstance();
    }

    public static PlayerManager getConnectionManager() {
        return getServer().playerManager;
    }

    /** Announces an operator's action to everyone and writes it to the log, as vanilla does. */
    public static void sendFeedbackAndLog(String user, String message) {
        String str = user + ": " + message;
        getConnectionManager().broadcast("§7(" + str + ")");
        LOGGER.info(str);
    }

    public static boolean isOp(String name) {
        return getServer().playerManager.isOperator(name);
    }

    /** Moving a server player has to go through their connection, or only the server believes it. */
    public static void serverTeleport(PlayerEntity p, double x, double y, double z) {
        ServerPlayerEntity sp = (ServerPlayerEntity) p;
        sp.networkHandler.teleport(x, y, z, p.yaw, p.pitch);
    }

    /** Tells a client whether it is op, so its own command tree matches what the server allows. */
    public static void informPlayerOpStatus(String playerName) {
        ServerPlayerEntity player = getConnectionManager().getPlayer(playerName);
        if (player == null) return;
        ServerPlayNetworking.send(player, RetroCommandsNetworking.OP_CHANNEL, buffer -> {
            buffer.writeBoolean(isOp(playerName));
        });
    }

    public static void informPlayerDisabledCommands(String playerName) {
        ServerPlayerEntity player = getConnectionManager().getPlayer(playerName);
        if (player == null) return;
        ServerPlayNetworking.send(player, RetroCommandsNetworking.DISABLED_CHANNEL, buffer -> {
            if (!RetroCommands.cryConfig) {
                buffer.writeString("");
            } else {
                buffer.writeString(String.join(",", RetroCommands.disabled_commands));
            }
        });
    }
}
