package com.periut.retroapi.mixin.movement.client;

import com.periut.retroapi.movement.MovementConstants;
import com.periut.retroapi.movement.api.EntitySprinting;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.client.network.MultiplayerClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerClientPlayerEntity.class)
public class MultiplayerClientPlayerEntityMixin {
    @Shadow
    public ClientNetworkHandler networkHandler;
    @Unique
    boolean wasSprinting = false;

    @Inject(method = "sendMovement", at = @At("HEAD"))
    void tick(CallbackInfo ci) {
        boolean sprinting = ((EntitySprinting) (Object) this).isSprinting();
        if (sprinting != wasSprinting) {
            if (sprinting) {
                this.networkHandler.sendPacket(new ClientCommandC2SPacket((Entity) (Object) this, MovementConstants.START_RUNNING_COMMAND));
            } else {
                this.networkHandler.sendPacket(new ClientCommandC2SPacket((Entity) (Object) this, MovementConstants.STOP_RUNNING_COMMAND));
            }
        }
        wasSprinting = sprinting;
    }
}
