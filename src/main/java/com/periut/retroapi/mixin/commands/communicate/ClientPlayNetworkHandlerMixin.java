package com.periut.retroapi.mixin.commands.communicate;
 
 import com.periut.retroapi.commands.RetroCommands;
 import net.minecraft.client.network.ClientNetworkHandler;
 import net.minecraft.network.packet.login.LoginHelloPacket;
 import org.spongepowered.asm.mixin.Mixin;
 import org.spongepowered.asm.mixin.injection.At;
 import org.spongepowered.asm.mixin.injection.Inject;
 import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientNetworkHandler.class)
 public abstract class ClientPlayNetworkHandlerMixin {
     @Inject(method = "onHello", at = @At("TAIL"))
     void askIfOp(LoginHelloPacket par1, CallbackInfo ci) {
         RetroCommands.mp_rc = false;
         RetroCommands.mp_op = false;
         // Joining somewhere new: block-entity state still waiting for a chunk belongs to the world
         // being left, and a position means something else entirely in this one.
         com.periut.retroapi.register.blockentity.PendingBlockEntitySync.clear();
     }
 }