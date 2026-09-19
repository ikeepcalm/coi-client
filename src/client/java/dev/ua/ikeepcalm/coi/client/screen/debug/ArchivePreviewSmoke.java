package dev.ua.ikeepcalm.coi.client.screen.debug;

import dev.ua.ikeepcalm.coi.CoiLog;
import dev.ua.ikeepcalm.coi.client.ability.AbilityInfo;
import dev.ua.ikeepcalm.coi.client.ability.AbilityRegistry;
import dev.ua.ikeepcalm.coi.client.ability.Pathways;
import dev.ua.ikeepcalm.coi.client.config.AbilityConfig;
import dev.ua.ikeepcalm.coi.client.config.HudConfig;
import dev.ua.ikeepcalm.coi.client.duck.AvatarRenderStateAccessor;
import dev.ua.ikeepcalm.coi.client.menu.MenuParser;
import dev.ua.ikeepcalm.coi.client.network.ServerCapabilities;
import dev.ua.ikeepcalm.coi.client.screen.ability.AbilityBindingScreen;
import dev.ua.ikeepcalm.coi.client.screen.menu.MenuScreen;
import dev.ua.ikeepcalm.coi.client.screen.sheet.CharacterSheetScreen;
import dev.ua.ikeepcalm.coi.client.state.MenuState;
import dev.ua.ikeepcalm.coi.client.state.SheetState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

/** Opt-in development capture, run in a disposable game directory with a copied singleplayer world. */
public class ArchivePreviewSmoke {
    private int ticks;

    private ArchivePreviewSmoke() {}

