package com.periut.retroapi.dimension;

import com.periut.retroapi.RetroAPI;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.dimension.PortalForcer;
import net.ornithemc.osl.core.api.util.NamespacedIdentifier;

/**
 * Moves a player between the overworld and a registered modded dimension - RetroAPI's own
 * generalisation of vanilla's hard-coded overworld&lt;-&gt;nether teleport, parameterised over an
 * arbitrary destination identifier, a coordinate scale, and a {@link PortalForcer}.
 *
 * <p>This class is environment-neutral: it references only generic classes so it loads on both the
 * client and the dedicated server. The actual move is delegated to a {@link DimensionTeleporter}
 * registered per environment ({@code ClientTeleporter} on the client, {@code ServerTeleporter} on the
 * server) - so neither side ever loads the other's environment-specific classes (this is what
 * prevents the "cannot load ServerPlayerEntity in environment type CLIENT" failure).
 *
 * <p><b>Why two teleporter fields instead of one.</b> The b1.7.3 INTEGRATED SERVER (RetroNetwork's
 * dev/merged-jar in-process {@code MinecraftServer}) runs BOTH entrypoints in the same JVM:
 * {@code RetroAPIClient.initClient()} registers the {@link #setClientTeleporter client} back end and
 * {@code RetroAPIServer.initServer()} registers the {@link #setServerTeleporter server} back end. A
 * single static field would let whichever entrypoint ran last clobber the other, so server players
 * could be sent through the client teleporter (which casts to client-only classes) or vice versa.
 * Holding both and selecting per call fixes that, while a pure client (only the client field set) and
 * a dedicated server (only the server field set) behave exactly as before.
 *
 * <p>Note that under the integrated server the actual transition is <b>server-driven</b> (the vanilla
 * client portal path is gated behind {@code !world.isRemote}, which is false for an integrated/networked
 * client), so in practice only {@code serverTeleporter} fires there. The split's value is defence in
 * depth - it guarantees the client entrypoint can never overwrite the server back end, and
 * {@code selectTeleporter} additionally refuses to route a real server player through the client back end
 * if the server slot is somehow unset. See {@link #selectTeleporter} for the full per-side routing.
 *
 * <p>Like vanilla, a transition flips between the destination and the overworld: a player already in
 * the destination is sent back to the overworld.
 */
public final class DimensionHelper {
	private DimensionHelper() {}

	/** Set from {@code RetroAPIClient.initClient()}. May be null on a dedicated server. */
	private static DimensionTeleporter clientTeleporter;
	/** Set from {@code RetroAPIServer.initServer()}. May be null on a pure client. */
	private static DimensionTeleporter serverTeleporter;

	/** Registered once from {@code RetroAPIClient}. */
	public static void setClientTeleporter(DimensionTeleporter teleporter) {
		DimensionHelper.clientTeleporter = teleporter;
	}

	/** Registered once from {@code RetroAPIServer}. */
	public static void setServerTeleporter(DimensionTeleporter teleporter) {
		DimensionHelper.serverTeleporter = teleporter;
	}

	/**
	 * Back-compat shim for third-party callers of the old single-teleporter API.
	 *
	 * <p>The original {@code setTeleporter} stored one teleporter for "this environment". We cannot
	 * know at this call site whether the supplied teleporter is the client or server back end, and the
	 * back-compat-safest choice is to register it for <b>both</b> sides: a third-party caller that only
	 * ever set one teleporter was, by construction, running in a single-sided context (pure client or
	 * dedicated server), so only one of the two selection branches can ever fire there anyway - storing
	 * it in both slots reproduces the old "one teleporter, always used" behavior exactly. (RetroAPI's
	 * own entrypoints no longer call this; they call the side-specific setters above, so they never
	 * cross-populate the wrong slot.)
	 *
	 * @deprecated use {@link #setClientTeleporter} / {@link #setServerTeleporter} instead.
	 */
	@Deprecated
	public static void setTeleporter(DimensionTeleporter teleporter) {
		DimensionHelper.clientTeleporter = teleporter;
		DimensionHelper.serverTeleporter = teleporter;
	}

