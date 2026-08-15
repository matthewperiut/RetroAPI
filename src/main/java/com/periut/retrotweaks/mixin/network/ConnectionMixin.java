package com.periut.retrotweaks.mixin.network;

import com.periut.retrotweaks.config.Config;

import net.minecraft.network.Connection;
import net.minecraft.network.NetworkHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Socket;
import java.net.SocketException;

/**
 * Turns Nagle's algorithm off on the game socket. From UniTweaks.
 *
 * <p>Nagle holds small packets back to batch them, which is the wrong trade for a game: movement
 * and block updates are small and want to leave immediately. Costs slightly more bandwidth.
 */
@Mixin(Connection.class)
public class ConnectionMixin {

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/net/Socket;setTrafficClass(I)V", shift = At.Shift.AFTER))
	private void retrotweaks$tcpNoDelay(Socket socket, String name, NetworkHandler handler, CallbackInfo ci) throws SocketException {
		if (Config.MULTIPLAYER.tcpNoDelay) socket.setTcpNoDelay(true);
	}
}
