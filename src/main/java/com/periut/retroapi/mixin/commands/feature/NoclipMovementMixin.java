package com.periut.retroapi.mixin.commands.feature;

import com.periut.retroapi.gamemode.RetroFlight;
import com.periut.retroapi.gamemode.RetroGameMode;
import com.periut.retroapi.gamemode.RetroGameModes;
import com.periut.retroapi.mixin.commands.access.EntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Flight: creative, spectator and {@code /noclip}, all three.
 *
 * <p>{@code LivingEntity.travel} is the right place and the only tidy one. It is where beta turns movement
 * input into velocity and calls {@code move}, and it already receives the two input axes as arguments, so
 * cancelling it and doing the movement here takes over exactly the physics that should be taken over -
 * gravity, friction, water drag, ladders, slipperiness - and nothing else. Everything upstream of it (input
 * collection, portals, the sneak camera offset) still runs.
 *
 * <p><b>The physics are modern's, not a constant-speed glide.</b> Input accelerates, air friction bleeds
 * speed off, and letting go coasts to a stop - which is what makes flight feel like flight rather than like
 * being dragged. The numbers are modern's own: {@code 0.05} of acceleration per tick (doubled while
 * sprinting), {@code 0.91} horizontal friction and {@code 0.6} vertical, giving the same ~11 blocks/second
 * top speed. {@code /noclip} and spectator scale the acceleration by their scroll-wheel throttle.
 *
 * <p>Placed on {@code LivingEntity} rather than the client player class because {@code travel} is declared
 * here, and because the check has to hold on both sides: a server that does not know the player is flying
 * simulates the same move with collision and shoves them back out of the wall.
 */
@Mixin(LivingEntity.class)
public abstract class NoclipMovementMixin {

    /** Modern's {@code Abilities.flySpeed}. */
    private static final double FLY_ACCELERATION = 0.05;
    private static final double HORIZONTAL_FRICTION = 0.91;
    private static final double VERTICAL_FRICTION = 0.6;
    /** Modern applies the vertical control as three times the fly speed. */
    private static final double VERTICAL_CONTROL = 3.0;

    @Shadow protected boolean jumping;

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void retroapi$noclipFlight(float sideways, float forward, CallbackInfo ci) {
        if (!((Object) this instanceof PlayerEntity player)) {
            return;
        }

        Entity self = (Entity) (Object) this;

        if (!RetroFlight.isFlying(player)) {
            // Self-healing, and the fix for the worst way this could go wrong: nothing in beta ever sets
            // noClip on a PLAYER, so a player who has it set got it from here. Leaving spectator (or
            // /noclip, or a disconnect mid-flight) used to leave the flag on forever - gravity applied,
            // terrain ignored, no way back. Clearing it whenever a player is not flying means even a world
            // saved in that state repairs itself on the next tick.
            if (self.noClip) {
                self.noClip = false;
            }
            return;
        }

        // Set every tick rather than once at toggle time: a respawn or a dimension change builds a new
        // entity, and this way RetroFlight stays the single source of truth. Creative flight keeps
        // collision (walls stop you, as in modern); /noclip and spectator do not.
        boolean ghost = RetroFlight.ignoresBlocks(player);
        self.noClip = ghost;
        ((EntityAccessor) self).spc$setFallDistance(0.0F);

        double throttle = RetroFlight.speed(player.name);
        double acceleration = FLY_ACCELERATION * throttle * (isSprinting(player) ? 2.0 : 1.0);

        // Vertical is direct control, horizontal is acceleration in the direction of travel; beta's
        // moveNonSolid does the normalise-and-rotate-by-yaw and ADDS to velocity, which is exactly the
        // accumulate-then-decay behaviour wanted here.
        if (this.jumping) {
            self.velocityY += FLY_ACCELERATION * throttle * VERTICAL_CONTROL;
        }
        if (self.isSneaking()) {
            self.velocityY -= FLY_ACCELERATION * throttle * VERTICAL_CONTROL;
        }
        self.moveNonSolid(sideways, forward, (float) acceleration);

        self.move(self.velocityX, self.velocityY, self.velocityZ);

        self.velocityX *= HORIZONTAL_FRICTION;
        self.velocityZ *= HORIZONTAL_FRICTION;
        self.velocityY *= VERTICAL_FRICTION;

        retroapi$updateWalkAnimation(self);

        // Touching down ends flight, as it does in modern - for a survival player given wings by /fly
        // just as much as for a creative one, because the whole point of that permission is that it
        // feels the same. A spectator has nothing to land on and /noclip is explicitly toggled, so
        // neither is affected.
        if (!ghost && self.onGround) {
            RetroGameModes.setFlying(player.name, false);
        }

        ci.cancel();
    }

    /**
     * The tail of {@code LivingEntity.travel}, which cancelling that method skips.
     *
     * <p>It is what drives the limbs: the swing speed chases the distance actually covered this tick and
     * the swing progress accumulates it. Skipped, both keep the values they held the instant flight
     * began, so the model hangs in whatever half-step it was mid-stride on - the frozen, glitchy pose.
     * Modern animates a flying player from the same movement delta, which is why they run through the
     * air rather than glide rigid, and why coming to a hover settles them back to standing.
     */
    private static void retroapi$updateWalkAnimation(final Entity self) {
        if (!((Object) self instanceof LivingEntity living)) {
            return;
        }

        living.lastWalkAnimationSpeed = living.walkAnimationSpeed;
        final double dx = self.x - self.prevX;
        final double dz = self.z - self.prevZ;
        float speed = MathHelper.sqrt(dx * dx + dz * dz) * 4.0F;
        if (speed > 1.0F) {
            speed = 1.0F;
        }

        living.walkAnimationSpeed += (speed - living.walkAnimationSpeed) * 0.4F;
        living.walkAnimationProgress += living.walkAnimationSpeed;
    }

    /**
     * The raw sprint flag, not "is sprinting allowed": the {@code sprinting} game rule governs running on
     * the ground, and a world with it off should still get the faster flight from holding the key.
     */
    private static boolean isSprinting(PlayerEntity player) {
        return player instanceof com.periut.retroapi.movement.api.EntitySprinting sprinting
            && sprinting.isSprinting();
    }
}
