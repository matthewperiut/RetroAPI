package com.periut.retroapi.mixin.commands.intercept;

import com.periut.retroapi.commands.client.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs singleplayer commands.
 *
 * <p>Beta has no integrated server, so there is nowhere to send a command to: the client executes it
 * against its own world. Anything else typed becomes a local chat line, because there is no server
 * to echo it back either.
 */
@Mixin(ClientPlayerEntity.class)
public class AbstractClientPlayerMixin {
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    public void retroapi$runLocally(final String message, final CallbackInfo ci) {
        final Minecraft minecraft = (Minecraft) FabricLoader.getInstance().getGameInstance();
        if (minecraft.isWorldRemote()) {
            return;
        }

        if (message.startsWith("/")) {
            ClientCommands.execute(message.substring(1));
        } else {
            final PlayerEntity player = (PlayerEntity) (Object) this;
            minecraft.inGameHud.addChatMessage("<" + player.name + "> " + message);
        }

        ci.cancel();
    }
}
