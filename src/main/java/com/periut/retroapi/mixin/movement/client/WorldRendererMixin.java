package com.periut.retroapi.mixin.movement.client;

import com.periut.retroapi.movement.particle.BlockDustParticleRetroAPI;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.periut.retroapi.movement.RetroMovement.stapi;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Shadow
    private World world;

    @Shadow
    private Minecraft client;

    @Inject(method = "addParticle", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z", ordinal = 0), cancellable = true)
    void addCustomParticle(String particleName, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        // Under StationAPI the atlas is StationAPI's, and its own particle mixins already place
        // sprites on it correctly - so leave vanilla's tilecrack path alone rather than spawning a
        // particle that would sample the wrong atlas.
        if (stapi || !particleName.startsWith("tilecrack_")) {
            return;
        }

        int blockId = Integer.parseInt(particleName.substring(particleName.indexOf("_") + 1));
        this.client.particleManager.addParticle(new BlockDustParticleRetroAPI(
            this.world, x, y, z, velocityX, velocityY, velocityZ, Block.BLOCKS[blockId], 0, 0));
        ci.cancel();
    }
}
