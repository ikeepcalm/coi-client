package dev.ua.ikeepcalm.coi.client.screen.menu;

import dev.ua.ikeepcalm.coi.client.menu.MenuComponent;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;

/**
 * Everything a collaborator may ask the menu screen for.
 * <p>
 * {@link MenuPart}s, the card chrome and the confirm modal are drawn beside
 * {@link MenuScreen} rather than inside it, and this is the whole of what they
 * may reach: the card's measurements, the document's accent, the one easing
 * chokepoint, the tooltip sinks, and the handful of actions that can reach the
 * wire. Nothing here exposes the document or the scroll position — a part is
 * told where to draw, it never asks.
 */
interface MenuContext {

    Font font();

    /** The document's own colour, or the mod's gold when it named none. */
    int accent();

    /** A short window has no room for the roomy paddings; see {@code pad}. */
    boolean compact();

    /** A button row's height, which the compact layout shortens. */
    int buttonH();

    /** The card's own padding, which the compact layout narrows. */
    int pad();

    /** The window, for the one thing centred on it rather than on the card. */
    int screenWidth();

    int screenHeight();

    /** The width inside the card's padding — every full-width band's width. */
    int contentW();

    /**
     * One step of a tween toward {@code target}, frame-rate independent, and
     * the one place {@code epilepsyMode} is honoured.
     */
    float approach(float current, float target, float durationMs);

    /** Raises a muted one-line tooltip, unless the text is empty. */
    void hint(String text);

    /** Raises a red one-line tooltip, unless the text is empty. */
    void reason(String text);

    /** Raises a multi-line tooltip: {@code lead} in white, the body muted. */
    void lines(List<String> body, String lead);

    /** Names the chevron under the cursor, so a collapsed row is not a silent one. */
    void disclosureHint(boolean open);

    /** Plays the click and sends the action, guarded by {@code canSend}. */
    void fire(String action, String value);

    /**
     * The one gate between a button and the wire: a {@code confirm} raises the
     * modal instead of sending.
     */
    void activate(MenuComponent.Button button, String value);

    int rowHeightFor(List<MenuComponent.Button> buttons);

    void drawButtons(GuiGraphicsExtractor g, List<MenuComponent.Button> buttons,
                     int x, int y, int width, int cols, int rowH, int mouseX, int mouseY);

    /** @return the index of the button under the cursor, or -1 */
    int hitButton(double mx, double my, int count, int x, int y, int width, int cols, int rowH);

    /** A text field that outlives the layout it was built for, keyed by {@code key}. */
    EditBox field(String key, Supplier<EditBox> factory);

    void drawField(GuiGraphicsExtractor g, EditBox box, int x, int y, int w, int mouseX, int mouseY);

    void focusField(EditBox box);

    /**
     * @param initial the server's opening suggestion, used only until the
     *                player has touched this one
     */
    boolean disclosed(String id, boolean initial);

    void toggleDisclosure(String id, boolean initial);

    /** The eased rotation of one disclosure chevron, 0 closed to 1 open. */
    float chevron(String id, boolean open);

    /** Heights changed, so the whole card has to be laid out again. */
    void relayout();
}
