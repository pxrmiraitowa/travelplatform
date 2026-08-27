package com.travelplatform.user.security;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String token, Date expiration) {
        blacklist.put(token, expiration.toInstant());
    }

    public boolean isBlacklisted(String token) {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        Instant expiry = blacklist.get(token);
        return expiry != null && expiry.isAfter(now);
    }
}
