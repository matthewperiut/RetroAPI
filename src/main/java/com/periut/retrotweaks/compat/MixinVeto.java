package com.periut.retrotweaks.compat;

import com.periut.retrotweaks.RetroTweaks;

import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.SortedSet;
import org.objectweb.asm.tree.ClassNode;

/**
 * Says NO to specific mixins from OTHER mods - the one thing {@code shouldApplyMixin} can never do,
 * because a mixin plugin is only ever asked about its own config's mixins.
 *
 * <p>How: an {@link IExtension} registered into Mixin's active transformer. Extensions see every
 * target class just before its mixins apply, and the {@code TargetClassContext} they are handed
 * carries the {@code SortedSet<IMixinInfo>} about to be applied; removing an entry there is a clean
 * cancellation - the mixin simply never applies to that class, with no error, exactly as if its own
 * plugin had said no. This is the same mechanism the MixinSquared library uses for its
 * {@code MixinCanceller} API, inlined here (one reflected field) rather than taken as a dependency.
 *
 * <p>Every veto MUST be a last resort and MUST document why standing down our own side (the usual
 * answer, see {@code RetroTweaksMixinPlugin}) cannot work. Vetoing rewrites another mod's behaviour
 * without its knowledge; the bar is "their mixin breaks the game and only one side can win".
 *
 * <p>Registration is attempted from {@code RetroTweaksMixinPlugin.onLoad} (earliest) and again from
 * {@code RetroTweaks.onInitialize} (belt and braces, in case the transformer was not live yet at
 * plugin load); both funnel through {@link #register()}, which is idempotent. Failure is logged and
 * otherwise harmless - the vetoed mixins then simply apply, which is the status quo ante.
 */
public final class MixinVeto implements IExtension {

	private MixinVeto() {}

	/**
	 * Fully-qualified mixin class names to cancel, and why.
	 *
	 * <p>UniTweaks {@code tweaks.resourceurl.ResourceDownloadThreadMixin}: both mods redirect the dead
	 * vanilla sound-resource URL in {@code ResourceDownloadThread.run}, and the two handlers stack so
	 * that UniTweaks' runs LAST and overrides RetroTweaks' with its own config value unless that value
	 * is empty. RetroTweaks' "Resource URL" options therefore silently did nothing with UniTweaks
	 * installed, and a stale or truncated URL in UniTweaks' YAML (its 0.17 releases wrote one - see
	 * DanyGames2014/UniTweaks#26) silences every sound in the game even though the same install works
	 * with RetroTweaks alone. Cancelling it makes the combo's download path byte-identical to
	 * RetroTweaks alone; UniTweaks' own screen still shows the value RetroTweaks drives, because
	 * {@code UniTweaksBridge.pushResourceUrl} keeps its config mirrored.
	 */
	private static final Set<String> VETOED = Set.of(
		"net.danygames2014.unitweaks.mixin.tweaks.resourceurl.ResourceDownloadThreadMixin"
	);

	private static boolean registered = false;

	/** Installs the veto extension. Safe to call more than once; a no-op without UniTweaks. */
	public static void register() {
		if (registered || !Mods.HAS_UNITWEAKS) return;
		try {
			IMixinTransformer transformer =
				(IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer();
			if (transformer == null) return; // too early; the onInitialize retry will land
			((Extensions) transformer.getExtensions()).add(new MixinVeto());
			registered = true;
		} catch (Throwable t) {
			// A Mixin internals change lands here. The vetoed mixins then apply as they always did -
			// worse behaviour, but nothing new broken - so log loudly and move on.
			RetroTweaks.LOGGER.warn("Could not install the mixin veto - vetoed mixins will apply", t);
			registered = true; // do not retry into the same failure from the second call site
		}
	}

	/** {@code TargetClassContext.mixins}, the set of mixins about to be applied. Resolved lazily. */
	private Field mixinsField;

	@Override
	public boolean checkActive(MixinEnvironment environment) {
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void preApply(ITargetClassContext context) {
		// The field lives on the concrete TargetClassContext, not the interface; anything else
		// (another implementation, a future rename) just means no veto for that class.
		if (!"org.spongepowered.asm.mixin.transformer.TargetClassContext".equals(context.getClass().getName())) return;
		try {
			if (mixinsField == null) {
				mixinsField = context.getClass().getDeclaredField("mixins");
				mixinsField.setAccessible(true);
			}
			SortedSet<IMixinInfo> mixins = (SortedSet<IMixinInfo>) mixinsField.get(context);
			mixins.removeIf(mixin -> {
				if (!VETOED.contains(mixin.getClassName())) return false;
				RetroTweaks.LOGGER.debug("Vetoed mixin {} - see MixinVeto for why", mixin.getClassName());
				return true;
			});
		} catch (ReflectiveOperationException | RuntimeException e) {
			RetroTweaks.LOGGER.warn("Mixin veto could not inspect {}", context, e);
		}
	}

	@Override
	public void postApply(ITargetClassContext context) {}

	@Override
	public void export(MixinEnvironment env, String name, boolean force, ClassNode classNode) {}
}
