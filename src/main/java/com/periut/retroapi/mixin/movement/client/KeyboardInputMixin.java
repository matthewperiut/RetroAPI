package com.periut.retroapi.mixin.movement.client;

import com.periut.retroapi.movement.RetroMovement;
import com.periut.retroapi.movement.api.EntitySwimming;
import com.periut.retroapi.compat.StationBridges;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.periut.retroapi.movement.RetroMovement.stapi;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input {
    @Shadow
    private boolean[] keys;

    @Inject(method = "updateKey", at = @At("TAIL"))
    void extraInput(int i, boolean bl, CallbackInfo ci) {
        // StationAPI has a keybind screen, so the sprint key is a real rebindable binding there and
        // is read back through the compat bridge. Without it there is nowhere to rebind, so the key
        // is the fixed default.
        int sprintKey = stapi ? StationBridges.get().sprintKeyCode() : RetroMovement.runKeyCode;
        if (i == sprintKey) {
            keys[6] = bl;
        }
    }

    @Inject(method = "update", at = @At("TAIL"))
    public void addRunKey(PlayerEntity par1, CallbackInfo ci) {
        unused = keys[6];

        // Sneaking scales movement to 30% in vanilla. Modern turns crouching off entirely while
        // swimming - shift is the dive control there, not a slow walk - so undo the scaling and
        // let the swim keep its full stroke.
        if (sneaking && ((EntitySwimming) par1).isSwimming()) {
            movementSideways = (keys[2] ? 1.0F : 0.0F) - (keys[3] ? 1.0F : 0.0F);
            movementForward = (keys[0] ? 1.0F : 0.0F) - (keys[1] ? 1.0F : 0.0F);
        }
    }
}
