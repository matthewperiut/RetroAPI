package com.periut.retroapi.client.screen;

import com.periut.retroapi.commandblock.CommandBlockEntity;
import com.periut.retroapi.commandblock.CommandBlockMode;
import com.periut.retroapi.commandblock.CommandBlocks;
import com.periut.retroapi.client.gui.RetroTextField;
import com.periut.retroapi.commands.client.gui.CommandSuggestor;
import com.periut.retroapi.client.gui.RetroKeys;
import com.periut.retroapi.commandblock.CommandBlockNetworking;
import com.periut.retroapi.network.RetroAPINetworking;
import net.ornithemc.osl.networking.api.client.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * The command block editor, ported from modern's {@code CommandBlockEditScreen} and its abstract
 * parent.
 *
 * <p>Modern's layout to the pixel: the title at {@code y=20}, the "Command" label at {@code y=40},
 * the command field 300 wide at {@code y=50}, the three setting buttons 100 wide at {@code y=165}
 * (mode, conditional, always-active, laid out left to right about the centre), the previous-output
 * field at {@code y=135} with its 20-wide track-output toggle at the right end, and Done / Cancel
 * 150 wide at {@code height/4 + 132}.
 *
 * <p>The command field is RetroAPI's own {@code RetroTextField} under the chat bar's own
 * {@code CommandSuggestor}, so this box is the chat box in every way that matters: click to place the
 * cursor, drag or shift-click to select, clipboard and word jumps, arguments coloured as they parse,
 * errors underlined, and Brigadier's completions in a window under the box - arrows to pick, Tab to
 * take, Escape to dismiss without leaving the screen.
 *
 * <p>The suggestor runs in modern's {@code commandsOnly} mode here: the whole line is the command, so
 * no leading slash is needed. One typed anyway is stripped before the command runs, as modern does.
 */
public class CommandBlockScreen extends Screen {

    private static final int DONE_ID = 0;
    private static final int CANCEL_ID = 1;
    private static final int MODE_ID = 2;
    private static final int CONDITIONAL_ID = 3;
    private static final int AUTO_ID = 4;
    private static final int TRACK_OUTPUT_ID = 5;

    /** Modern's own: the command box is 300 wide at y=50, the previous-output box 20 tall at y=135. */
    private static final int FIELD_WIDTH = 300;
    private static final int FIELD_Y = 50;
    private static final int OUTPUT_Y = 135;
    /** The mode / conditional / always-active row, which the suggestion list must not reach. */
    private static final int BUTTON_ROW_Y = 165;
    private static final int SUGGESTION_Y = FIELD_Y + 22;

    private final int blockX;
    private final int blockY;
    private final int blockZ;

    private RetroTextField commandField;
    private CommandSuggestor suggestor;
    /** Beta gives a press and a release and nothing between, so a drag-select is followed by hand. */
    private boolean selecting;

    private CommandBlockMode mode = CommandBlockMode.REDSTONE;
    private boolean conditional;
    private boolean automatic;
    private boolean trackOutput = true;
    private String previousOutput = "";

    public CommandBlockScreen(final int x, final int y, final int z) {
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
    }

    @Override
    public void init() {
        org.lwjgl.input.Keyboard.enableRepeatEvents(true);
        buttons.clear();

        readFromBlock();

        // The chat bar's own field, so the block editor edits text exactly the way chat does -
        // same cursor, selection, clipboard and word jumps - and the chat bar's own suggestor over
        // it, in modern's commandsOnly mode: the whole line is the command, no leading slash needed
        // (the executor takes one if it is there). The window hangs under the box rather than off
        // the bottom of the screen, as modern's does here.
        commandField = new RetroTextField(textRenderer, FIELD_WIDTH - 8);
        commandField.setMaxLength(32500);
        suggestor = new CommandSuggestor(this, commandField, textRenderer, true);
        suggestor.setAnchor(fieldTextX(), SUGGESTION_Y);
        // Seven rows, which is modern's own limit for this screen and here is also exactly what fits:
        // the list starts under the command box and has to stop before the settings buttons.
        suggestor.setMaxVisible((BUTTON_ROW_Y - SUGGESTION_Y) / 12);
        commandField.setRenderTextProvider(suggestor::highlight);
        commandField.setChangedListener(text -> suggestor.refresh());
        commandField.setText(commandText());
        suggestor.refresh();

        buttons.add(new ButtonWidget(MODE_ID, width / 2 - 50 - 100 - 4, 165, 100, 20, modeLabel()));
        buttons.add(new ButtonWidget(CONDITIONAL_ID, width / 2 - 50, 165, 100, 20, conditionalLabel()));
        buttons.add(new ButtonWidget(AUTO_ID, width / 2 + 50 + 4, 165, 100, 20, autoLabel()));
        buttons.add(new ButtonWidget(TRACK_OUTPUT_ID, width / 2 + 150 - 20, 135, 20, 20, trackOutput ? "O" : "X"));
        buttons.add(new ButtonWidget(DONE_ID, width / 2 - 4 - 150, height / 4 + 120 + 12, 150, 20, "Done"));
        buttons.add(new ButtonWidget(CANCEL_ID, width / 2 + 4, height / 4 + 120 + 12, 150, 20, "Cancel"));
    }

