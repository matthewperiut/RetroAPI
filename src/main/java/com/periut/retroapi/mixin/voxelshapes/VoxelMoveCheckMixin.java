package com.periut.retroapi.mixin.voxelshapes;

import com.periut.retroapi.voxelshapes.HasCollisionVoxelShape;
import com.periut.retroapi.voxelshapes.HasVoxelShape;
import com.periut.retroapi.voxelshapes.VoxelShape;
import net.minecraft.block.Block;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class VoxelMoveCheckMixin {
    @Shadow
    private ServerPlayerEntity player;
    @Shadow
    private MinecraftServer server;

    @Redirect(method = "onPlayerMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;teleport(DDDFF)V", ordinal = 0))
    private void retroapi$verifyBlockIntersectionForBlockVoxelShapes(ServerPlayNetworkHandler instance, double e, double f, double g, float h, float v) {
        ServerWorld world = this.server.getWorld(this.player.dimensionId);
        Box originalPlayerBox = player.boundingBox;
        Box playerBox = Box.create(originalPlayerBox.minX, originalPlayerBox.minY, originalPlayerBox.minZ,
                originalPlayerBox.maxX, originalPlayerBox.maxY, originalPlayerBox.maxZ);

        Vec3i min = new Vec3i((int) Math.floor(playerBox.minX), (int) Math.floor(playerBox.minY), (int) Math.floor(playerBox.minZ));
        Vec3i max = new Vec3i((int) Math.ceil(playerBox.maxX), (int) Math.ceil(playerBox.maxY), (int) Math.ceil(playerBox.maxZ));

        boolean collisionVerified = false;

        for (int x = min.x; x <= max.x; x++) {
            for (int y = min.y; y <= max.y; y++) {
                for (int z = min.z; z <= max.z; z++) {
                    int blockId = world.getBlockId(x, y, z);
                    Block block = (blockId > 0 && blockId < Block.BLOCKS.length) ? Block.BLOCKS[blockId] : null;
                    if (block == null) continue;
                    List<Box> boxes = null;

                    if (block instanceof HasCollisionVoxelShape hasCollisionVoxelShape) {
                        VoxelShape voxelShape = hasCollisionVoxelShape.getCollisionVoxelShape(world, x, y, z);
                        if (voxelShape != null) {
                            boxes = voxelShape.getOffsetBoxes();
                        }
                    } else if (block instanceof HasVoxelShape hasVoxelShape) {
                        VoxelShape voxelShape = hasVoxelShape.getVoxelShape(world, x, y, z);
                        if (voxelShape != null) {
                            boxes = voxelShape.getOffsetBoxes();
                        }
                    } else {
                        // getCollisionShape is null for non-colliding blocks (flowers, the
                        // pink flower, ...). List.of(null) throws, which kicked the player on
                        // any move near one, so guard it.
                        Box collisionShape = block.getCollisionShape(world, x, y, z);
                        boxes = collisionShape != null ? List.of(collisionShape) : null;
                    }

                    if (boxes != null) {
                        for (Box blockBoxPart : boxes) {
                            if (blockBoxPart == null) continue;
                            if (playerBox.intersects(blockBoxPart)) {
                                collisionVerified = true;
                            }
                        }
                    }
                }
            }
        }

        if (collisionVerified) {
            instance.teleport(e, f, g, h, v);
        }
    }
}