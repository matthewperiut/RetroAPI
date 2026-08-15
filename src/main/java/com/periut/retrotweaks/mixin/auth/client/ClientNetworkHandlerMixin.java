package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.profile.GameProfile;
import com.periut.retrotweaks.feature.auth.session.SessionData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.ClientNetworkHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.handshake.HandshakePacket;
import net.minecraft.network.packet.login.LoginHelloPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.HttpURLConnection;
import java.net.URL;

@Mixin(ClientNetworkHandler.class)
public abstract class ClientNetworkHandlerMixin {
    @Shadow
    private Minecraft minecraft;

    @Shadow
    private Connection connection;

    @Shadow
    public abstract void sendPacket(Packet arg);

    @Redirect(method = "onHandshake", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    private boolean checkServerId(String serverId, Object offline) {
        if (!com.periut.retrotweaks.config.Config.MULTIPLAYER.modernAuthentication) {
            return serverId.equals(offline);
        }
        return serverId.trim().isEmpty() || serverId.equals(offline) || this.minecraft.session.sessionId.trim().isEmpty() || this.minecraft.session.sessionId.equals(offline);
    }

    @Inject(method = "onHandshake", at = @At(value = "NEW", target = "java/net/URL"), cancellable = true)
    private void onJoinServer(HandshakePacket packet, CallbackInfo ci) {
        // With the option off, leave the handshake entirely alone: the session was never parsed
        // into a profile, so carrying on here would kick the player off every online-mode server.
        if (!com.periut.retrotweaks.config.Config.MULTIPLAYER.modernAuthentication) return;

        SessionData session = (SessionData) this.minecraft.session;

        try {
            if (session.getGameProfile() == null || session.getAccessToken() == null) {
                this.connection.disconnect("disconnect.loginFailedInfo", "Invalid access token!");
            }

            joinServer(session.getGameProfile(), session.getAccessToken(), packet.name);
            this.sendPacket(new LoginHelloPacket(this.minecraft.session.username, 14));
        } catch (Exception e) {
            this.connection.disconnect("disconnect.loginFailedInfo", e.getMessage());
        }

        ci.cancel();
    }

    @Unique
    private void joinServer(GameProfile profile, String accessToken, String serverId) throws Exception {
        URL url = new URL("https://sessionserver.mojang.com/session/minecraft/join");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        String payload = String.format(
                "{\"accessToken\":\"%s\",\"selectedProfile\":\"%s\",\"serverId\":\"%s\"}",
                accessToken, profile.getId(), serverId
        );

        connection.getOutputStream().write(payload.getBytes());
        if (connection.getResponseCode() != 204) {
            throw new Exception("Failed to join server: " + connection.getResponseMessage());
        }
    }
}
