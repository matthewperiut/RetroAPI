package com.periut.retrotweaks.mixin.auth;

import net.minecraft.network.packet.handshake.HandshakePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Widens the handshake string-length cap. Vanilla b1.7.3 reads the handshake
 * string with a small max ({@code readString(stream, 32)}); modern
 * auth/companion mods can carry a longer payload through the handshake, which
 * otherwise trips {@code "Received string length longer than maximum allowed"}.
 *
 * <p>Bumping the constant (rather than {@code @ModifyArg} on the inherited
 * {@code readString} call, whose owner is ambiguous between HandshakePacket and
 * its Packet superclass) is mapping-stable and always applies.
 */
@Mixin(HandshakePacket.class)
public abstract class HandshakePacketMixin {

    // require = 0: glass-networking ships an identical @ModifyConstant on this
    // same constant. Only one injector can rewrite it, so when glass-networking
    // is present it wins and this one finds 0 targets. Without require = 0 that
    // "(0/1) succeeded" is a fatal InjectionError that aborts HandshakePacket
    // transformation and crashes the game. Tolerating 0 lets us apply when alone
    // and cleanly no-op when glass-networking already widened the cap.
    @ModifyConstant(method = "read", constant = @Constant(intValue = 32), require = 0)
    private int retrotweaks$widenHandshakeLimit(int original) {
        return 512;
    }
}
