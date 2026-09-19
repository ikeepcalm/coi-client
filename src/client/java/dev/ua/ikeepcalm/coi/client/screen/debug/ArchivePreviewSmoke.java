package dev.ua.ikeepcalm.coi.client.screen.debug;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuScreen;
import dev.ua.ikeepcalm.coi.client.screen.sheet.CharacterSheetScreen;
import dev.ua.ikeepcalm.coi.client.state.MenuState;
import dev.ua.ikeepcalm.coi.client.state.SheetState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

/** Opt-in development capture, run in a disposable game directory with a copied singleplayer world. */
public final class ArchivePreviewSmoke {
    private int ticks;

    private ArchivePreviewSmoke() {}

    public static void register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment() || !Boolean.getBoolean("coi.archivePreview")) return;
        ArchivePreviewSmoke smoke = new ArchivePreviewSmoke();
        ClientTickEvents.END_CLIENT_TICK.register(smoke::tick);
    }

    private void tick(Minecraft client) {
        if (client.player == null || !client.hasSingleplayerServer()) return;
        ticks++;
        switch (ticks) {
            case 40 -> {
                HudConfig.getSettings().epilepsyMode = true;
                client.options.guiScale().set(2);
                client.resizeGui();
                SheetState.debugInject();
                client.gui.setScreen(new CharacterSheetScreen(null));
            }
            case 70 -> capture(client, "01-dossier");
            case 80 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0));
            case 100 -> capture(client, "02-acting");
            case 110 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0));
            case 130 -> capture(client, "03-destinations");
            case 140 -> {
                MenuState.debugSpecimen();
                client.gui.setScreen(new MenuScreen(null));
            }
            case 165 -> capture(client, "04-specimen");
            case 175 -> {
                client.options.guiScale().set(4);
                client.resizeGui();
            }
            case 200 -> capture(client, "05-specimen-compact");
            case 210 -> client.gui.setScreen(new CharacterSheetScreen(null));
            case 235 -> capture(client, "06-dossier-compact");
            case 245 -> {
                client.options.guiScale().set(2);
                client.resizeGui();
                MenuState.debugSpecimen("visionary");
                client.gui.setScreen(new MenuScreen(null));
            }
            case 270 -> capture(client, "07-partial-form");
            case 280 -> MenuState.debugSpecimen("unknown-form");
            case 300 -> capture(client, "08-unknown-form");
            case 320 -> {
                client.options.guiScale().set(1);
                client.resizeGui();
                client.gui.setScreen(new CharacterSheetScreen(null));
            }
            case 345 -> capture(client, "09-dossier-roomy");
            case 355 -> {
                client.options.guiScale().set(4);
                client.resizeGui();
                client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0));
            }
            case 375 -> capture(client, "10-acting-compact");
            case 385 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_END, 0, 0));
            case 405 -> capture(client, "11-acting-compact-end");
            case 415 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0));
            case 435 -> capture(client, "12-destinations-compact");
            case 445 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_END, 0, 0));
            case 465 -> capture(client, "13-destinations-compact-end");
            case 485 -> client.stop();
            default -> { }
        }
    }

    private static void capture(Minecraft client, String name) {
        Screenshot.grab(client.gameDirectory, name + ".png", client.gameRenderer.mainRenderTarget(), 1,
                message -> CoiLog.LOG.info("Archive preview: {}", message.getString()));
    }
}