	/**
	 * Pick the teleporter that matches this player's side WITHOUT loading any server-only class from
	 * this common-code class.
	 *
	 * <p><b>Why {@code player.world.isRemote} and never {@code instanceof ServerPlayerEntity}.</b> On a
	 * production <i>split</i> client jar the class {@code net.minecraft.entity.player.ServerPlayerEntity}
	 * is physically absent. An {@code instanceof} against it bakes a {@code CONSTANT_Class} ref into this
	 * method's constant pool; resolving it at verify/link time on a client where the class is missing
	 * risks {@code NoClassDefFoundError}. {@code World.isRemote} is a plain {@code boolean} field on the
	 * common {@code World} class and {@code Entity.world} is a common field, so this branch touches zero
	 * server-only classes - it is safe to evaluate on a pure client.
	 *
	 * <p><b>How a player reaches this method.</b> Exactly two mixins call
	 * {@code DimensionHelper.switchDimension}: the server-side {@code ServerPlayerEntityMixin} fires from
	 * {@code ServerPlayerEntity.playerTick} and always hands us a real {@code ServerPlayerEntity}; the
	 * client-side {@code ClientPlayerEntityMixin} fires from {@code ClientPlayerEntity.tickMovement} and
	 * always hands us a {@code ClientPlayerEntity}. That client redirect target
	 * ({@code Minecraft.changeDimension}) is itself guarded by {@code if (!this.world.isRemote)} in
	 * vanilla {@code tickMovement}, so on a networked/integrated client ({@code isRemote==true}) the
	 * client teleport path never fires at all - the transition is driven entirely server-side.
	 *
	 * <p><b>All four runtime matrices</b> ({@code isRemote==false} =&gt; server-authoritative world,
	 * {@code isRemote==true} =&gt; networked client world):
	 * <ol>
	 *   <li><b>Dedicated server</b> - only {@code serverTeleporter} set; every player is a
	 *       {@code ServerPlayerEntity} in a {@code ServerWorld} ({@code isRemote==false}). Prefer
	 *       server, present =&gt; <b>serverTeleporter</b>. (Unchanged from 0.2.0.)</li>
	 *   <li><b>Pure client connected to a remote server</b> - only {@code clientTeleporter} set; the
	 *       player is a plain client entity in the networked client world ({@code isRemote==true}). The
	 *       vanilla {@code !isRemote} guard means the client redirect does not even fire here; vanilla
	 *       handles the transition. (Unchanged.)</li>
	 *   <li><b>Vanilla single-player (separate client world + hidden integrated world, pre-merge)</b> -
	 *       only {@code clientTeleporter} set. The entity the client portal mixin hands us is a
	 *       {@code ClientPlayerEntity} in the CLIENT world. Although that local world is also
	 *       {@code isRemote==false}, the entity is NOT a server player, so the server-player guard below
	 *       does not fire and we fall through to <b>clientTeleporter</b>. The client fallback - not the
	 *       isRemote bit - is what makes this case correct.</li>
	 *   <li><b>Integrated server (RetroNetwork dev/merged jar, both entrypoints in one JVM)</b> - the
	 *       transition is <b>server-driven</b>: {@code ServerPlayerEntityMixin} hands us a real
	 *       {@code ServerPlayerEntity} in a {@code ServerWorld} ({@code isRemote==false}) =&gt; server
	 *       branch =&gt; <b>serverTeleporter</b>. The client-side redirect ({@code Minecraft.changeDimension})
	 *       is inert because the integrated client's world is networked ({@code isRemote==true}), so the
	 *       client teleporter does not participate in integrated transitions at all. This routing is only
	 *       correct if {@code serverTeleporter} is actually registered (i.e. {@code RetroAPIServer.initServer}
	 *       ran in this JVM); see the defensive guard below for what happens if it was not.</li>
	 * </ol>
	 *
	 * <p>Selection rule: a non-remote world ({@code isRemote==false}) is server-authoritative, so prefer
	 * {@code serverTeleporter} when it exists; otherwise prefer {@code clientTeleporter}. Either way fall
	 * back to whichever single teleporter is non-null so a single-sided install never no-ops.
	 *
	 * <p><b>Defensive guard: never run the client back end for a real server player.</b> If a server
	 * player reaches us in a server-authoritative world but {@code serverTeleporter} is null (e.g. an
	 * integrated/in-process {@code MinecraftServer} that booted WITHOUT firing OSL {@code server-init},
	 * so {@code RetroAPIServer.initServer} never registered the server back end), the old fall-through
	 * would hand that {@code ServerPlayerEntity} to {@code ClientTeleporter}, which casts the game
	 * instance to {@code Minecraft} and mutates {@code minecraft.world} - corrupting client state or
	 * throwing {@code ClassCastException}. Instead we log a loud warning and return {@code null} so
	 * {@code switchDimension} no-ops, making the misconfiguration diagnosable rather than silently
	 * destructive. The "is this a server player" check uses the runtime class's NAME as a String, never
	 * {@code instanceof ServerPlayerEntity}: a {@code String} literal is a {@code CONSTANT_String}, so it
	 * bakes no {@code CONSTANT_Class} ref into this common method and stays verify/link-safe on a split
	 * client where {@code net.minecraft.entity.player.ServerPlayerEntity} is physically absent.
	 */
	private static final String SERVER_PLAYER_CLASS_NAME = "net.minecraft.entity.player.ServerPlayerEntity";

