package com.example.im.service;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Online user registry.
 * Prefers Redisson (Redis); falls back to in-memory ConcurrentHashMap for POC if Redis unavailable.
 */
@Slf4j
@Service
public class OnlineUserRegistry {

    private final RedissonClient redisson;
    private final ConcurrentHashMap<String, Long> localFallback = new ConcurrentHashMap<>();

    public OnlineUserRegistry(RedissonClient redisson) {
        this.redisson = redisson;
    }

    public void userOnline(String userId) {
        try {
            RMap<String, Long> map = redisson.getMap("im:online");
            map.put(userId, System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("Redis unavailable, using local fallback: {}", e.getMessage());
            localFallback.put(userId, System.currentTimeMillis());
        }
    }

    public void userOffline(String userId) {
        try {
            RMap<String, Long> map = redisson.getMap("im:online");
            map.remove(userId);
        } catch (Exception e) {
            localFallback.remove(userId);
        }
    }

    public boolean isOnline(String userId) {
        try {
            RMap<String, Long> map = redisson.getMap("im:online");
            return map.containsKey(userId);
        } catch (Exception e) {
            return localFallback.containsKey(userId);
        }
    }
}
