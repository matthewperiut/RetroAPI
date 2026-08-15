package com.periut.retroapi.mixin.commands.access;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("dataTracker")
    net.minecraft.entity.data.DataTracker getDataTracker();

    /**
     * {@code fallDistance} is protected on Entity, and a mixin cannot {@code @Shadow} a field it inherits
     * rather than declares - so noclip reaches it here instead. Needed because enabling flight mid-fall would
     * otherwise bank the damage and hand it back the moment flight is switched off.
     */
    @Accessor("fallDistance")
    void spc$setFallDistance(float fallDistance);
}
