package dev.ua.ikeepcalm.coi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's one logger.
 * <p>
 * Everything that used to call {@code System.out.println} with a hand-written
 * "COI Client: " prefix goes through here instead: the prefix is the logger's
 * name, so it cannot drift between call sites, and the lines land in the game's
 * log with a level and a timestamp like every other mod's.
 */
public class CoiLog {

    public static final Logger LOG = LoggerFactory.getLogger("COI Client");

    private CoiLog() {
    }
}
