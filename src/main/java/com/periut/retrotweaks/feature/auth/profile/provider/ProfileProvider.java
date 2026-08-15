package com.periut.retrotweaks.feature.auth.profile.provider;

import com.periut.retrotweaks.feature.auth.profile.GameProfile;

import java.util.concurrent.Future;

public interface ProfileProvider {
    Future<GameProfile> get(String username);
    String getProviderName();
}