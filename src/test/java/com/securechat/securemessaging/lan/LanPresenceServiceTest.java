package com.securechat.securemessaging.lan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LanPresenceServiceTest {

    private LanPresenceService service;

    @BeforeEach
    void setUp() {
        service = new LanPresenceService();
    }

    @Test
    void register_storesUsernameWithRecentTimestamp() {
        Instant before = Instant.now();
        Instant result = service.register("alice");
        Instant after  = Instant.now();

        assertThat(result).isAfterOrEqualTo(before);
        assertThat(result).isBeforeOrEqualTo(after);
        assertThat(service.getRegistry()).containsKey("alice");
    }

    @Test
    void register_calledTwice_updatesTimestamp() throws InterruptedException {
        service.register("alice");
        Instant first = service.getRegistry().get("alice");

        Thread.sleep(10);
        service.register("alice");
        Instant second = service.getRegistry().get("alice");

        assertThat(second).isAfter(first);
    }

    @Test
    void isAvailable_returnsTrueForFreshlyRegisteredUser() {
        service.register("bob");
        assertThat(service.isAvailable("bob")).isTrue();
    }

    @Test
    void isAvailable_returnsFalseForUnregisteredUser() {
        assertThat(service.isAvailable("nobody")).isFalse();
    }

    @Test
    void isAvailable_returnsFalseAfterTtlElapsed() {
        // Inject a stale timestamp directly into the registry
        Instant stale = Instant.now().minus(LanPresenceService.PRESENCE_TTL).minusSeconds(1);
        service.getRegistry().put("carol", stale);

        assertThat(service.isAvailable("carol")).isFalse();
    }

    @Test
    void unregister_removesEntry_subsequentIsAvailableReturnsFalse() {
        service.register("dave");
        assertThat(service.isAvailable("dave")).isTrue();

        service.unregister("dave");
        assertThat(service.isAvailable("dave")).isFalse();
    }

    @Test
    void evictStaleEntries_removesStaleAndRetainsFresh() {
        // Fresh entry
        service.register("fresh");

        // Stale entry
        Instant stale = Instant.now().minus(LanPresenceService.PRESENCE_TTL).minusSeconds(1);
        service.getRegistry().put("stale", stale);

        service.evictStaleEntries();

        assertThat(service.getRegistry()).containsKey("fresh");
        assertThat(service.getRegistry()).doesNotContainKey("stale");
    }
}
