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
            case 485 -> {
                client.options.guiScale().set(2);
                client.resizeGui();
                seedManual();
                client.gui.setScreen(new MenuScreen(null));
            }
            case 510 -> capture(client, "14-ability-manual");
            case 515 -> type(client, "Open passage");
            case 519 -> capture(client, "19-manual-category");
            case 520 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
            case 540 -> capture(client, "15-manual-bindings");
            case 545 -> {
                for (int i = 0; i < 2; i++) client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_TAB, 0, 0));
                client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                String stored = dev.ua.ikeepcalm.coi.client.config.AbilityConfig.loadBindings()[0];
                if (!"passage".equals(dev.ua.ikeepcalm.coi.client.ability.AbilityInfo.extractCategory(stored)))
                    throw new IllegalStateException("Category binding did not survive UI assignment and config reload");
                CoiLog.LOG.info("Archive preview: category binding persisted correctly");
            }
            case 547 -> capture(client, "20-manual-assigned");
            case 550 -> {
                client.gui.setScreen(null);
                client.options.guiScale().set(4);
                client.resizeGui();
                seedManual();
                client.gui.setScreen(new MenuScreen(null));
            }
            case 570 -> capture(client, "16-manual-compact-index");
            case 580 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
            case 600 -> capture(client, "17-manual-compact-detail");
            case 610 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0));
            case 630 -> capture(client, "18-manual-compact-modes");
            case 650 -> {
                client.options.guiScale().set(2);
                client.resizeGui();
                client.gui.setScreen(new dev.ua.ikeepcalm.coi.client.screen.ability.AbilityBindingScreen(null));
            }
            case 665 -> capture(client, "21-binding-screen");
            case 670 -> click(client, client.gui.screen().width / 2, 110);
            case 685 -> capture(client, "22-binding-picker");
            case 690 -> type(client, "Open passage");
            case 695 -> capture(client, "23-binding-search");
            case 700 -> {
                client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                String stored = dev.ua.ikeepcalm.coi.client.config.AbilityConfig.loadBindings()[0];
                if (!"passage".equals(dev.ua.ikeepcalm.coi.client.ability.AbilityInfo.extractCategory(stored)))
                    throw new IllegalStateException("Direct category picker failed");
            }
            case 705 -> click(client, client.gui.screen().width / 2, 110);
            case 710 -> type(client, "Secondary action");
            case 715 -> {
                client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                String stored = dev.ua.ikeepcalm.coi.client.config.AbilityConfig.loadBindings()[0];
                if (!"left_click".equals(dev.ua.ikeepcalm.coi.client.ability.AbilityInfo.extractAction(stored)))
                    throw new IllegalStateException("Direct secondary action picker failed");
                CoiLog.LOG.info("Archive preview: grouped category and secondary entries assigned correctly");
            }
            case 720 -> click(client, client.gui.screen().width / 2, 110);
            case 725 -> {
                client.options.guiScale().set(4);
                client.resizeGui();
            }
            case 740 -> capture(client, "24-binding-picker-compact");
            case 750 -> {
                client.gui.setScreen(null);
                client.options.guiScale().set(2);
                client.resizeGui();
                dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry.handleAbilityDataV2("""
                        {"abilities":[{"id":"door-9-2","name":"Spiritual Perception","englishName":"Spiritual Perception",
                        "pathway":"door","sequence":9,"kind":"passive","active":true,
                        "description":"Sense nearby spiritual traces."}]}
                        """);
                passiveDocument(1, true, true);
                client.gui.setScreen(new MenuScreen(null));
            }
            case 770 -> capture(client, "25-passive-enabled");
            case 775 -> {
                for (int i=0; i<2; i++) client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_TAB, 0, 0));
                client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                if (!dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry.getAbilityInfo("door-9-2").active())
                    throw new IllegalStateException("Passive changed without server confirmation");
            }
            case 780 -> {
                dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry.handleAbilityState("{\"id\":\"door-9-2\",\"active\":false}");
                if (dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry.getAbilityInfo("door-9-2").active())
                    throw new IllegalStateException("Passive metadata did not follow server state");
                passiveDocument(2, false, true);
            }
            case 800 -> capture(client, "26-passive-disabled");
            case 805 -> {
                dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry.handleAbilityState("{\"id\":\"door-9-2\",\"active\":true}");
                passiveDocument(3, true, false);
                CoiLog.LOG.info("Archive preview: passive controls follow server state");
            }
            case 820 -> capture(client, "27-passive-always-on");
            case 830 -> client.stop();
            default -> { }
        }
    }

    private static void capture(Minecraft client, String name) {
        Screenshot.grab(client.gameDirectory, name + ".png", client.gameRenderer.mainRenderTarget(), 1,
                message -> CoiLog.LOG.info("Archive preview: {}", message.getString()));
    }

    private static void passiveDocument(int version, boolean active, boolean toggleable) {
        MenuState.adopt(dev.ua.ikeepcalm.coi.client.menu.MenuParser.parse("""
                {"session":"preview","version":%d,"screen":"preview.passives","title":"Door · Sequence 9",
                 "presentation":{"template":"ability_manual"},"sections":[{"components":[
                   {"type":"list","rows":[{"id":"door-9-2","title":"Spiritual Perception","subtitle":"%s",
                    "action":"passive-toggle","enabled":%s,"disabledReason":"This passive is always enabled."}]}]}]}
                """.formatted(version, active ? "Enabled" : "Disabled", toggleable)));
    }

    private static void type(Minecraft client, String text) {
        text.codePoints().forEach(code -> client.gui.screen().charTyped(new net.minecraft.client.input.CharacterEvent(code)));
    }

    private static void click(Minecraft client, double x, double y) {
        client.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y,
                new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
    }

    private static void seedManual() {
        dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry.handleAbilityDataV2("""
                {"abilities":[
                  {"id":"door-5-1","name":"Traveler's Door","englishName":"Traveler's Door","pathway":"door","sequence":5,
                   "category":"mobility","hasLeftClick":true,"description":"Open a door to a place you have visited. Choose whether to travel yourself or let others pass through.",
                   "cost":120,"cooldownSeconds":15,"selectedCategory":"travel","castCategories":[
                     {"id":"travel","name":"Travel","cooldownSeconds":15,"cooldownRemainingTicks":100},
                     {"id":"passage","name":"Open passage","cooldownSeconds":30},
                     {"id":"return","name":"Return","cooldownSeconds":5}]},
                  {"id":"door-6-2","name":"Blink","englishName":"Blink","pathway":"door","sequence":6,"category":"mobility",
                   "description":"Teleport a short distance in the direction you are looking.","cost":40,"cooldownSeconds":3},
                  {"id":"door-7-1","name":"Spirit Vision","englishName":"Spirit Vision","pathway":"door","sequence":7,
                   "kind":"activated","description":"See nearby spiritual traces.","drainPerSecond":2},
                  {"id":"fool-7-2","name":"Flame Jump","englishName":"Flame Jump","pathway":"fool","sequence":7,
                   "locked":true,"description":"Travel between nearby flames.","cost":60,"cooldownSeconds":8}
                ]}
                """);
        MenuState.adopt(dev.ua.ikeepcalm.coi.client.menu.MenuParser.parse("""
                {"session":"preview","version":1,"screen":"preview.manual","title":"Door · Sequence 5",
                 "presentation":{"template":"ability_manual"},"sections":[{"title":"Pathway controls",
                 "components":[{"type":"text","text":"Server controls remain here."}]}]}
                """));
    }
}
