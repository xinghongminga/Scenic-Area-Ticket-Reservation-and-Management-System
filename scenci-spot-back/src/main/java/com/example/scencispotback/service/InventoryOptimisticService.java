package com.example.scencispotback.service;

import com.example.scencispotback.domain.TicketInventoryRow;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;

@Service
public class InventoryOptimisticService {

    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
        "local v=redis.call('GET', KEYS[1]);"
            + "if not v then return -2 end;"
            + "local remain=tonumber(v);"
            + "local qty=tonumber(ARGV[1]);"
            + "if remain<qty then return 0 end;"
            + "return redis.call('DECRBY', KEYS[1], qty);",
        Long.class
    );

    private static final Duration KEY_TTL = Duration.ofHours(12);

    private final StringRedisTemplate redisTemplate;

    public InventoryOptimisticService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean reserve(Long ticketId, LocalDate visitDate, Long timeslotId, int qty, TicketInventoryRow inventory) {
        String key = key(ticketId, visitDate, timeslotId);
        int remain = Math.max(0, nvl(inventory.getTotalQty()) - nvl(inventory.getSoldQty()) - nvl(inventory.getLockedQty()));
        redisTemplate.opsForValue().setIfAbsent(key, String.valueOf(remain), KEY_TTL);
        Long result = redisTemplate.execute(RESERVE_SCRIPT, Collections.singletonList(key), String.valueOf(qty));
        if (result == null) {
            return false;
        }
        if (result >= 0) {
            redisTemplate.expire(key, KEY_TTL);
            return true;
        }
        if (result == -2) {
            redisTemplate.opsForValue().set(key, String.valueOf(remain), KEY_TTL);
            Long retry = redisTemplate.execute(RESERVE_SCRIPT, Collections.singletonList(key), String.valueOf(qty));
            if (retry != null && retry >= 0) {
                redisTemplate.expire(key, KEY_TTL);
                return true;
            }
        }
        return false;
    }

    public void release(Long ticketId, LocalDate visitDate, Long timeslotId, int qty) {
        String key = key(ticketId, visitDate, timeslotId);
        redisTemplate.opsForValue().increment(key, qty);
        redisTemplate.expire(key, KEY_TTL);
    }

    private String key(Long ticketId, LocalDate visitDate, Long timeslotId) {
        return "inventory:remain:" + ticketId + ":" + visitDate + ":" + timeslotId;
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}