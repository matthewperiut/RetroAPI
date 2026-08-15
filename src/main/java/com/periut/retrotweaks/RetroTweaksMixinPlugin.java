package com.periut.retrotweaks;

import com.periut.retrotweaks.compat.Mods;
import com.periut.retrotweaks.config.Config;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Decides which mixins are worth applying at all.
 *
 * <p>Almost every feature is gated by a config check inside the mixin, because the options have to
 * be changeable without a restart. This is for the handful where that is not enough:
 *
 * <ul>
 *   <li>The session and skin mixins rewrite how the player model and login work, and running them
 *       alongside RetroAuth - which does the same job, and better - would mean two mods fighting
 *       over the same fields. Not loading them at all is the only way to stay out of its way
 *       completely.
 *   <li>{@link #UNITWEAKS_SUPERSEDED_MIXINS} does the same job for UniTweaks. Config-level standing
 *       down (forcing an {@code @Opt(source = Mods.UNITWEAKS)} option off) is not enough on its own:
 *       the mixin still applies and still rewrites bytecode even while the option reads false, and
 *       an {@code @ModifyConstant}/{@code @Redirect}/ordinal-{@code @At} injector that rewrites a
 *       constant or a local can eat the exact instruction another mod's injector was counting on
 *       finding. That is exactly what happened in the field: RetroTweaks' elevator-boats
 *       {@code @ModifyConstant} on {@code BoatEntity.tick} turned UniTweaks' matching constant into a
 *       method call, so UniTweaks' own (un-defended) injector found zero targets and crashed the
 *       whole game during class load. Skipping the whole mixin class removes both the duplicate
 *       feature and the collision risk at once.
 * </ul>
 */
public class RetroTweaksMixinPlugin implements IMixinConfigPlugin {

	/** Mixins ported from RetroAuth. Skipped entirely when the real RetroAuth is installed. */
	private static final String AUTH_PACKAGE = "com.periut.retrotweaks.mixin.auth.";

	private static final String MIXIN_PACKAGE = "com.periut.retrotweaks.mixin.";

	/**
	 * FISHINFOODTWEAKS DISABLED - flip to true to bring the whole FishinFoodTweaks feature set back.
	 *
	 * <p>Turned off on request. These three mixins are fish-only in their entirety, so switching them
	 * off here is cleaner than commenting each file out: the sources stay compiled and registered, so
	 * verify-mixins.py still checks their targets against the real bytecode and will tell you if one
	 * has rotted before you ever re-enable it.
	 *
	 * <p>The rest of the feature is commented out at its call sites rather than here, because it is
	 * not mixin-shaped: the config category in {@code Config.Root}, the non-vanilla fish registration
	 * in {@code RetroTweaksRetroInitializer}, the fish smelting recipes in {@code RecipeTweaks}, and one
	 * single injector inside {@code mixin.item.FoodItemMixin} (whose other two injectors are NOT
	 * fishing - they are the max-health eating guard and the eating sound, both of which stay live).
	 * Every one of those carries the same marker: {@code grep -rn "FISHINFOODTWEAKS DISABLED"}.
	 */
	private static final boolean FISHING_ENABLED = false;

	/** The fish-only mixins, skipped wholesale while {@link #FISHING_ENABLED} is false. */
	private static final Set<String> FISHING_MIXINS = Set.of(
		MIXIN_PACKAGE + "client.gui.ItemTooltipMixin",
		MIXIN_PACKAGE + "entity.FishingBobberEntityMixin",
		MIXIN_PACKAGE + "blockentity.FurnaceFishSizeMixin"
	);

	/**
	 * RetroTweaks mixin classes that are skipped entirely when UniTweaks is installed, because
	 * UniTweaks applies its own mixin to the exact same target class.
	 *
	 * <p><b>Scope, and how this list was built:</b> starting from every RetroTweaks mixin whose feature
	 * traces back to UniTweaks (javadoc says "From UniTweaks", or the matching {@code Config} option
	 * carries {@code @Opt(source = Mods.UNITWEAKS)}), each one was checked against
	 * {@code unitweaks.mixins.json}'s full mixin list for a target-class match - not a feature-name
	 * match, because two mods can collide on the same method even when what they are trying to do is
	 * different (see the class doc). A candidate whose target class UniTweaks never touches was left
	 * out: standing it down would only lose a working feature for no compatibility benefit.
	 *
	 * <p><b>Deliberately NOT listed</b> despite a UniTweaks-derived feature living on a class
	 * UniTweaks also touches - config gating alone is provably safe for these, because every
	 * overlapping injector (both sides) is a plain, non-consuming {@code @Inject} at HEAD/RETURN/TAIL
	 * with no ordinal, and the file also carries an unrelated, non-UniTweaks-sourced feature that
	 * standing it down would needlessly kill:
	 * <ul>
	 *   <li>{@code mixin.auth.client.PlayerEntityRendererMixin} - the leggings-while-riding fix is
	 *       one HEAD inject tacked onto the entire custom skin/cape renderer. Standing down the whole
	 *       file for UniTweaks would take the skin renderer with it.
	 *   <li>{@code mixin.item.FoodItemMixin} - "No Food Wastage" and the eating sound are UniTweaks
	 *       derived, but "Random Fish Sizes" (FishinFoodTweaks, not UniTweaks) shares the same
	 *       {@code use} method and would go down with it.
	 *   <li>{@code mixin.block.ToolItemMixin} - covers axe/pickaxe (UniTweaks) and shovel
	 *       effectiveness (not UniTweaks, no {@code source}) through one shared HEAD inject.
	 *   <li>{@code mixin.entity.PigEntityMixin} - saddle drop (UniTweaks) is on {@code
	 *       getDroppedItemId}; "Pigs Drop Brown Mushrooms" (BetaTweaks) is bundled in the same file
	 *       but on a different method ({@code dropItems}, which UniTweaks reaches by Java override,
	 *       not by touching {@code getDroppedItemId} at all) - no shared instruction either way.
	 *   <li>{@code mixin.entity.SheepEntityMixin} - "Punch Sheep For Wool" and the shear sound are
	 *       UniTweaks derived; "Disable Colored Sheep Spawning" is not and shares the file.
	 *   <li>{@code mixin.block.LeavesBlockMixin} - "Fast Leaf Decay" is UniTweaks derived; "Player
	 *       Placed Leaves Persist" and "Apple Drop Chance" (both MiscTweaks) are not and share the
	 *       file.
	 * </ul>
	 *
	 * <p><b>Listed despite bundling a non-UniTweaks feature</b> - the shared injector is ordinal- or
	 * constant-sensitive (the crashing pattern), so the collision risk outweighs the secondary
	 * feature, which is also off by default:
	 * <ul>
	 *   <li>{@code mixin.entity.BoatEntityMixin} - four separate UniTweaks mixins patch
	 *       {@code BoatEntity} (elevators, dismount, drops-itself, doesn't-break); this file's "Boat
	 *       Collision Behavior" tri-state (not itself {@code @Opt(source = Mods.UNITWEAKS)}) shares
	 *       {@code tick}/{@code damage} with them and stands down too. UniTweaks' own "Boats Don't
	 *       Break" option covers the same ground.
	 *   <li>{@code mixin.block.TrapdoorBlockMixin} - "Trapdoors Without Support" and "Glue Trapdoors
	 *       With A Slimeball" (MiscTweaks, default off) share a {@code @WrapOperation} on {@code
	 *       World.shouldSuffocate} that UniTweaks' own {@code trapdoorplacement.TrapdoorBlockMixin}
	 *       also wraps - defensively {@code require = 0} on UniTweaks' own side too, which is itself
	 *       a sign this exact call site is known to be contested.
	 *   <li>{@code mixin.block.ChestBlockMixin} - "Stackable Chests" and "Chests Open When Blocked"
	 *       (MiscTweaks, default off) share the same {@code World.shouldSuffocate} wrap on {@code
	 *       ChestBlock}; UniTweaks' {@code stackablechests.ChestBlockMixin} wraps the identical call
	 *       with no {@code require = 0} at all, so this pairing could already crash today independent
	 *       of the reported boat bug - not just theoretical.
	 *   <li>{@code mixin.client.render.BlockRenderManagerMixin} - fence lighting, torch bottom face
	 *       and the grass-top item tint (all UniTweaks) use explicit-ordinal {@code @WrapOperation}s
	 *       and injects at instructions that match UniTweaks' own fence/torch/grass mixins
	 *       instruction-for-instruction (its grass fix WRAPS the {@code renderTopFace} call this
	 *       file's tint injects at, so with {@code defaultRequire = 1} coexisting would crash).
	 *       The rotated-log renderer (MiscTweaks, "Log Rotation", no {@code source}) used to share
	 *       this file and stood down with it - which left {@code LogBlockMixin} still answering
	 *       render type 18 with nothing registered to draw it: every log invisible, x-ray through
	 *       the culled faces, and broken 2D item tiles. It now lives in its own
	 *       {@code mixin.client.render.RotatedLogRenderMixin}, which is NOT listed here because its
	 *       three injection points ({@code getRenderType} inside {@code render(Block;IF)V},
	 *       {@code isSideLit} HEAD, {@code render(Block;III)Z} TAIL) touch no instruction any
	 *       UniTweaks 0.29.0 BlockRenderManager mixin consumes.
	 * </ul>
	 */
	private static final Set<String> UNITWEAKS_SUPERSEDED_MIXINS = Set.of(
		// -- System / startup --------------------------------------------------------------------
		MIXIN_PACKAGE + "client.GameRendererPauseOnLostFocusMixin", // tweaks.nopauseonlostfocus
		MIXIN_PACKAGE + "world.WorldAutosaveMixin",                 // tweaks.autosaveinterval.WorldMixin
		MIXIN_PACKAGE + "client.MinecraftRawInputMixin",            // tweaks.rawinput.MinecraftMixin
		MIXIN_PACKAGE + "client.MinecraftControllerInitMixin",      // tweaks.disablecontrollerinit.MinecraftMixin
		MIXIN_PACKAGE + "server.DisabledDimensionsMixin",           // tweaks.disabledimensionload.MinecraftServerMixin

		// -- Interface / title & pause screens ----------------------------------------------------
		MIXIN_PACKAGE + "client.MinecraftAppletQuitMixin",   // tweaks.quitbutton.MinecraftAppletMixin
		MIXIN_PACKAGE + "client.gui.ImprovedControlsMixin",  // tweaks.improvedcontrols + options.OptionsScreenMixin (OptionsScreen overlap)
		MIXIN_PACKAGE + "client.hud.AchievementToastMixin",  // tweaks.hideachievementtoast.AchievementToastMixin
		MIXIN_PACKAGE + "client.gui.AchievementsScreenMixin", // tweaks.achievementbacktomenu.AchievementsScreenMixin
		MIXIN_PACKAGE + "client.frontview.FrontViewToggleMixin", // tweaks.frontviewthirdperson.MinecraftMixin
		MIXIN_PACKAGE + "client.frontview.FrontViewCameraMixin", // tweaks.frontviewthirdperson.GameRendererMixin
		MIXIN_PACKAGE + "client.frontview.FrontViewHudMixin",    // tweaks.frontviewthirdperson.InGameHudMixin
		MIXIN_PACKAGE + "client.frontview.FrontViewModelMixin",  // tweaks.frontviewthirdperson.EntityRenderDispatcherMixin

		// -- Video settings sliders (FOV, GUI scale, render distance, fog, brightness, clouds) ----
		MIXIN_PACKAGE + "client.option.OptionMixin",              // options.OptionMixin
		MIXIN_PACKAGE + "client.option.GameOptionsMixin",         // options.GameOptionsMixin / hooks.GameOptionsMixin
		MIXIN_PACKAGE + "client.gui.VideoOptionsScreenMixin",     // options.VideoOptionsScreenMixin
		MIXIN_PACKAGE + "client.ScreenScalerMixin",               // tweaks.guiscaleslider.ScreenScalerMixin
		MIXIN_PACKAGE + "client.render.WorldRendererMixin",       // tweaks.renderdistance.WorldRendererMixin
		MIXIN_PACKAGE + "client.render.VideoSettingsRendererMixin", // tweaks.renderdistance.GameRendererMixin
		MIXIN_PACKAGE + "world.DimensionBrightnessMixin",         // tweaks.brightness.DimensionMixin
		MIXIN_PACKAGE + "world.DimensionCloudHeightMixin",        // tweaks.cloudheight.WorldRendererMixin (Dimension overlap via cloud height)
		MIXIN_PACKAGE + "client.render.CloudsMixin",               // tweaks.cloudstoggle.WorldRendererMixin
		MIXIN_PACKAGE + "client.MinecraftZoomMixin",               // tweaks.fov.MinecraftMixin
		MIXIN_PACKAGE + "client.render.GameRendererFovMixin",      // tweaks.fov.GameRendererMixin
		MIXIN_PACKAGE + "client.render.FpsLimitMixin",             // tweaks.fpslimitslider.GameRendererMixin
		MIXIN_PACKAGE + "client.FpsLimitSyncMixin",                // tweaks.fpslimitslider.MinecraftMixin

		// -- Version text ------------------------------------------------------------------------
		MIXIN_PACKAGE + "client.hud.VersionTextMixin",        // tweaks.ingameversiontext.InGameHudMixin
		MIXIN_PACKAGE + "client.hud.VersionTextOverlayMixin", // tweaks.ingameversiontext.AchievementToastMixin

		// -- Debug overlay / HUD --------------------------------------------------------------------
		MIXIN_PACKAGE + "client.render.LivingEntityRendererMixin", // tweaks.disablef3entityids.LivingEntityRendererMixin

		// -- Gameplay ------------------------------------------------------------------------------
		MIXIN_PACKAGE + "client.InteractionManagerMixin",        // tweaks.shiftplacing.InteractionManagerMixin
		MIXIN_PACKAGE + "server.ShiftPlacingServerMixin",        // tweaks.shiftplacing.ServerPlayerInteractionManagerMixin
		MIXIN_PACKAGE + "item.ArmorItemMixin",                   // tweaks.rightclickarmor.ArmorItemMixin
		MIXIN_PACKAGE + "entity.PlayerInventoryPickBlockMixin",  // tweaks.pickblockfrominventory.PlayerInventoryMixin
		MIXIN_PACKAGE + "client.MinecraftPickBlockMixin",        // bugfixes.pickblockfix.MinecraftMixin (@ModifyVariable at the identical injection point; see Config.pickBlockFixes)
		MIXIN_PACKAGE + "entity.PlayerMovementMixin",            // tweaks.stepassist + tweaks.fencejump (both PlayerEntity)
		MIXIN_PACKAGE + "world.NaturalSpawnerMixin",             // tweaks.bedtweaks.class_567Mixin + bugfixes.nightmarepathfindingfix.NaturalSpawnerMixin

		// -- Old features (BetaTweaks/AnnoyanceFix behaviour also shipped by UniTweaks) ----------
		MIXIN_PACKAGE + "entity.LivingEntityLadderMixin", // tweaks.laddergaps.LivingEntityMixin
		MIXIN_PACKAGE + "entity.BoatEntityMixin",         // tweaks.boatelevators/boatsdontbreak/boatsdropthemselves + bugfixes.boatdismountfix (all BoatEntity)
		MIXIN_PACKAGE + "entity.MinecartBoosterMixin",    // tweaks.minecartboosters.MinecartEntityMixin
		MIXIN_PACKAGE + "item.HoeItemMixin",              // tweaks.hoegrassforseeds.HoeItemMixin
		MIXIN_PACKAGE + "block.TntBlockMixin",            // tweaks.punchtntignite.TntBlockMixin
		MIXIN_PACKAGE + "entity.TntEntityMixin",          // tweaks.punchtntdefuse.TntEntityMixin

		// -- Block placement/shape fixes ------------------------------------------------------------
		MIXIN_PACKAGE + "block.FenceBlockMixin",         // bugfixes.fenceboundingboxfix + tweaks.fenceplacement (both FenceBlock)
		MIXIN_PACKAGE + "block.PumpkinBlockMixin",       // tweaks.pumpkinplacement.PumpkinBlockMixin
		MIXIN_PACKAGE + "block.StairsBlockMixin",        // bugfixes.stairsdropfix.StairsBlockMixin
		MIXIN_PACKAGE + "block.TrapdoorBlockMixin",      // tweaks.trapdoorplacement.TrapdoorBlockMixin
		MIXIN_PACKAGE + "block.PressurePlateBlockMixin", // tweaks.pressureplatesonfences.PressurePlateBlockMixin
		MIXIN_PACKAGE + "block.SugarCaneBlockMixin",     // tweaks.sugarcaneonsand.SugarCaneBlockMixin
		MIXIN_PACKAGE + "block.ChestBlockMixin",         // tweaks.stackablechests.ChestBlockMixin
		MIXIN_PACKAGE + "block.BookshelfBlockMixin",     // tweaks.bookshelvesdropbooks.BookshelfBlockMixin

		// -- Liquids / fire --------------------------------------------------------------------------
		MIXIN_PACKAGE + "item.FlintAndSteelItemMixin",  // tweaks.igniteentities + tweaks.modernflintandsteel (both FlintAndSteel)
		MIXIN_PACKAGE + "block.FlowingLiquidBlockMixin", // bugfixes.lavawithoutsourcefix + liquidblockdropfix + springpropagationfix (all FlowingLiquidBlock)

		// -- Entities / mobs ---------------------------------------------------------------------
		MIXIN_PACKAGE + "entity.ChickenEntityMixin",     // tweaks.biggerchickenhitbox.ChickenEntityMixin
		MIXIN_PACKAGE + "entity.BetterBurningMixin",     // tweaks.betterburning.SkeletonEntityMixin
		MIXIN_PACKAGE + "entity.ArrowEntityBurningMixin", // tweaks.betterburning.ArrowEntityMixin
		MIXIN_PACKAGE + "entity.PlayerBurningSpreadMixin", // tweaks.betterburning.PlayerEntityMixin

		// -- Flora / world gen ---------------------------------------------------------------------
		MIXIN_PACKAGE + "world.feature.GrassPatchFeatureMixin",    // tweaks.disabletallgrass.GrassPatchFeatureMixin
		MIXIN_PACKAGE + "world.feature.DeadBushPatchFeatureMixin", // tweaks.disabledeadbush.DeadbushFeatureMixin
		MIXIN_PACKAGE + "block.BlockLeafLightingMixin",             // tweaks.beta18leavesrender.BlockMixin (Block overlap)

		// -- Bugfixes --------------------------------------------------------------------------------
		MIXIN_PACKAGE + "client.MinecraftBitDepthMixin",                   // bugfixes.bitdepthfix.MinecraftMixin
		MIXIN_PACKAGE + "client.render.ChunkRendererMixin",                // bugfixes.jitterfix.ChunkRendererMixin
		MIXIN_PACKAGE + "entity.SlimeEntityMixin",                         // bugfixes.slimefix.SlimeEntityMixin
		MIXIN_PACKAGE + "entity.EntityJitterMixin",                        // bugfixes.mpentityphysicsfix.EntityMixin
		MIXIN_PACKAGE + "client.render.GameRendererMixin",                 // bugfixes.sleepingheadturningfix.GameRendererMixin
		MIXIN_PACKAGE + "client.SingleplayerWoodenSlabMiningFixMixin",     // (SingleplayerInteractionManager - shares class w/ bugfixes.lastdurabilityfix)
		MIXIN_PACKAGE + "server.WoodenSlabMiningFixMixin",                 // (ServerPlayerInteractionManager - shares class w/ tweaks.shiftplacing)
		MIXIN_PACKAGE + "client.render.BowHeldFixPlayerEntityRendererMixin", // bugfixes.bowheldfix.PlayerEntityRendererMixin
		MIXIN_PACKAGE + "client.render.BowHeldFixSkeletonEntityRendererMixin", // bugfixes.bowheldfix.SkeletonEntityRendererMixin
		MIXIN_PACKAGE + "item.BowHeldFixBowItemMixin",                     // bugfixes.bowheldfix.BowItemMixin
		MIXIN_PACKAGE + "item.BowHeldFixItemMixin",                        // bugfixes.bowheldfix.ItemMixin
		MIXIN_PACKAGE + "client.gui.HandledScreenMixin",                   // bugfixes.containeritemrendering.HandledScreenMixin
		MIXIN_PACKAGE + "client.render.BreakingAnimationMixin",            // bugfixes.blockbreakinganimation.WorldRendererMixin
		MIXIN_PACKAGE + "client.gui.DeathScreenMixin",                     // bugfixes.deathscreenscorefix.DeathScreenMixin
		MIXIN_PACKAGE + "block.GrassBlockMixin",                           // bugfixes.grassblockitemfix.GrassBlockMixin
		MIXIN_PACKAGE + "client.MultiplayerMiningDelayFixMixin",           // bugfixes.miningdelayfix.MultiplayerInteractionManagerMixin
		MIXIN_PACKAGE + "client.SingleplayerLastDurabilityFixMixin",       // bugfixes.lastdurabilityfix.SingleplayerInteractionManagerMixin
		MIXIN_PACKAGE + "server.LastDurabilityFixMixin",                   // bugfixes.lastdurabilityfix.ServerPlayerInteractionManagerMixin
		MIXIN_PACKAGE + "client.render.BlockRenderManagerMixin",           // bugfixes.fencelightingfix + torchbottomfix + grassblockitemfix (all BlockRenderManager; RotatedLogRenderMixin deliberately absent - see class doc)

		// -- Sounds / networking -------------------------------------------------------------------
		MIXIN_PACKAGE + "client.ChestSoundMixin", // tweaks.moresounds.ClientPlayerEntityMixin
		MIXIN_PACKAGE + "item.MoreSoundsMixin",   // tweaks.moresounds.ItemStackMixin
		MIXIN_PACKAGE + "network.ConnectionMixin" // tweaks.tcpnodelay.ConnectionMixin
	);

	@Override
	public void onLoad(String mixinPackage) {
		// The config has to exist before any gated mixin is asked about; loading it here rather
		// than in the entrypoint means it is ready even for mixins applied during pre-launch.
		Config.class.getName();
		// Cancels the few OTHER-mod mixins RetroTweaks cannot coexist with - see its class doc for
		// the (deliberately high) bar an entry has to clear.
		com.periut.retrotweaks.compat.MixinVeto.register();
	}

	/**
	 * Mixins that cannot coexist with StationAPI's own renderer, because it does not merely inject into
	 * the method they target - it REPLACES it.
	 *
	 * <p>{@code stationapi.mixin.arsenic.client.ItemRendererMixin} merges {@code ItemRenderer
	 * .renderGuiItem}, and Mixin refuses to inject into a method another mixin has replaced at equal
	 * priority: it throws {@code InvalidInjectionException} during apply, which is a hard crash at class
	 * load rather than a feature quietly not working. {@code require = 0} does not help - the refusal
	 * happens while locating the target, before any match is counted.
	 *
	 * <p>Skipping is the honest answer rather than out-prioritising it. StationAPI's replacement IS the
	 * item renderer on that install - it draws from JSON models through its own pipeline - so an
	 * injection into vanilla's version would be patching code that no longer runs even if Mixin allowed
	 * it. Tinting an item sprite there is StationAPI's own colour-provider API's job, and RetroTweaks
	 * hard-depends on nothing.
	 *
	 * <p>This is the complete set as of StationAPI 2.0.0-alpha.6.2: every {@code @Overwrite} across all
	 * 43 of its modules was enumerated (eleven of them) and checked against every RetroTweaks mixin
	 * target. Only {@code ItemRenderer} collides - its {@code renderItem}, {@code renderGuiItem} and
	 * {@code drawTexture} are all replaced. {@code BlockRenderManager} is safe: StationAPI only injects
	 * there, never replaces, which is why the grass-block work next door still applies.
	 */
	private static final Set<String> STATIONAPI_SUPERSEDED_MIXINS = Set.of(
		// arsenic.client.ItemRendererMixin @Overwrites renderGuiItem...
		MIXIN_PACKAGE + "client.render.ItemSpriteTintMixin",
		// ...and the dropped/pickup sprite tint, which is a separate class precisely so that ONLY the
		// UniTweaks skip list can leave it running (see its class doc) - but arsenic's @Overwrite
		// still bars any injection into that method, so under StationAPI it stands down with the rest
		// and StationApiItemColors carries the tint instead.
		MIXIN_PACKAGE + "client.render.DroppedItemTintMixin",
		// arsenic.client.class_556Mixin @Overwrites HeldItemRenderer.renderItem the same way.
		MIXIN_PACKAGE + "client.render.HeldItemSpriteTintMixin"
	);

	/**
	 * Nothing here targets a StationAPI class any more. What used to - the arsenic replacement for the
	 * dropped/pickup sprite tint stood down just above - now lives in the {@code retroapi-stationapi}
	 * module as {@code ArsenicDroppedItemTintMixin}, whose whole mod is only loaded when StationAPI is
	 * installed. That is a better gate than a check here AND it fixes the tint on Ornithe: naming
	 * arsenic by string meant naming its two Minecraft members by hand-written intermediary, and a
	 * hand-written intermediary name is only ever right for one of the two toolchains RetroAPI ships
	 * for. See that class's own doc.
	 */

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (!FISHING_ENABLED && FISHING_MIXINS.contains(mixinClassName)) {
			return false;
		}
		if (STATIONAPI_SUPERSEDED_MIXINS.contains(mixinClassName)) {
			return !Mods.HAS_STATIONAPI;
		}
		if (mixinClassName.startsWith(AUTH_PACKAGE)) {
			return !Mods.HAS_RETROAUTH;
		}
		if (UNITWEAKS_SUPERSEDED_MIXINS.contains(mixinClassName)) {
			return !Mods.HAS_UNITWEAKS;
		}
		return true;
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
