package com.periut.retrotweaks.mixin.auth;

import net.minecraft.network.packet.handshake.HandshakePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Widens the handshake string-length cap. Vanilla b1.7.3 reads the handshake
 * string with a small max ({@code readString(stream, 32)}); modern
 * auth/companion mods can carry a longer payload through the handshake, which
 * otherwise trips {@code "Received string length longer than maximum allowed"}.
 *
 * <p>This modifies the {@code readString} argument rather than the {@code 32}
 * constant on purpose. {@code @ModifyConstant} is a constant <em>redirect</em>,
 * and mixin allows only one redirect per constant: glass-networking ships its
 * own widener on this same constant, so whichever mod applied second was
 * skipped and then died on its own injection check. {@code @ModifyArg} wraps
 * the call argument instead, which is not exclusive, so several wideners can
 * coexist. RetroAuth widens the same argument the same way.
 */
@Mixin(HandshakePacket.class)
public abstract class HandshakePacketMixin {

    @ModifyArg(
            method = "read",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/handshake/HandshakePacket;readString(Ljava/io/DataInputStream;I)Ljava/lang/String;"
            ),
            index = 1
    )
    private int retrotweaks$widenHandshakeLimit(int originalMax) {
        // Never narrow: another widener may already have raised this past 512.
        return Math.max(originalMax, 512);
    }
}
