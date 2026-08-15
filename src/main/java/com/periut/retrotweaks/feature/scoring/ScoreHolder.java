package com.periut.retrotweaks.feature.scoring;

/**
 * Implemented by {@code PlayerEntity} through a mixin, so every player carries their own score.
 *
 * <p>An interface on the entity rather than a map keyed by player: the counters then live and die
 * with the player object, are saved and loaded with it, and cannot leak when someone disconnects.
 */
public interface ScoreHolder {

	Score.Fields retrotweaks$score();
}
