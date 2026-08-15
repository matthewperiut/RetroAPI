package com.periut.retroapi.mixin.commands.feature;

import com.periut.retroapi.commands.api.PlayerWarps;
import com.periut.retroapi.commands.builtin.GodCommand;
import com.periut.retroapi.gamemode.RetroFlight;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Priority 999 to cancel fire even when in creative, because creative also cancels here.
// BHCreative uses priority 1000
@Mixin(value = PlayerEntity.class, priority = 999)
public class PlayerBaseMixin implements PlayerWarps {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    public void onGod(Entity i, int par2, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (GodCommand.isInvincible(player.name)) {
            if (player.fireTicks > 0) {
                player.fireTicks = 0;
            }
            cir.cancel();
        }
    }

    /**
     * A player who passes through terrain is never inside a wall, however far inside one they are.
     *
     * <p>One override for two symptoms, because beta asks this same question for both: {@code LivingEntity}'s
     * base tick suffocates anyone it returns true for, and {@code HeldItemRenderer.renderScreenOverlays} plasters
     * the block's texture over the whole screen. Flying through rock does neither if the answer is no.
     */
    @Inject(method = "isInsideWall", at = @At("HEAD"), cancellable = true)
    private void retroapi$noclipIgnoresWalls(CallbackInfoReturnable<Boolean> cir) {
        if (RetroFlight.ignoresBlocks(((PlayerEntity) (Object) this).name)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    String warpStr = "";

    @Override
    public void spc$setWarpString(String warp) {
        warpStr = warp;
    }

    @Override
    public String spc$getWarpString() {
        return warpStr;
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readFromTag(NbtCompound tag, CallbackInfo ci) {
        if (tag.contains("warps")) warpStr = tag.getString("warps");
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeToTag(NbtCompound tag, CallbackInfo ci) {
        if (!warpStr.isEmpty()) tag.putString("warps", warpStr);
    }
}
