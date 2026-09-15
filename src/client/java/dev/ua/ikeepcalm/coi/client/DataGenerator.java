package dev.ua.ikeepcalm.coi.client;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * The mod's datagen entry point. It creates the pack and generates nothing into
 * it yet: every asset this client ships is hand-authored, so the hook exists to
 * keep the {@code runDatagen} task wired up rather than to produce anything.
 */
public class DataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        fabricDataGenerator.createPack();
    }
}