    public static void register() {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment() || !Boolean.getBoolean("coi.archivePreview")) return;
        ArchivePreviewSmoke smoke = new ArchivePreviewSmoke();
        ClientTickEvents.END_CLIENT_TICK.register(smoke::tick);
    }

    private static void cursor(Minecraft client, double x, double y) {
        int[] width = new int[1], height = new int[1];
        long handle = client.getWindow().handle();
        GLFW.glfwGetWindowSize(handle, width, height);
        GLFW.glfwSetCursorPos(handle, width[0] * x, height[0] * y);
        // Unfocused capture windows may not receive GLFW's move callback.
        // This runner is development-only; keep its synthetic pointer deterministic.
        try {
            var mouseX = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
            var mouseY = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
            mouseX.setAccessible(true);
            mouseY.setAccessible(true);
            mouseX.setDouble(client.mouseHandler, width[0] * x);
            mouseY.setDouble(client.mouseHandler, height[0] * y);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot position capture pointer", exception);
        }
    }

    private static void capture(Minecraft client, String name) {
        Screenshot.grab(client.gameDirectory, name + ".png", client.gameRenderer.mainRenderTarget(), 1,
                message -> CoiLog.LOG.info("Archive preview: {}", message.getString()));
    }

    private static void portrait(Minecraft client, String pathway, int sequence, String name) {
        client.gui.setScreen(null);
        ServerCapabilities.handle(
                "{\"protocol\":2,\"features\":[\"menu_archive\",\"character_sheet\"]}");
        client.options.guiScale().set(2);
        client.resizeGui();
        SheetState.handle("""
                {"pathway":"%s","pathwayName":"%s","sequence":%d,"sequenceName":"%s",
                 "health":40,"maxHealth":40,"spirituality":1200,"maxSpirituality":1600,
                 "actions":{"church":true,"abilities":true,"mythical":true,"uniqueness":true,
                 "honorific":true,"map":true,"seat":true}}
                """.formatted(pathway, Pathways.formatPathwayName(pathway), sequence, name));
        client.gui.setScreen(new CharacterSheetScreen(null));
    }

    private static void archive(Minecraft client, String template, String title) {
        client.gui.setScreen(null);
        String body = switch (template) {
            case "ledger" -> """
                    {"type":"kv","rows":[{"label":"Your rank","value":"Bishop"},
                     {"label":"Members","value":"12"},{"label":"Treasury","value":"2,400"}]},
                    {"type":"buttons","buttons":[{"id":"members","label":"Members"},
                     {"id":"sites","label":"Sacred sites"},{"id":"settings","label":"Settings"}]}
                    """;
            case "relic" -> """
                    {"type":"hero","icon":{"kind":"pathway","value":"fool"},"title":"The Fool",
                     "subtitle":"Accommodated","badge":"SEQUENCE 0"},
                    {"type":"stat","label":"Stability","value":"84%","fraction":0.84},
                    {"type":"button","id":"release","label":"Release uniqueness","style":"danger",
                     "confirm":{"title":"Release uniqueness?","body":"The uniqueness will leave your possession."}}
                    """;
            case "inscription" -> """
                    {"type":"text","text":"The Fool that does not belong to this era;
                    The mysterious ruler above the gray fog;
                    The King of Yellow and Black who wields good luck."},
                    {"type":"input","id":"name","label":"Honorific name","value":"The Fool",
                     "submit":"save","submitLabel":"Save","maxLength":80}
                    """;
            case "atlas" -> """
                    {"type":"toggle","id":"visibility","label":"Visible on the map","on":true,
                     "desc":"Show your location to other players."},
                    {"type":"kv","rows":[{"label":"Current state","value":"Visible"}]},
                    {"type":"note","style":"warn","text":"Hiding your marker does not conceal you from divination."}
                    """;
            default -> """
                    {"type":"steps","items":[{"title":"Hold the sequence bracket","done":true},
                     {"title":"Prepare the acting reserve","text":"8,000 / 10,000","done":false},
                     {"title":"Challenge the seat","text":"The server selects the holder."}]},
                    {"type":"button","id":"challenge","label":"Challenge","enabled":false,
                     "disabledReason":"You need 2,000 more acting points."}
                    """;
        };
        MenuState.adopt(MenuParser.parse("""
                {"session":"preview","version":1,"screen":"preview.%s","title":"%s",
                 "accent":"B893D5","icon":{"kind":"pathway","value":"fool"},"back":true,
                 "presentation":{"template":"%s","subject":"fool"},
                 "sections":[{"title":"Overview","components":[%s]},
                   {"title":"Requirements","components":[{"type":"checklist","items":[
                    {"label":"Pathway unlocked","state":"ok"},{"label":"Permission granted","state":"ok"}]}]},
                   {"title":"Details","components":[{"type":"details","id":"more","summary":"More information",
                    "text":["The server checks your current state before applying any changes."]}]}],
                 "footer":[{"id":"refresh","label":"Refresh"}]}
                """.formatted(template, title, template, body)));
        client.gui.setScreen(new MenuScreen(null));
    }

    private static void passiveDocument(int version, boolean active, boolean toggleable) {
        MenuState.adopt(MenuParser.parse("""
                {"session":"preview","version":%d,"screen":"preview.passives","title":"Door · Sequence 9",
                 "presentation":{"template":"ability_manual"},"sections":[{"components":[
                   {"type":"list","rows":[{"id":"door-9-2","title":"Spiritual Perception","subtitle":"%s",
                    "action":"passive-toggle","enabled":%s,"disabledReason":"This passive is always enabled."}]}]}]}
                """.formatted(version, active ? "Enabled" : "Disabled", toggleable)));
    }

    private static void type(Minecraft client, String text) {
        text.codePoints().forEach(code -> client.gui.screen().charTyped(new CharacterEvent(code)));
    }

    private static void click(Minecraft client, double x, double y) {
        client.gui.screen().mouseClicked(new MouseButtonEvent(x, y,
                new MouseButtonInfo(0, 0)), false);
    }

    private static void seedManual() {
        AbilityRegistry.handleAbilityDataV2("""
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
        MenuState.adopt(MenuParser.parse("""
                {"session":"preview","version":1,"screen":"preview.manual","title":"Door · Sequence 5",
                 "presentation":{"template":"ability_manual"},"sections":[{"title":"Pathway controls",
                 "components":[{"type":"text","text":"Server controls remain here."}]}]}
                """));
    }

    private void tick(Minecraft client) {
        if (client.player == null || !client.hasSingleplayerServer()) return;
        ticks++;
        // Keep hover tooltips out of exports, except during the cursor-follow captures.
        if (!Boolean.getBoolean("coi.portraitPreview") || ticks < 40 + (Pathways.RING.size() + 1) * 32 + 48)
            cursor(client, .99, .01);
        if (Boolean.getBoolean("coi.hudPreview")) {
            if (ticks == 40) {
                client.options.guiScale().set(2);
                client.resizeGui();
                client.gui.setScreen(new HudPreviewScreen(false));
            }
            if (ticks == 65) capture(client, "hud-instruments");
            if (ticks == 70) {
                client.options.guiScale().set(3);
                client.resizeGui();
            }
            if (ticks == 95) capture(client, "hud-instruments-large");
            if (ticks == 100) client.gui.setScreen(new HudPreviewScreen(true));
            if (ticks == 120) capture(client, "hud-health-large");
            if (ticks == 125) {
                client.options.guiScale().set(2);
                client.resizeGui();
            }
            if (ticks == 145) capture(client, "hud-health");
            if (ticks == 150) client.stop();
            return;
        }
        if (Boolean.getBoolean("coi.portraitPreview")) {
            portraitTick(client);
            return;
        }
        switch (ticks) {
            case 40 -> {
                HudConfig.getSettings().epilepsyMode = true;
                client.options.guiScale().set(2);
                client.resizeGui();
                ServerCapabilities.handle("{\"protocol\":2,\"features\":[\"menu_archive\",\"character_sheet\"]}");
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
                String stored = AbilityConfig.loadBindings()[0];
                if (!"passage".equals(AbilityInfo.extractCategory(stored)))
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
                client.gui.setScreen(new AbilityBindingScreen(null));
            }
            case 665 -> capture(client, "21-binding-screen");
            case 670 -> click(client, client.gui.screen().width / 2, 110);
            case 685 -> capture(client, "22-binding-picker");
            case 690 -> type(client, "Open passage");
            case 695 -> capture(client, "23-binding-search");
            case 700 -> {
                client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                String stored = AbilityConfig.loadBindings()[0];
                if (!"passage".equals(AbilityInfo.extractCategory(stored)))
                    throw new IllegalStateException("Direct category picker failed");
            }
            case 705 -> click(client, client.gui.screen().width / 2, 110);
            case 710 -> type(client, net.minecraft.client.resources.language.I18n.get("screen.coi.manual_secondary"));
            case 715 -> {
                client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
                String stored = AbilityConfig.loadBindings()[0];
                if (!"left_click".equals(AbilityInfo.extractAction(stored)))
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
                AbilityRegistry.handleAbilityDataV2("""
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
                if (!AbilityRegistry.getAbilityInfo("door-9-2").active())
                    throw new IllegalStateException("Passive changed without server confirmation");
            }
            case 780 -> {
                AbilityRegistry.handleAbilityState("{\"id\":\"door-9-2\",\"active\":false}");
                if (AbilityRegistry.getAbilityInfo("door-9-2").active())
                    throw new IllegalStateException("Passive metadata did not follow server state");
                passiveDocument(2, false, true);
            }
            case 800 -> capture(client, "26-passive-disabled");
            case 805 -> {
                AbilityRegistry.handleAbilityState("{\"id\":\"door-9-2\",\"active\":true}");
                passiveDocument(3, true, false);
                CoiLog.LOG.info("Archive preview: passive controls follow server state");
            }
            case 820 -> capture(client, "27-passive-always-on");
            case 830 -> archive(client, "ledger", "Church of the Fool");
            case 850 -> capture(client, "28-church-ledger");
            case 860 -> archive(client, "relic", "Uniqueness");
            case 880 -> capture(client, "29-relic");
            case 890 -> archive(client, "inscription", "Honorific name");
            case 910 -> capture(client, "30-inscription");
            case 920 -> archive(client, "atlas", "Map visibility");
            case 940 -> capture(client, "31-atlas");
            case 950 -> archive(client, "challenge", "Sequence seat");
            case 970 -> capture(client, "32-challenge");
            case 980 -> {
                client.options.guiScale().set(4);
                client.resizeGui();
            }
            case 1000 -> capture(client, "33-challenge-compact");
            case 1010 -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_END, 0, 0));
            case 1030 -> capture(client, "34-challenge-compact-end");
            case 1040 -> portrait(client, "priest", 9, "Hunter");
            case 1060 -> capture(client, "35-priest-nine");
            case 1070 -> portrait(client, "priest", 0, "Red Priest");
            case 1090 -> capture(client, "36-priest-zero");
            case 1100 -> portrait(client, "fool", 0, "The Fool");
            case 1120 -> capture(client, "37-fool-zero");
            case 1130 -> portrait(client, "door", 0, "Door");
            case 1150 -> capture(client, "38-door-zero");
            case 1160 -> HudConfig.getSettings().epilepsyMode = false;
            case 1180 -> capture(client, "39-door-animated");
            case 1190 -> {
                client.options.guiScale().set(4);
                client.resizeGui();
            }
            case 1210 -> capture(client, "40-door-compact");
            case 1220 -> {
                client.gui.setScreen(null);
                var renderer = client.getEntityRenderDispatcher().getRenderer(client.player);
                var state = renderer.createRenderState(client.player, 1f);
                if (state instanceof AvatarRenderStateAccessor avatar
                        && (avatar.coi$getPortraitPose() != null || avatar.coi$getPreviewForm() != null))
                    throw new IllegalStateException("Portrait state leaked into normal player extraction");
                CoiLog.LOG.info("Archive preview: normal player extraction has no portrait overrides");
                client.stop();
            }
            default -> { }
        }
    }

    private void portraitTick(Minecraft client) {
        var pathways = new ArrayList<>(Pathways.RING);
        pathways.add("error");
        int phase = ticks - 40;
        if (phase < 0) return;
        int shot = phase / 16;
        if (shot < pathways.size() * 2) {
            String pathway = pathways.get(shot / 2);
            int sequence = shot % 2 == 0 ? 9 : 0;
            if (phase % 16 == 0) {
                HudConfig.getSettings().epilepsyMode = false;
                portrait(client, pathway, sequence, "");
            } else if (phase % 16 == 12) capture(client, "pathway-" + pathway + "-" + sequence);
            return;
        }
        int end = phase - pathways.size() * 32;
        switch (end) {
            case 0 -> {
                HudConfig.getSettings().epilepsyMode = true;
                portrait(client, "chained", 0, "Chained");
            }
            case 12 -> capture(client, "pathway-chained-frozen");
            case 16 -> {
                client.options.guiScale().set(4);
                client.resizeGui();
            }
            case 28 -> capture(client, "pathway-chained-compact");
            case 32 -> {
                client.options.guiScale().set(2);
                client.resizeGui();
                archive(client, "inscription", "Honorific name");
            }
            case 44 -> capture(client, "honorific-emblem-aligned");
            case 48 -> {
                HudConfig.getSettings().epilepsyMode = false;
                portrait(client, "chained", 0, "Chained");
                cursor(client, .01, .01);
            }
            case 68 -> capture(client, "portrait-cursor-left");
            case 72 -> cursor(client, .99, .99);
            case 92 -> capture(client, "portrait-cursor-right");
            case 96 -> HudConfig.getSettings().epilepsyMode = true;
            case 108 -> capture(client, "portrait-cursor-frozen");
            case 112 -> {
                client.gui.setScreen(null);
                var state = client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player, 1f);
                if (state instanceof AvatarRenderStateAccessor avatar
                        && avatar.coi$getPortraitPose() != null)
                    throw new IllegalStateException("Portrait override leaked into world rendering");
                CoiLog.LOG.info("Portrait preview: all 25 pathways at Sequences 9/0 and clean world state verified");
                client.stop();
            }
            default -> {
            }
        }
    }
}
