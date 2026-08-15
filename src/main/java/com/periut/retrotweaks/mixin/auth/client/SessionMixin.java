package com.periut.retrotweaks.mixin.auth.client;

import com.periut.retrotweaks.feature.auth.profile.GameProfile;
import com.periut.retrotweaks.feature.auth.session.SessionData;
import com.periut.retrotweaks.feature.auth.skin.SkinService;
import net.minecraft.client.util.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.regex.Pattern;

@Mixin(Session.class)
public class SessionMixin implements SessionData {
    @Unique
    private static final Pattern UUID_PATTERN = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    @Unique
    private GameProfile gameProfile;
    @Unique
    private String accessToken;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(String username, String sessionId, CallbackInfo ci) {
        if (!com.periut.retrotweaks.config.Config.MULTIPLAYER.modernAuthentication) return;

        // Modern launchers hand the session in as "token:<accessToken>:<uuid>", which b1.7.3
        // treats as an opaque string; splitting it out is what makes online-mode servers work.
        String[] split = sessionId.split(":");
        if (split.length == 3 && split[0].equalsIgnoreCase("token")) {
            accessToken = split[1];
            UUID uuid = UUID.fromString(UUID_PATTERN.matcher(split[2]).replaceAll("$1-$2-$3-$4-$5"));
            gameProfile = new GameProfile(uuid.toString(), username, null, null, null, null);

            com.periut.retrotweaks.RetroTweaks.LOGGER.info("Signed in as {} ({})", username, uuid);
        } else {
            com.periut.retrotweaks.RetroTweaks.LOGGER.info("Signed in as {}", username);
        }

        SkinService.getInstance().init(username);
    }

    @Override
    public GameProfile getGameProfile() {
        return gameProfile;
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }
}