	private static DimensionTeleporter selectTeleporter(PlayerEntity player) {
		// isRemote==false => server-authoritative world (dedicated, integrated server world, or the
		// vanilla SP local world). Server-authoritative + a registered server back end => use it.
		boolean serverAuthoritative = (player.world != null) && !player.world.isRemote;
		if (serverAuthoritative && serverTeleporter != null) {
			return serverTeleporter;
		}
		// Defensive guard: a real ServerPlayerEntity must NEVER run the client teleporter. If we got here
		// it means the server back end is missing (serverTeleporter==null) while a genuine server player
		// is mid-transition - refuse to fall through to clientTeleporter, which would corrupt client
		// state. Class-name-as-String comparison keeps this branch free of any server-only class ref.
		if (serverAuthoritative && isServerPlayer(player)) {
			RetroAPI.LOGGER.warn(
				"switchDimension: a server player reached the dimension change but no serverTeleporter is "
				+ "registered (RetroAPIServer.initServer did not run in this JVM - e.g. an integrated "
				+ "MinecraftServer booted without firing OSL 'server-init'). Refusing to run the CLIENT "
				+ "teleporter for a server player; transition skipped.");
			return null;
		}
		if (clientTeleporter != null) {
			return clientTeleporter;
		}
		// Fallbacks: whichever is non-null (covers any single-sided install).
		return serverTeleporter;
	}

	/**
	 * Side-safe "is this a server player" check. Compares the runtime class name as a {@link String} so
	 * no {@code CONSTANT_Class} ref to {@code ServerPlayerEntity} is baked into this common method - safe
	 * to evaluate on a split client where that class is absent. Matches the exact concrete type rather
	 * than a hierarchy because only {@code ServerPlayerEntity} carries the client-incompatible state.
	 */
	private static boolean isServerPlayer(PlayerEntity player) {
		return SERVER_PLAYER_CLASS_NAME.equals(player.getClass().getName());
	}

	public static void switchDimension(PlayerEntity player, NamespacedIdentifier destination, double scale, PortalForcer travelAgent) {
		DimensionRegistration reg = RetroDimensionRegistry.getByIdentifier(destination);
		if (reg == null) {
			RetroAPI.LOGGER.warn("switchDimension: no registered dimension {}", destination);
			return;
		}
		DimensionTeleporter teleporter = selectTeleporter(player);
		if (teleporter == null) {
			RetroAPI.LOGGER.warn("switchDimension: no DimensionTeleporter registered for this environment");
			return;
		}
		if (scale <= 0.0) scale = 1.0;
		int destId = reg.getSerialId();
		// Flip between the destination and the overworld (mirrors vanilla nether<->overworld).
		int target = (player.dimensionId == destId) ? RetroDimensionRegistry.OVERWORLD_ID : destId;
		boolean entering = (target == destId);
		// Vanilla scales nether coords by /8 entering, *8 leaving; scale==1 (the default) is identity.
		double coordFactor = entering ? (1.0 / scale) : scale;

		teleporter.teleport(player, target, destId, coordFactor, travelAgent);
	}
}
