package dev.ua.ikeepcalm.coi.client;

import dev.ua.ikeepcalm.coi.client.config.ClientStateStore;

public class ClientBeyonderState {

    private static double madness = 0.0;
    private static double permanentMadness = 0.0;
    private static int freezeStacks = 0;
    private static int mentalPressure = 0;
    private static double tiredness = 0.0;

    // For flash / shake animation when madness increases
    private static double lastMadness = 0.0;
    private static long lastMadnessIncreaseTime = 0;
    private static float flashIntensity = 0.0f;

    public static double getMadness() {
        return madness;
    }

    public static double getPermanentMadness() {
        return permanentMadness;
    }

    public static int getFreezeStacks() {
        return freezeStacks;
    }

    public static int getMentalPressure() {
        return mentalPressure;
    }

    public static double getTiredness() {
        return tiredness;
    }

    public static float getFlashIntensity() {
        long elapsed = System.currentTimeMillis() - lastMadnessIncreaseTime;
        if (elapsed >= 500) {
            flashIntensity = 0.0f;
        } else {
            flashIntensity = 1.0f - (elapsed / 500.0f);
        }
        return flashIntensity;
    }

    public static void updateConditions(double madnessVal, double permMadnessVal, int freezeVal, int pressureVal, double tirednessVal) {
        if (madnessVal > madness) {
            lastMadnessIncreaseTime = System.currentTimeMillis();
            flashIntensity = 1.0f;
        }
        lastMadness = madness;
        madness = madnessVal;
        permanentMadness = permMadnessVal;
        ClientStateStore.setPermanentMadness(permMadnessVal);
        freezeStacks = freezeVal;
        mentalPressure = pressureVal;
        tiredness = tirednessVal;
    }

    public static void parseAndUpdate(String data) {
        if (data == null || data.isEmpty()) return;

        double newMadness = madness;
        double newPermMadness = permanentMadness;
        int newFreeze = freezeStacks;
        int newPressure = mentalPressure;
        double newTiredness = tiredness;

        try {
            String[] pairs = data.split(";");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    switch (key) {
                        case "madness" -> newMadness = Double.parseDouble(value);
                        case "permanentMadness" -> newPermMadness = Double.parseDouble(value);
                        case "freezeStacks" -> newFreeze = Integer.parseInt(value);
                        case "mentalPressure" -> newPressure = Integer.parseInt(value);
                        case "tiredness" -> newTiredness = Double.parseDouble(value);
                    }
                }
            }
            updateConditions(newMadness, newPermMadness, newFreeze, newPressure, newTiredness);
        } catch (Exception e) {
            System.err.println("Error parsing Beyonder conditions: " + data);
            e.printStackTrace();
        }
    }

    public static void reset() {
        madness = 0.0;
        permanentMadness = 0.0;
        freezeStacks = 0;
        mentalPressure = 0;
        tiredness = 0.0;
        lastMadness = 0.0;
        lastMadnessIncreaseTime = 0;
        flashIntensity = 0.0f;
    }
}
