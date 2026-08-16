package com.periut.retroapi.commands;

import com.periut.retroapi.RetroAPI;
import com.periut.retroapi.commands.optionaldep.cryonicconfig.CryonicConfigCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

/**
 * State shared by the command system, and its start-up.
 *
 * <p>Not an entrypoint: RetroAPI's own initialisers call {@link #init()} (common) and, on a client,
 * {@code NetworkingUtil.registerClient()}, so the commands API starts in a defined place in
 * RetroAPI's start-up order rather than racing it.
 */
public final class RetroCommands {
    private RetroCommands() {
    }

    public static final String MOD_ID = "retroapi";

    // other mods located
    public static boolean cryConfig = false;

    // Multiplayer: whether the server runs this mod, and whether we are op on it.
    public static boolean mp_rc = false;
    public static boolean mp_op = false;

    // Filled in from the server, for suggestions.
    public static String[] player_names = null;
    public static List<String> disabled_commands = List.of();

    public static void init() {
        // Cryonic Config is reached by reflection, so require the API to have resolved too -
        // an incompatible version must not register /reloadcryonicconfig.
        cryConfig = FabricLoader.getInstance().isModLoaded("cryonicconfig")
                && CryonicConfigCompat.isAvailable();

        // A dedicated server builds its tree now; a client waits until it has a world, because
        // singleplayer commands run against that world and the tree describes it.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            RetroCommandManager.setInstance(new RetroCommandManager(RegistrationEnvironment.DEDICATED));
            com.periut.retroapi.commands.network.ServerCommandNetworking.register();
        }

        RetroAPI.LOGGER.debug("RetroAPI commands ready");
    }
}
