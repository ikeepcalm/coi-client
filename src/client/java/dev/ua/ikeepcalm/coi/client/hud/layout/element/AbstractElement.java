package dev.ua.ikeepcalm.coi.client.hud.layout.element;

import dev.ua.ikeepcalm.coi.client.hud.layout.HudElement;

/**
 * Shared id/label plumbing for every descriptor in this package.
 */
public abstract class AbstractElement implements HudElement {

    private final String id;

    protected AbstractElement(String id) {
        this.id = id;
    }

    @Override
    public final String id() {
        return id;
    }
}
