package com.periut.retrotweaks.feature.auth.session;

import com.periut.retrotweaks.feature.auth.profile.GameProfile;

public interface SessionData {
    GameProfile getGameProfile();

    String getAccessToken();
}
