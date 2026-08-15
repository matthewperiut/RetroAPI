package com.periut.retroapi.commands.util;

import com.periut.retroapi.commands.RetroCommands;
import com.periut.retroapi.commands.optionaldep.cryonicconfig.CryonicConfigCompat;

public class ConfigUtil {
    public static void refreshDisabledCommands() {
        String disabled = CryonicConfigCompat.getString(RetroCommands.MOD_ID, "disabledCommands", "");
        RetroCommands.disabled_commands = java.util.List.of(disabled.split(","));
    }
}
