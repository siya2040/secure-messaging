package com.securechat.securemessaging.controller;

import com.securechat.securemessaging.dto.LanPresenceResponse;
import com.securechat.securemessaging.lan.LanDiscoveryService;
import com.securechat.securemessaging.lan.LanInfoResponse;
import com.securechat.securemessaging.lan.LanPresenceService;
import com.securechat.securemessaging.model.GroupMember;
import com.securechat.securemessaging.repository.GroupMemberRepository;
import com.securechat.securemessaging.repository.MessageRepository;
import com.securechat.securemessaging.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Exposes LAN discovery information and presence endpoints.
 *
 * Public endpoints (no JWT required):
 *   GET  /lan/info        — server LAN IP, port, URL
 *   GET  /lan/peers       — all verified users (for LAN discovery)
 *
 * JWT-protected endpoints:
 *   POST   /lan/presence/register    — register/refresh caller's LAN presence
 *   GET    /lan/presence/contacts    — get known contacts with LAN availability
 *   DELETE /lan/presence/unregister  — immediately remove caller's LAN presence
 */
@RestController
@RequestMapping("/lan")
public class LanController {

    private final LanDiscoveryService  discoveryService;
    private final UserRepository       userRepository;
    private final LanPresenceService   presenceService;
    private final MessageRepository    messageRepository;
    private final GroupMemberRepository groupMemberRepository;

    public LanController(LanDiscoveryService discoveryService,
                         UserRepository userRepository,
                         LanPresenceService presenceService,
                         MessageRepository messageRepository,
                         GroupMemberRepository groupMemberRepository) {
        this.discoveryService      = discoveryService;
        this.userRepository        = userRepository;
        this.presenceService       = presenceService;
        this.messageRepository     = messageRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    // ── Public endpoints ──────────────────────────────────────

    /** Returns the server's LAN IP, port, and full URL. */
    @GetMapping("/info")
    public ResponseEntity<LanInfoResponse> getLanInfo() {
        return ResponseEntity.ok(
                new LanInfoResponse(
                        discoveryService.getLanIp(),
                        discoveryService.getServerPort()));
    }

    /**
     * Returns all verified users on this server as potential LAN peers.
     * The requesting user's name is excluded from the list.
     */
    @GetMapping("/peers")
    public ResponseEntity<List<Map<String, String>>> getPeers(
            @RequestParam(required = false) String name) {

        List<Map<String, String>> peers = userRepository.findAll()
                .stream()
                .filter(u -> u.isEmailVerified())
                .filter(u -> name == null || !u.getUsername().equals(name))
                .map(u -> Map.of("name", u.getUsername()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(peers);
    }

    // ── JWT-protected presence endpoints ─────────────────────

    /**
     * Register or refresh the authenticated user's LAN presence.
     * The username is taken from the validated JWT — never from the request body.
     */
    @PostMapping("/presence/register")
    public ResponseEntity<Map<String, String>> registerPresence(
            @AuthenticationPrincipal String username) {

        Instant registeredAt = presenceService.register(username);
        return ResponseEntity.ok(Map.of(
                "username",     username,
                "registeredAt", registeredAt.toString()
        ));
    }

    /**
     * Returns all known contacts of the authenticated user with their LAN availability status.
     *
     * "Known contacts" = DM partners ∪ group co-members (excludes the requesting user).
     * Results are sorted alphabetically by username.
     */
    @GetMapping("/presence/contacts")
    public ResponseEntity<List<LanPresenceResponse>> getPresenceContacts(
            @AuthenticationPrincipal String me) {

        // 1. DM partners (both directions)
        Set<String> contacts = new HashSet<>();
        contacts.addAll(messageRepository.findReceiversForSender(me));
        contacts.addAll(messageRepository.findSendersForReceiver(me));

        // 2. Group co-members
        List<GroupMember> myMemberships = groupMemberRepository.findByUsername(me);
        for (GroupMember membership : myMemberships) {
            groupMemberRepository.findByGroupId(membership.getGroupId())
                    .stream()
                    .map(GroupMember::getUsername)
                    .filter(u -> !u.equals(me))
                    .forEach(contacts::add);
        }

        // 3. Exclude self (belt-and-suspenders)
        contacts.remove(me);

        // 4. Map to response, sort alphabetically
        List<LanPresenceResponse> response = contacts.stream()
                .sorted()
                .map(u -> new LanPresenceResponse(u, presenceService.isAvailable(u)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Immediately remove the authenticated user's LAN presence record.
     * Called via fetch keepalive on tab close / mode switch.
     */
    @DeleteMapping("/presence/unregister")
    public ResponseEntity<Void> unregisterPresence(
            @AuthenticationPrincipal String username) {

        presenceService.unregister(username);
        return ResponseEntity.noContent().build();
    }
}
