package com.securechat.securemessaging.dto;

public class LanPresenceResponse {

    private final String  username;
    private final boolean lanAvailable;

    public LanPresenceResponse(String username, boolean lanAvailable) {
        this.username     = username;
        this.lanAvailable = lanAvailable;
    }

    public String  getUsername()     { return username; }
    public boolean isLanAvailable()  { return lanAvailable; }
}