    private CommandBlockEntity entity() {
        return minecraft.world != null
            && minecraft.world.getBlockEntity(blockX, blockY, blockZ) instanceof CommandBlockEntity block
            ? block : null;
    }

    private String commandText() {
        final CommandBlockEntity block = entity();
        return block == null ? "" : block.getCommand();
    }

    private void readFromBlock() {
        final CommandBlockEntity block = entity();
        if (block == null) {
            return;
        }
        mode = block.getMode();
        trackOutput = block.isTrackOutput();
        automatic = block.isAutomatic();
        previousOutput = block.getLastOutput();
        conditional = minecraft.world != null
            && CommandBlocks.isConditional(minecraft.world, blockX, blockY, blockZ);
    }

    /**
     * Sends the edit, or applies it here when this game owns the world.
     *
     * <p>Lives on the screen rather than beside {@code CommandBlockNetworking.apply}: that class is
     * common code the dedicated server loads to handle the packet, and a {@code Minecraft} reference in
     * it made the server try to load {@code ClientPlayerEntity} and refuse - taking the save with it.
     */
    private void save() {
        if (ClientPlayNetworking.isPlayReady(RetroAPINetworking.COMMAND_BLOCK_CHANNEL)) {
            final String command = commandField.getText();
            ClientPlayNetworking.send(RetroAPINetworking.COMMAND_BLOCK_CHANNEL, buffer -> {
                buffer.writeInt(blockX);
                buffer.writeInt(blockY);
                buffer.writeInt(blockZ);
                buffer.writeString(command == null ? "" : command);
                buffer.writeVarInt(mode.ordinal());
                buffer.writeBoolean(conditional);
                buffer.writeBoolean(automatic);
                buffer.writeBoolean(trackOutput);
            });
            return;
        }

        // Singleplayer: this game IS the server.
        CommandBlockNetworking.apply(minecraft.world, minecraft.player, blockX, blockY, blockZ,
            commandField.getText(), mode, conditional, automatic, trackOutput);
    }

    /** The block's own last output, or nothing while output is not being tracked. */
    private String lastOutputOfBlock() {
        final CommandBlockEntity block = entity();
        return trackOutput && block != null ? block.getLastOutput() : "";
    }

    /** Where the command field draws its text, which is also where a completion has to line up. */
    private int fieldTextX() {
        return width / 2 - FIELD_WIDTH / 2 + 4;
    }

    private String modeLabel() {
        return switch (mode) {
            case SEQUENCE -> "Chain";
            case AUTO -> "Repeat";
            case REDSTONE -> "Impulse";
        };
    }

    private String conditionalLabel() {
        return conditional ? "Conditional" : "Unconditional";
    }

    private String autoLabel() {
        return automatic ? "Always Active" : "Needs Redstone";
    }

    @Override
    protected void buttonClicked(final ButtonWidget button) {
        switch (button.id) {
            case DONE_ID -> {
                save();
                minecraft.setScreen(null);
            }
            case CANCEL_ID -> minecraft.setScreen(null);
            case MODE_ID -> {
                mode = mode.next();
                button.text = modeLabel();
            }
            case CONDITIONAL_ID -> {
                conditional = !conditional;
                button.text = conditionalLabel();
            }
            case AUTO_ID -> {
                automatic = !automatic;
                button.text = autoLabel();
            }
            case TRACK_OUTPUT_ID -> {
                // Modern only changes what the box SHOWS - "-" while output is off - and reads the
                // block's own last output back when it goes on again. Clearing the field here is
                // what made the output vanish for good.
                trackOutput = !trackOutput;
                button.text = trackOutput ? "O" : "X";
                previousOutput = lastOutputOfBlock();
            }
            default -> {
            }
        }
    }

