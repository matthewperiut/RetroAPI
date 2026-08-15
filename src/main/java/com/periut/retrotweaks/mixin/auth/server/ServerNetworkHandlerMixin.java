package com.periut.retrotweaks.mixin.auth.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.packet.login.LoginHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Replaces vanilla b1.7.3 online-mode verification (which hits the long-dead
 * {@code www.minecraft.net/game/checkserver.jsp} and NPEs on the null response)
 * with a modern Mojang {@code sessionserver.mojang.com/session/minecraft/hasJoined}
 * check.
 *
 * <p>Vanilla runs the auth inside an anonymous {@link Thread} created in
 * {@code onHello}. Older retroauth builds tried to {@code @Overwrite} that
 * thread's {@code run()} by targeting it as an inner class - a name that is not
 * stable across mapping sets / mapping builds and silently failed to apply on
 * the gen2 runtime (vanilla auth then ran and NPE'd). Instead we inject at the
 * head of {@code onHello} itself and take over the online path, which is
 * mapping-stable and applies cleanly on gen2.
 */
@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerNetworkHandlerMixin {
    private static final String SESSION_SERVER_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";

    @Shadow private MinecraftServer server;
    @Shadow private String serverId;
    @Shadow private String username;
    @Shadow private LoginHelloPacket loginPacket;

    @Shadow public abstract void disconnect(String reason);

    @Inject(method = "onHello", at = @At("HEAD"), cancellable = true)
    private void retrotweaks$modernAuth(LoginHelloPacket packet, CallbackInfo ci) {
        if (!com.periut.retrotweaks.config.Config.MULTIPLAYER.modernAuthentication) return;
        // Offline servers keep vanilla behaviour (it just accepts the player).
        if (!this.server.onlineMode) {
            return;
        }

        // Mirror vanilla's protocol-version gate before we take over.
        if (packet.protocolVersion != 14) {
            disconnect(packet.protocolVersion > 14 ? "Outdated server!" : "Outdated client!");
            ci.cancel();
            return;
        }

        this.username = packet.username;
        final String user = packet.username;
        final String sid = this.serverId;

        new Thread(() -> {
            try {
                String requestUrl = SESSION_SERVER_URL
                        + "?username=" + URLEncoder.encode(user, StandardCharsets.UTF_8)
                        + "&serverId=" + URLEncoder.encode(sid, StandardCharsets.UTF_8);

                HttpURLConnection connection = (HttpURLConnection) new URL(requestUrl).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    JsonObject json = JsonParser.parseReader(
                            new InputStreamReader(connection.getInputStream())).getAsJsonObject();
                    com.periut.retrotweaks.RetroTweaks.LOGGER.info("Authenticated {} ({})",
                            json.get("name").getAsString(), json.get("id").getAsString());
                    // tick() picks this up on the main thread and calls accept().
                    this.loginPacket = packet;
                    return;
                }

                disconnect("Failed to authenticate!");
            } catch (Exception e) {
                disconnect("Failed to authenticate! [internal error " + e + "]");
            }
        }, "RetroAuth Server Verifier").start();

        ci.cancel();
    }
}
