package com.periut.retroapi.mixin.commands.intercept;

import com.periut.retroapi.commands.RetroCommandManager;
import com.periut.retroapi.commands.ServerCommandSources;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayerPacketHandlerMixin {
    @Shadow private ServerPlayerEntity player;

    @Shadow public abstract void sendPacket(Packet arg);

    /**
     * Every command a player types goes to the dispatcher, whether or not they are an operator -
     * the tree's own requirements decide what they may run, and an unknown command has to produce
     * the same "Unknown command" it would in modern rather than beta's silent shrug.
     */
    @Inject(method = "handleCommand", at = @At(value = "HEAD"), cancellable = true)
    private void retroapi$dispatch(String command, CallbackInfo ci) {
        final RetroCommandManager manager = RetroCommandManager.getInstance();
        if (manager == null) {
            return;
        }

        manager.execute(ServerCommandSources.forPlayer(this.player), command.substring(1));
        ci.cancel();
    }
}