    @Override
    protected void keyPressed(final char typed, final int keyCode) {
        // The suggestor first, exactly as modern asks it first: it owns the arrows and Tab while the
        // window is open, and Escape closes the window rather than the screen.
        if (suggestor.keyPressed(typed, keyCode)) {
            return;
        }

        switch (keyCode) {
            case RetroKeys.ESCAPE -> {
                minecraft.setScreen(null);
                return;
            }
            case RetroKeys.RETURN, RetroKeys.NUMPAD_ENTER -> {
                buttonClicked(buttonById(DONE_ID));
                return;
            }
            default -> {
            }
        }

        commandField.keyPressed(typed, keyCode);
    }

    private ButtonWidget buttonById(final int id) {
        for (final Object candidate : buttons) {
            if (candidate instanceof ButtonWidget button && button.id == id) {
                return button;
            }
        }
        return null;
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) {
        if (suggestor.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (button == 0 && mouseY >= FIELD_Y && mouseY < FIELD_Y + 20) {
            // Shift extends the selection, and holding the button drags it out - the same two things
            // the chat bar does, which is what "highlightable" means for this field.
            commandField.click(mouseX - fieldTextX(), RetroKeys.isShiftDown());
            selecting = true;
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseReleased(final int mouseX, final int mouseY, final int button) {
        super.mouseReleased(mouseX, mouseY, button);
        if (button == 0) {
            selecting = false;
        }
    }

    /** Beta routes the wheel through here, which is how the suggestion list scrolls. */
    @Override
    public void onMouseEvent() {
        super.onMouseEvent();

        final int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            suggestor.mouseScrolled(wheel > 0 ? 1 : -1);
        }
    }

    @Override
    public void render(final int mouseX, final int mouseY, final float delta) {
        renderBackground();

        if (selecting) {
            if (org.lwjgl.input.Mouse.isButtonDown(0)) {
                commandField.click(mouseX - fieldTextX(), true);
            } else {
                selecting = false;
            }
        }

        drawCenteredTextWithShadow(textRenderer, "Set Console Command", width / 2, 20, 0xFFFFFF);
        drawTextWithShadow(textRenderer, "Console Command", width / 2 - 150 + 1, 40, 0x9D9D9D);

        fill(width / 2 - 151, FIELD_Y - 1, width / 2 + 151, FIELD_Y + 21, 0xFF373737);
        fill(width / 2 - 150, FIELD_Y, width / 2 + 150, FIELD_Y + 20, 0xFF000000);
        commandField.render(fieldTextX(), FIELD_Y + 6);

        // O shows the output row, X takes it away - and toggling back to O brings it, and whatever the
        // block last printed, straight back: the text is read from the block rather than thrown away.
        if (trackOutput) {
            drawTextWithShadow(textRenderer, "Previous Output", width / 2 - 150 + 1, OUTPUT_Y - 16, 0x9D9D9D);
            fill(width / 2 - 150, OUTPUT_Y, width / 2 + 150 - 24, OUTPUT_Y + 20, 0xFF000000);
            drawTextWithShadow(textRenderer, trim(previousOutput), width / 2 - 150 + 4, OUTPUT_Y + 6, 0x9D9D9D);
        }

        super.render(mouseX, mouseY, delta);

        // Over the buttons: the window drops below the command box and would otherwise be drawn under
        // them the moment it is more than a few entries tall.
        suggestor.render(mouseX, mouseY);
    }

    /** One line of output, cut to the field, because beta's field does not scroll. */
    private String trim(final String text) {
        final String firstLine = text.contains("\n") ? text.substring(0, text.indexOf('\n')) : text;
        return textRenderer.getWidth(firstLine) <= 268 ? firstLine : firstLine.substring(0, 40) + "...";
    }

    @Override
    public void tick() {
        super.tick();
        commandField.tick();
    }

    @Override
    public void removed() {
        org.lwjgl.input.Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
