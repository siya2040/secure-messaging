package com.securechat.securemessaging.lan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory LAN presence registry.
 *
 * Tracks which authenticated users are currently active on the local network.
 * Records expire after PRESENCE_TTL (30 seconds) if not refreshed.
 * A scheduled cleanup runs every CLEANUP_INTERVAL (10 seconds) to evict stale entries.
 *
 * The registry is intentionally NOT persisted to the database — it resets on server restart.
 */
@Service
public class LanPresenceService {

    private static final Logger log = LoggerFactory.getLogger(LanPresenceService.class);

    public static final Duration PRESENCE_TTL     = Duration.ofSeconds(30);
    public static final long     CLEANUP_INTERVAL = 10_000L; // ms

    // username → last-seen Instant
    private final ConcurrentHashMap<String, Instant> registry = new ConcurrentHashMap<>();

    /**
     * Record or refresh a user's LAN presence.
     * @return the Instant at which the registration was recorded
     */
    public Instant register(String username) {
        Instant now = Instant.now();
        registry.put(username, now);
        log.debug("LAN presence registered: {}", username);
        return now;
    }

    /**
     * Immediately remove a user's LAN presence record.
     */
    public void unregister(String username) {
        registry.remove(username);
        log.debug("LAN presence unregistered: {}", username);
    }

    /**
     * Check whether a user is currently available on LAN.
     * @return true if the user has a presence record with a last-seen timestamp within PRESENCE_TTL
     */
    public boolean isAvailable(String username) {
        Instant lastSeen = registry.get(username);
        if (lastSeen == null) return false;
        return Duration.between(lastSeen, Instant.now()).compareTo(PRESENCE_TTL) < 0;
    }

    /**
     * Scheduled cleanup: removes all entries whose last-seen timestamp is older than PRESENCE_TTL.
     * Runs every CLEANUP_INTERVAL milliseconds.
     */
    @Scheduled(fixedDelay = CLEANUP_INTERVAL)
    public void evictStaleEntries() {
        Instant cutoff = Instant.now().minus(PRESENCE_TTL);
        int before = registry.size();
        registry.entrySet().removeIf(e -> e.getValue().isBefore(cutoff));
        int removed = before - registry.size();
        if (removed > 0) {
            log.debug("LAN presence cleanup: removed {} stale entries", removed);
        }
    }

    /** Package-private accessor for testing — allows injecting stale timestamps. */
    ConcurrentHashMap<String, Instant> getRegistry() {
        return registry;
    }
}
