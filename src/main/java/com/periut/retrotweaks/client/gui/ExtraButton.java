package com.periut.retrotweaks.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.function.Consumer;

/**
 * A button that carries its own click handler, for adding rows to vanilla screens.
 *
 * <p>b1.7.3 routes clicks through {@code Screen.buttonClicked(ButtonWidget)} matching on an int id,
 * so every mod-added button normally needs a mixin on that method plus an id nobody else picked.
 * RetroTweaks adds buttons to several vanilla screens, so instead the button runs its own action from
 * {@code isMouseOver} - the same hook vanilla's own {@code SliderWidget} acts on - and a mixin only
 * has to build the widget:
 *
 * <pre>{@code
 * this.buttons.add(new ExtraButton(x, y, 200, 20, "RetroTweaks...", b -> minecraft.setScreen(new ConfigScreen(screen))));
 * }</pre>
 *
 * <p>Ids start at {@link #ID_BASE} so that a screen's own {@code buttonClicked} - which tests bare
 * numbers like {@code button.id == 100} - can never mistake one of these for its own.
 */
@Environment(EnvType.CLIENT)
public class ExtraButton extends ButtonWidget {

	/**
	 * The id every one of these carries. They all share it because none of them is ever dispatched
	 * by id - the action lives on the widget - and it only has to be a number no vanilla screen
	 * tests for. b1.7.3 uses ids well under 1000.
	 */
	public static final int ID = 8000;

	private final Consumer<ExtraButton> action;

	public ExtraButton(int x, int y, int width, int height, String text, Consumer<ExtraButton> action) {
		super(ID, x, y, width, height, text);
		this.action = action;
	}

	@Override
	public boolean isMouseOver(Minecraft minecraft, int mouseX, int mouseY) {
		if (!super.isMouseOver(minecraft, mouseX, mouseY)) return false;
		action.accept(this);
		return true;
	}
}
