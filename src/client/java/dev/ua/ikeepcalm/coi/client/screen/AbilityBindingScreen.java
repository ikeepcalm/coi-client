package dev.ua.ikeepcalm.coi.client.screen;

import dev.ua.ikeepcalm.coi.client.CircleOfImaginationClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AbilityBindingScreen extends Screen {

    private final Screen parent;
    private AbilityDropdownWidget ability1Dropdown;
    private AbilityDropdownWidget ability2Dropdown;
    private AbilityDropdownWidget ability3Dropdown;

    public AbilityBindingScreen(Screen parent) {
        super(Text.of("screen.coi.ability_binding"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Get the list of abilities from the client (these are in "id - name" format)
        List<String> abilitiesFromClient = CircleOfImaginationClient.getAvailableAbilities();

        // Prepare display options for the dropdown, including a "None" option
        List<String> displayOptions = new ArrayList<>();
        displayOptions.add("None"); // Add "None" as the first option
        displayOptions.addAll(abilitiesFromClient);

        int centerX = this.width / 2;
        int startY = this.height / 4;

        // --- Ability Slot 1 ---
        ability1Dropdown = new AbilityDropdownWidget(
                centerX - 100, startY, 200, 20,
                displayOptions, // Pass the list with "None"
                getCurrentDisplayForBoundAbility(0, displayOptions), // Get current display string for this slot
                selectedDisplayString -> { // This is the string selected in the dropdown (e.g., "id - name" or "None")
                    String idToSet = null;
                    if (selectedDisplayString != null && !selectedDisplayString.equals("None") && !selectedDisplayString.equals("Select Ability")) {
                        // Extract the ID part from "id - name"
                        String[] parts = selectedDisplayString.split(" - ", 2);
                        if (parts.length > 0) {
                            idToSet = parts[0];
                        }
                    }
                    CircleOfImaginationClient.setBoundAbility(0, idToSet);
                }
        );
        this.addDrawableChild(ability1Dropdown);

        // --- Ability Slot 2 ---
        ability2Dropdown = new AbilityDropdownWidget(
                centerX - 100, startY + 40, 200, 20,
                displayOptions,
                getCurrentDisplayForBoundAbility(1, displayOptions),
                selectedDisplayString -> {
                    String idToSet = null;
                    if (selectedDisplayString != null && !selectedDisplayString.equals("None") && !selectedDisplayString.equals("Select Ability")) {
                        String[] parts = selectedDisplayString.split(" - ", 2);
                        if (parts.length > 0) {
                            idToSet = parts[0];
                        }
                    }
                    CircleOfImaginationClient.setBoundAbility(1, idToSet);
                }
        );
        this.addDrawableChild(ability2Dropdown);

        // --- Ability Slot 3 ---
        ability3Dropdown = new AbilityDropdownWidget(
                centerX - 100, startY + 80, 200, 20,
                displayOptions,
                getCurrentDisplayForBoundAbility(2, displayOptions),
                selectedDisplayString -> {
                    String idToSet = null;
                    if (selectedDisplayString != null && !selectedDisplayString.equals("None") && !selectedDisplayString.equals("Select Ability")) {
                        String[] parts = selectedDisplayString.split(" - ", 2);
                        if (parts.length > 0) {
                            idToSet = parts[0];
                        }
                    }
                    CircleOfImaginationClient.setBoundAbility(2, idToSet);
                }
        );
        this.addDrawableChild(ability3Dropdown);

        // --- Done Button ---
        this.addDrawableChild(ButtonWidget.builder(
                Text.of("Готово"),
                button -> this.close()
        ).dimensions(centerX - 50, this.height - 40, 100, 20).build());
    }

    /**
     * Helper method to find the correct display string (e.g., "id - name" or "None")
     * for the currently bound ability ID in a given slot.
     * This is used to set the initial selected item in the dropdown.
     */
    private String getCurrentDisplayForBoundAbility(int slot, List<String> displayOptions) {
        String boundId = CircleOfImaginationClient.getBoundAbility(slot);

        if (boundId == null) {
            return "None"; // If no ability is bound, "None" should be selected
        }

        // Search for the full "id - name" string in displayOptions that matches the boundId
        for (String option : displayOptions) {
            if (option.equals("None")) continue; // Skip the "None" option itself in this check
            // Check if the option starts with "boundId - "
            if (option.startsWith(boundId + " - ")) {
                return option; // Found the matching display string
            }
        }
        // If the boundId is somehow not in the current list (e.g., old config),
        // default to "None" or you could return the raw boundId if you want to indicate an issue.
        return "None";
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta); // Use renderBackground for default screen background

        context.drawCenteredTextWithShadow(this.textRenderer,
                this.title, this.width / 2, 20, 0xFFFFFF);

        int centerX = this.width / 2;
        int startY = this.height / 4;

        // Labels for the dropdowns
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("screen.coi.ability1_label"), // Make sure these translation keys exist
                centerX - 100, startY - 12, 0xA0A0A0);

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("screen.coi.ability1_label"),
                centerX - 100, startY + 28, 0xA0A0A0);

        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("screen.coi.ability1_label"),
                centerX - 100, startY + 68, 0xA0A0A0);

        super.render(context, mouseX, mouseY, delta); // Renders drawable children like dropdowns and buttons
    }

    @Override
    public void close() {
        // Save bindings when closing (optional, could also save on every change)
        // AbilityConfig.saveBindings is already called within CircleOfImaginationClient.setBoundAbility
        this.client.setScreen(this.parent);
    }
}
