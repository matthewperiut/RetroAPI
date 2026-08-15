/*
 * Ported from HudTweaks by Telvarost. The layout maths and every injection point are unchanged;
 * only the config references and the chat-scrolling parts differ. Chat scrolling and chat history
 * size were removed on purpose - see TEXT_FEATURES.txt.
 */
package com.periut.retrotweaks.mixin.client.hud;


import com.periut.retrotweaks.client.gui.ConfigScreen;
import com.periut.retrotweaks.config.Config;
import com.periut.retrotweaks.config.Enums;
import com.periut.retrotweaks.feature.hud.HudLayout;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.client.util.ScreenScaler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin extends DrawContext {

    @Shadow private Minecraft minecraft;
    @Shadow private List messages;
    @Shadow private Random random;
    @Shadow private int overlayRemaining;
    @Shadow private String overlayMessage;

    @Shadow protected abstract void renderHotbarItem(int slot, int x, int y, float f);
    @Unique private int prevSelectedSlot = 0;
    @Unique private int scaledWidth    = 0;
    @Unique private int scaledHeight   = 0;
    @Unique private boolean isHotbarRaised = false;
    @Unique private boolean hotbarVisibility  = true;
    @Unique private int xOffsetHotbar  = 0;
    @Unique private int yOffsetHotbar  = 0;
    @Unique private boolean heartsVisibility  = true;
    @Unique private int xOffsetHearts  = 0;
    @Unique private int yOffsetHearts  = 0;
    @Unique private boolean armorVisibility   = true;
    @Unique private int xOffsetArmor   = 0;
    @Unique private int yOffsetArmor   = 0;
    @Unique private boolean oxygenVisibility  = true;
    @Unique private int xOffsetOxygen  = 0;
    @Unique private int yOffsetOxygen  = 0;
    @Unique private boolean overlayVisibility = true;
    @Unique private int xOffsetOverlay = 0;
    @Unique private int yOffsetOverlay = 0;

    // ---- live HUD-position preview backdrop, see retrotweaks$renderPositionPreview below ----
    /** Matches the vanilla hotbar's own 20px slot pitch, so grid lines land on real HUD units. */
    @Unique private static final int PREVIEW_GRID_SPACING = 20;
    /** Grey, ~50% opaque - dims the world without hiding it. */
    @Unique private static final int PREVIEW_DIM_COLOR = 0x80202020;
    /** Faint, so a 20px grid stays a guide rather than noise. */
    @Unique private static final int PREVIEW_GRID_COLOR = 0x28FFFFFF;
    /** Brighter amber - the centre lines and the four edges, the six anchors every element's
     *  HorizontalPosition (LEFT/CENTERED/RIGHT) and VerticalPosition (TOP/CENTERED/BOTTOM) can target. */
    @Unique private static final int PREVIEW_AXIS_COLOR = 0x90FFD040;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void retrotweaks$render(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
        ScreenScaler screenScaler = new ScreenScaler(this.minecraft.options, this.minecraft.displayWidth, this.minecraft.displayHeight);

        if (  (scaledWidth != screenScaler.getScaledWidth())
           || (scaledHeight != screenScaler.getScaledHeight())
           || (HudLayout.configUpdated)
           )
        {
            scaledWidth = screenScaler.getScaledWidth();
            scaledHeight = screenScaler.getScaledHeight();
            HudLayout.configUpdated = false;

            if (Enums.HudPositioning.SIMPLE == Config.HUD.positions.hudPositioningSystem) {
                if (Config.HUD.positions.simple.visible) {
                    int xOffset = 0;
                    int yOffset = 0;

                    if (Enums.HorizontalPosition.LEFT == Config.HUD.positions.simple.horizontal) {
                        xOffset = 91 - (scaledWidth / 2);
                    } else if (Enums.HorizontalPosition.RIGHT == Config.HUD.positions.simple.horizontal) {
                        xOffset = -91 + (scaledWidth / 2) + (scaledWidth % 2);
                    }

                    if (Enums.VerticalPosition.TOP == Config.HUD.positions.simple.vertical) {
                        yOffset = 22 - (scaledHeight);
                    } else if (Enums.VerticalPosition.CENTERED == Config.HUD.positions.simple.vertical) {
                        yOffset = 11 - (scaledHeight / 2) - (scaledHeight % 2);
                    }

                    xOffset += Config.HUD.positions.simple.horizontalOffset;
                    yOffset += Config.HUD.positions.simple.verticalOffset;

                    xOffsetHotbar  = xOffset;
                    yOffsetHotbar  = yOffset;
                    xOffsetHearts  = xOffset;
                    yOffsetHearts  = yOffset;
                    xOffsetArmor   = xOffset;
                    yOffsetArmor   = yOffset;
                    xOffsetOxygen  = xOffset;
                    yOffsetOxygen  = yOffset;
                    xOffsetOverlay = xOffset;
                    yOffsetOverlay = yOffset;

                    if (  (Enums.VerticalPosition.TOP == Config.HUD.positions.simple.vertical)
                       || (Enums.VerticalPosition.CENTERED == Config.HUD.positions.simple.vertical)
                       || (0 > yOffsetHotbar)
                    ) {
                        isHotbarRaised = true;
                    } else {
                        isHotbarRaised = false;
                    }

                    if (Config.HUD.positions.putStatusBarIconsBelowHotbar) {
                        yOffsetHearts  += 33;
                        yOffsetArmor   += 33;
                        yOffsetOxygen  += 51;
                    }

                    if (Config.HUD.positions.putOverlayMessagesBelowHotbar) {
                        yOffsetOverlay += 74;
                    }

                    hotbarVisibility  = true;
                    heartsVisibility  = true;
                    armorVisibility   = true;
                    oxygenVisibility  = true;
                    overlayVisibility = true;
                } else {
                    hotbarVisibility  = false;
                    heartsVisibility  = false;
                    armorVisibility   = false;
                    oxygenVisibility  = false;
                    overlayVisibility = false;

                }
            } else if (Enums.HudPositioning.ADVANCED == Config.HUD.positions.hudPositioningSystem) {
                xOffsetHotbar  = 0;
                yOffsetHotbar  = 0;
                xOffsetHearts  = 0;
                yOffsetHearts  = 0;
                xOffsetArmor   = 0;
                yOffsetArmor   = 0;
                xOffsetOxygen  = 0;
                yOffsetOxygen  = 0;
                xOffsetOverlay = 0;
                yOffsetOverlay = 0;

                if (Config.HUD.positions.hotbar.visible) {
                    if (Enums.HorizontalPosition.LEFT == Config.HUD.positions.hotbar.horizontal) {
                        xOffsetHotbar = 91 - (scaledWidth / 2);
                    } else if (Enums.HorizontalPosition.RIGHT == Config.HUD.positions.hotbar.horizontal) {
                        xOffsetHotbar = -91 + (scaledWidth / 2) + (scaledWidth % 2);
                    }

                    if (Enums.VerticalPosition.TOP == Config.HUD.positions.hotbar.vertical) {
                        yOffsetHotbar = 22 - (scaledHeight);
                    } else if (Enums.VerticalPosition.CENTERED == Config.HUD.positions.hotbar.vertical) {
                        yOffsetHotbar = 11 - (scaledHeight / 2) - (scaledHeight % 2);
                    }

                    xOffsetHotbar += Config.HUD.positions.hotbar.horizontalOffset;
                    yOffsetHotbar += Config.HUD.positions.hotbar.verticalOffset;

                    if (  (Enums.VerticalPosition.TOP == Config.HUD.positions.hotbar.vertical)
                       || (Enums.VerticalPosition.CENTERED == Config.HUD.positions.hotbar.vertical)
                       || (0 > yOffsetHotbar)
                    ) {
                        isHotbarRaised = true;
                    } else {
                        isHotbarRaised = false;
                    }

                    hotbarVisibility  = true;
                } else {
                    hotbarVisibility  = false;
                }

                if (Config.HUD.positions.hearts.visible) {
                    if (Enums.HorizontalPosition.LEFT == Config.HUD.positions.hearts.horizontal) {
                        xOffsetHearts = 91 - (scaledWidth / 2);
                    } else if (Enums.HorizontalPosition.RIGHT == Config.HUD.positions.hearts.horizontal) {
                        xOffsetHearts = -91 + (scaledWidth / 2) + (scaledWidth % 2);
                    }

                    if (Enums.VerticalPosition.TOP == Config.HUD.positions.hearts.vertical) {
                        yOffsetHearts = 22 - (scaledHeight);
                    } else if (Enums.VerticalPosition.CENTERED == Config.HUD.positions.hearts.vertical) {
                        yOffsetHearts = 11 - (scaledHeight / 2) - (scaledHeight % 2);
                    }

                    xOffsetHearts += Config.HUD.positions.hearts.horizontalOffset;
                    yOffsetHearts += Config.HUD.positions.hearts.verticalOffset;

                    if (Config.HUD.positions.putStatusBarIconsBelowHotbar) {
                        yOffsetHearts += 33;
                    }

                    heartsVisibility  = true;
                } else {
                    heartsVisibility  = false;
                }

                if (Config.HUD.positions.armor.visible) {
                    if (Enums.HorizontalPosition.LEFT == Config.HUD.positions.armor.horizontal) {
                        xOffsetArmor = 91 - (scaledWidth / 2);
                    } else if (Enums.HorizontalPosition.RIGHT == Config.HUD.positions.armor.horizontal) {
                        xOffsetArmor = -91 + (scaledWidth / 2) + (scaledWidth % 2);
                    }

                    if (Enums.VerticalPosition.TOP == Config.HUD.positions.armor.vertical) {
                        yOffsetArmor = 22 - (scaledHeight);
                    } else if (Enums.VerticalPosition.CENTERED == Config.HUD.positions.armor.vertical) {
                        yOffsetArmor = 11 - (scaledHeight / 2) - (scaledHeight % 2);
                    }

                    xOffsetArmor += Config.HUD.positions.armor.horizontalOffset;
                    yOffsetArmor += Config.HUD.positions.armor.verticalOffset;

                    if (Config.HUD.positions.putStatusBarIconsBelowHotbar) {
                        yOffsetArmor += 33;
                    }

                    armorVisibility  = true;
                } else {
                    armorVisibility  = false;
                }

                if (Config.HUD.positions.oxygen.visible) {
                    if (Enums.HorizontalPosition.LEFT == Config.HUD.positions.oxygen.horizontal) {
                        xOffsetOxygen = 91 - (scaledWidth / 2);
                    } else if (Enums.HorizontalPosition.RIGHT == Config.HUD.positions.oxygen.horizontal) {
                        xOffsetOxygen = -91 + (scaledWidth / 2) + (scaledWidth % 2);
                    }

                    if (Enums.VerticalPosition.TOP == Config.HUD.positions.oxygen.vertical) {
                        yOffsetOxygen = 22 - (scaledHeight);
                    } else if (Enums.VerticalPosition.CENTERED == Config.HUD.positions.oxygen.vertical) {
                        yOffsetOxygen = 11 - (scaledHeight / 2) - (scaledHeight % 2);
                    }

                    xOffsetOxygen += Config.HUD.positions.oxygen.horizontalOffset;
                    yOffsetOxygen += Config.HUD.positions.oxygen.verticalOffset;

                    if (Config.HUD.positions.putStatusBarIconsBelowHotbar) {
                        yOffsetOxygen += 51;
                    }

                    oxygenVisibility  = true;
                } else {
                    oxygenVisibility  = false;
                }

                if (Config.HUD.positions.overlayMessage.visible) {
                    if (Enums.HorizontalPosition.LEFT == Config.HUD.positions.overlayMessage.horizontal) {
                        xOffsetOverlay = 91 - (scaledWidth / 2);
                    } else if (Enums.HorizontalPosition.RIGHT == Config.HUD.positions.overlayMessage.horizontal) {
                        xOffsetOverlay = -91 + (scaledWidth / 2) + (scaledWidth % 2);
                    }

                    if (Enums.VerticalPosition.TOP == Config.HUD.positions.overlayMessage.vertical) {
                        yOffsetOverlay = 22 - (scaledHeight);
                    } else if (Enums.VerticalPosition.CENTERED == Config.HUD.positions.overlayMessage.vertical) {
                        yOffsetOverlay = 11 - (scaledHeight / 2) - (scaledHeight % 2);
                    }

                    xOffsetOverlay += Config.HUD.positions.overlayMessage.horizontalOffset;
                    yOffsetOverlay += Config.HUD.positions.overlayMessage.verticalOffset;

                    if (Config.HUD.positions.putOverlayMessagesBelowHotbar) {
                        yOffsetOverlay += 74;
                    }

                    overlayVisibility  = true;
                } else {
                    overlayVisibility  = false;
                }

            } else {
                // Enums.HudPositioning.DISABLED
                isHotbarRaised = false;
                xOffsetHotbar  = 0;
                yOffsetHotbar  = 0;
                xOffsetHearts  = 0;
                yOffsetHearts  = 0;
                xOffsetArmor   = 0;
                yOffsetArmor   = 0;
                xOffsetOxygen  = 0;
                yOffsetOxygen  = 0;
                xOffsetOverlay = 0;
                yOffsetOverlay = 0;
                hotbarVisibility  = true;
                heartsVisibility  = true;
                armorVisibility   = true;
                oxygenVisibility  = true;
                overlayVisibility = true;
            }

            // Published for the HUD-position editor (ConfigScreen's drag-to-move mode, see
            // HudEditorMath): a copy of exactly what was just computed above, not a second opinion.
            // Never read back by this method - see HudLayout's own class doc.
            HudLayout.scaledWidth    = scaledWidth;
            HudLayout.scaledHeight   = scaledHeight;
            HudLayout.hotbarVisible  = hotbarVisibility;
            HudLayout.heartsVisible  = heartsVisibility;
            HudLayout.armorVisible   = armorVisibility;
            HudLayout.oxygenVisible  = oxygenVisibility;
            HudLayout.overlayVisible = overlayVisibility;
            HudLayout.xOffsetHotbar  = xOffsetHotbar;
            HudLayout.yOffsetHotbar  = yOffsetHotbar;
            HudLayout.xOffsetHearts  = xOffsetHearts;
            HudLayout.yOffsetHearts  = yOffsetHearts;
            HudLayout.xOffsetArmor   = xOffsetArmor;
            HudLayout.yOffsetArmor   = yOffsetArmor;
            HudLayout.xOffsetOxygen  = xOffsetOxygen;
            HudLayout.yOffsetOxygen  = yOffsetOxygen;
            HudLayout.xOffsetOverlay = xOffsetOverlay;
            HudLayout.yOffsetOverlay = yOffsetOverlay;
        }
    }

    /**
     * Live HUD-position preview: a dim + alignment grid drawn from the HUD side, before any HUD
     * element, so the real hotbar/hearts/armor/oxygen/overlay draw on top of it at their real (already
     * gate-correct) offsets - see {@link Config.HudPositions}. There is nothing to fake here: the HUD
     * this class draws every frame already IS the preview, at the group {@code hudPositioningSystem}
     * actually reads, so this only ever adds a backdrop and never touches the offsets above.
     *
     * <p>Engages only while {@link Minecraft#currentScreen} is the "HUD Positions" page or one of its
     * six element subsections ({@link ConfigScreen#isHudPositionsScreen()}); every other screen, and
     * the case with no screen open at all, leaves the HUD untouched.
     *
     * <p>Injected right after {@code GameRenderer.setupHudRender()} rather than at this method's own
     * {@code HEAD}: that call is what sets up the 2D ortho projection {@code fill}/{@code
     * drawHorizontalLine}/{@code drawVerticalLine} (inherited from {@link DrawContext}) assume: drawing
     * any earlier would push these quads through the still-active 3D camera matrix from the world pass
     * that just ran, landing them nowhere near the screen. Depth test is off for the same reason
     * {@code InGameHud.renderVignette} turns it off right after this same call: a quad with no
     * zOffset drawn while depth test is on would win against the hotbar's own zOffset=-90 background
     * and hide it, instead of sitting behind it as this is meant to.
     */
    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;setupHudRender()V",
                    shift = At.Shift.AFTER
            )
    )
    private void retrotweaks$renderPositionPreview(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
        if (!screenOpen) return;
        if (!(this.minecraft.currentScreen instanceof ConfigScreen configScreen) || !configScreen.isHudPositionsScreen()) return;

        ScreenScaler screenScaler = new ScreenScaler(this.minecraft.options, this.minecraft.displayWidth, this.minecraft.displayHeight);
        int width = screenScaler.getScaledWidth();
        int height = screenScaler.getScaledHeight();

        // Touched: depth test + depth mask (restored below), colour and blend/texture (already
        // restored per-call by fill()/drawHorizontalLine()/drawVerticalLine(), and reset to opaque
        // white afterwards regardless, since that is what every vanilla HUD draw right after this
        // expects to find). Texture binding is never touched - none of these calls bind one.
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        this.fill(0, 0, width, height, PREVIEW_DIM_COLOR);

        for (int x = PREVIEW_GRID_SPACING; x < width; x += PREVIEW_GRID_SPACING) {
            this.drawVerticalLine(x, 0, height, PREVIEW_GRID_COLOR);
        }
        for (int y = PREVIEW_GRID_SPACING; y < height; y += PREVIEW_GRID_SPACING) {
            this.drawHorizontalLine(0, width, y, PREVIEW_GRID_COLOR);
        }

        // Centre lines and screen edges, marked distinctly: every HorizontalPosition/VerticalPosition
        // an element can anchor to (LEFT/CENTERED/RIGHT, TOP/CENTERED/BOTTOM) lands on one of these six.
        this.drawVerticalLine(width / 2, 0, height, PREVIEW_AXIS_COLOR);
        this.drawHorizontalLine(0, width, height / 2, PREVIEW_AXIS_COLOR);
        this.drawHorizontalLine(0, width, 0, PREVIEW_AXIS_COLOR);
        this.drawHorizontalLine(0, width, height - 1, PREVIEW_AXIS_COLOR);
        this.drawVerticalLine(0, 0, height, PREVIEW_AXIS_COLOR);
        this.drawVerticalLine(width - 1, 0, height, PREVIEW_AXIS_COLOR);

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;isFancyGraphicsEnabled()Z"
            )
    )
    private boolean retrotweaks$renderVignette(Operation<Boolean> original) {
        if (Config.HUD.disableVignette) {
            return false;
        } else {
            return original.call();
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 0
            ), require = 0)
    private void retrotweaks$renderHotbar(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (hotbarVisibility) {
            x += xOffsetHotbar;
            y += yOffsetHotbar;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 1
            ), require = 0)
    private void retrotweaks$renderSelectedItemBorder(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (hotbarVisibility) {
            x += xOffsetHotbar;
            y += yOffsetHotbar;

            if (isHotbarRaised) {
                height += 2;
            }

            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 3
            ), require = 0)
    private void retrotweaks$renderArmor1(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (armorVisibility) {
            x += xOffsetArmor;
            y += yOffsetArmor;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 4
            ), require = 0)
    private void retrotweaks$renderArmor2(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (armorVisibility) {
            x += xOffsetArmor;
            y += yOffsetArmor;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 5
            ), require = 0)
    private void retrotweaks$renderArmor3(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (armorVisibility) {
            x += xOffsetArmor;
            y += yOffsetArmor;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 6
            ), require = 0)
    private void retrotweaks$renderHearts1(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 7
            ), require = 0)
    private void retrotweaks$renderHearts2(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 8
            ), require = 0)
    private void retrotweaks$renderHearts3(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 9
            ), require = 0)
    private void retrotweaks$renderHearts4(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 10
            ), require = 0)
    private void retrotweaks$renderHearts5(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (heartsVisibility) {
            x += xOffsetHearts;
            y += yOffsetHearts;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 11
            ), require = 0)
    private void retrotweaks$renderOxygen1(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (oxygenVisibility) {
            x += xOffsetOxygen;
            y += yOffsetOxygen;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTexture(IIIIII)V",
                    ordinal = 12
            ), require = 0)
    private void retrotweaks$renderOxygen2(InGameHud instance, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (oxygenVisibility) {
            x += xOffsetOxygen;
            y += yOffsetOxygen;
            original.call(instance, x, y, u, v, width, height);
        }
    }

    @ModifyConstant(
            method = "render",
            constant = @Constant(intValue = 200), require = 0)
    private int retrotweaks$renderChatFadeTime(int value) {
        return (Config.HUD.chatFadeTime * 2);
    }

    @ModifyConstant(
            method = "render",
            constant = @Constant(doubleValue = 200.0), require = 0)
    private double retrotweaks$renderChatFadeTimeDivisor(double value) {
        return (Config.HUD.chatFadeTime * 2);
    }

    // The crosshair draw (drawTexture ordinal 2) lives in CrosshairRenderMixin, not here: it needs
    // priority 1100 to sit outside UniTweaks' wrap of the same call - see that class's doc.

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
                    ordinal = 2
            ), require = 0)
    public void retrotweaks$renderDrawCoordinate_X(InGameHud instance, TextRenderer textRenderer, String s, int i, int j, int k, Operation<Void> original) {
        if (Enums.CoordinateDisplay.HIDE == Config.HUD.debug.coordinateDisplay) {
            original.call(instance, textRenderer, "", i, j, k);
        } else if (Enums.CoordinateDisplay.RANDOMIZE == Config.HUD.debug.coordinateDisplay) {
            double randomVal = random.nextDouble(-10000, 10000);
            original.call(instance, textRenderer, "x: " + randomVal, i, j, k);
        } else {
            original.call(instance, textRenderer, s, i, j, k);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
                    ordinal = 3
            ), require = 0)
    public void retrotweaks$renderDrawCoordinate_Y(InGameHud instance, TextRenderer textRenderer, String s, int i, int j, int k, Operation<Void> original) {
        if (Enums.CoordinateDisplay.HIDE == Config.HUD.debug.coordinateDisplay) {
            original.call(instance, textRenderer, "", i, j, k);
        } else if (Enums.CoordinateDisplay.RANDOMIZE == Config.HUD.debug.coordinateDisplay) {
            double randomVal = random.nextDouble(-10000, 10000);
            original.call(instance, textRenderer, "y: " + randomVal, i, j, k);
        } else {
            original.call(instance, textRenderer, s, i, j, k);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V",
                    ordinal = 4
            ), require = 0)
    public void retrotweaks$renderDrawCoordinate_Z(InGameHud instance, TextRenderer textRenderer, String s, int i, int j, int k, Operation<Void> original) {
        if (Enums.CoordinateDisplay.HIDE == Config.HUD.debug.coordinateDisplay) {
            original.call(instance, textRenderer, "", i, j, k);
        } else if (Enums.CoordinateDisplay.RANDOMIZE == Config.HUD.debug.coordinateDisplay) {
            double randomVal = random.nextDouble(-10000, 10000);
            original.call(instance, textRenderer, "z: " + randomVal, i, j, k);
        } else {
            original.call(instance, textRenderer, s, i, j, k);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(IIIF)V"
            )
    )
    public void retrotweaks$renderHotbarItem(InGameHud instance, int slot, int x, int y, float f, Operation<Void> original) {
        if (hotbarVisibility) {
            x += xOffsetHotbar;
            y += yOffsetHotbar;
            original.call(instance, slot, x, y, f);
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/platform/Lighting;turnOff()V"
            )
    )
    public void retrotweaks$glClearAndItemSelectedTooltip(Operation<Void> original) {
        original.call();

        if (Config.HUD.enableHotbarBlockRenderingFix) {
            GL11.glClear(256);
        }

        if (Config.HUD.enableHotbarItemSelectionTooltips) {
            PlayerInventory playerInventory = this.minecraft.player.inventory;
            if (prevSelectedSlot != playerInventory.selectedSlot) {
                prevSelectedSlot = playerInventory.selectedSlot;
                ItemStack selectedItemStack = playerInventory.getSelectedItem();
                if (null != selectedItemStack) {
                    TranslationStorage translationStorage = TranslationStorage.getInstance();
                    this.overlayMessage = translationStorage.get(selectedItemStack.getTranslationKey() + ".name");
                    this.overlayRemaining = Config.HUD.hotbarItemSelectionFadeTime;
                }
            }
        }
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/opengl/GL11;glTranslatef(FFF)V",
                    ordinal = 1
            ), require = 0)
    public void retrotweaks$overlayMesssagePosition(float x, float y, float z, Operation<Void> original) {
        x += xOffsetOverlay;
        y += yOffsetOverlay;
        original.call(x, y, z);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)V"
            )
    )
    public void retrotweaks$overlayMesssagePosition(TextRenderer instance, String text, int x, int y, int color, Operation<Void> original) {
        if (overlayVisibility) {
            original.call(instance, text, x, y, color);
        }
    }



    @Inject(method = "render", at = @At("RETURN"), cancellable = true)
    public void retrotweaks$renderXboxButtons(float f, boolean bl, int i, int j, CallbackInfo ci) {
        if (Config.HUD.drawXboxXAndYButtons) {
            ScreenScaler var5 = new ScreenScaler(this.minecraft.options, this.minecraft.displayWidth, this.minecraft.displayHeight);
            //int var6 = var5.method_1857();
            int var7 = var5.getScaledHeight();
            GL11.glBindTexture(3553 /* GL_TEXTURE_2D */, minecraft.textureManager.getTextureId("/assets/retrotweaks/button_icons.png"));
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexture(10, var7 - 30, (2 % 12) * 20, (2 / 12) * 20, 20, 20);
            TextRenderer var8 = this.minecraft.textRenderer;
            var8.drawWithShadow("Crafting", 31, var7 - 24, 16777215);
            GL11.glBindTexture(3553 /* GL_TEXTURE_2D */, minecraft.textureManager.getTextureId("/assets/retrotweaks/button_icons.png"));
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            drawTexture(78, var7 - 30, (3 % 12) * 20, (3 / 12) * 20, 20, 20);
            var8.drawWithShadow("Inventory", 99, var7 - 24, 16777215);
        }
    }
}
