package com.securechat.securemessaging.lan;

import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for LanPresenceService.
 * Feature: unified-account-lan-presence
 */
class LanPresenceServicePropertyTest {

    // ── Property 1: Registration records the JWT username ─────────────────────
    // Feature: unified-account-lan-presence, Property 1: registration records JWT username
    // Validates: Requirements 3.3, 7.2
    @Property
    void property1_registerMakesUserAvailable(
            @ForAll @AlphaChars @StringLength(min = 3, max = 30) String username) {

        LanPresenceService service = new LanPresenceService();
        service.register(username);
        assertThat(service.isAvailable(username)).isTrue();
    }

    // ── Property 2: TTL expiry makes stale records unavailable ────────────────
    // Feature: unified-account-lan-presence, Property 2: TTL expiry makes stale records unavailable
    // Validates: Requirements 3.4, 8.1
    @Property
    void property2_staleRecordIsUnavailable(
            @ForAll @AlphaChars @StringLength(min = 3, max = 30) String username) {

        LanPresenceService service = new LanPresenceService();
        // Inject a timestamp 31 seconds past the TTL boundary
        Instant stale = Instant.now().minus(LanPresenceService.PRESENCE_TTL).minusSeconds(1);
        service.getRegistry().put(username, stale);

        assertThat(service.isAvailable(username)).isFalse();
    }

    // ── Property 3: Unregister removes the record immediately ─────────────────
    // Feature: unified-account-lan-presence, Property 3: unregister removes record immediately
    // Validates: Requirements 8.4
    @Property
    void property3_unregisterMakesUserUnavailable(
            @ForAll @AlphaChars @StringLength(min = 3, max = 30) String username) {

        LanPresenceService service = new LanPresenceService();
        service.register(username);
        service.unregister(username);

        assertThat(service.isAvailable(username)).isFalse();
    }

    // ── Property 7: lanAvailable reflects registry state ──────────────────────
    // Feature: unified-account-lan-presence, Property 7: lanAvailable reflects registry state
    // Validates: Requirements 4.3, 4.4
    @Property
    void property7_isAvailableReflectsRegistryState(
            @ForAll @AlphaChars @StringLength(min = 3, max = 30) String contact,
            @ForAll boolean shouldBeRegistered) {

        LanPresenceService service = new LanPresenceService();

        if (shouldBeRegistered) {
            service.register(contact);
        }
        // If not registered, the registry has no entry for this contact

        assertThat(service.isAvailable(contact)).isEqualTo(shouldBeRegistered);
    }
}
