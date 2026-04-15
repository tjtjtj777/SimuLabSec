package com.simulab.common.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class SecurityThrottleService {

    private static final Logger log = LoggerFactory.getLogger(SecurityThrottleService.class);
    private static final DefaultRedisScript<Long> INCR_WITH_EXPIRE_SCRIPT = new DefaultRedisScript<>(
        "local current = redis.call('INCR', KEYS[1]); "
            + "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
            + "return current;",
        Long.class
    );

    @Nullable
    private final StringRedisTemplate stringRedisTemplate;
    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    public SecurityThrottleService(@Nullable StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long incrementAndGet(String key, Duration window) {
        Long redisCount = tryIncrementRedis(key, window);
        if (redisCount != null) {
            return redisCount;
        }
        return incrementLocal(key, window);
    }

    public boolean isBlocked(String key) {
        if (stringRedisTemplate != null) {
            try {
                return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
            } catch (Exception ex) {
                log.warn("[security-throttle] redis hasKey fallback key={} reason={}", key, ex.getMessage());
            }
        }
        LocalCounter local = localCounters.get(key);
        return local != null && !local.isExpired();
    }

    public void block(String key, Duration ttl) {
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set(key, "1", ttl);
                return;
            } catch (Exception ex) {
                log.warn("[security-throttle] redis block fallback key={} reason={}", key, ex.getMessage());
            }
        }
        localCounters.put(key, LocalCounter.blocked(ttl));
    }

    public long ttlSeconds(String key) {
        if (stringRedisTemplate != null) {
            try {
                Long ttl = stringRedisTemplate.getExpire(key);
                return ttl == null ? -1L : Math.max(ttl, 0L);
            } catch (Exception ex) {
                log.warn("[security-throttle] redis ttl fallback key={} reason={}", key, ex.getMessage());
            }
        }
        LocalCounter local = localCounters.get(key);
        if (local == null || local.isExpired()) {
            return -1L;
        }
        return Math.max((local.expireAtMs - System.currentTimeMillis()) / 1000L, 0L);
    }

    public void clear(String key) {
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.delete(key);
                return;
            } catch (Exception ex) {
                log.warn("[security-throttle] redis clear fallback key={} reason={}", key, ex.getMessage());
            }
        }
        localCounters.remove(key);
    }

    @Nullable
    private Long tryIncrementRedis(String key, Duration window) {
        if (stringRedisTemplate == null) {
            return null;
        }
        try {
            return stringRedisTemplate.execute(
                INCR_WITH_EXPIRE_SCRIPT,
                java.util.Collections.singletonList(key),
                String.valueOf(window.getSeconds())
            );
        } catch (Exception ex) {
            log.warn("[security-throttle] redis increment fallback key={} reason={}", key, ex.getMessage());
            return null;
        }
    }

    private long incrementLocal(String key, Duration window) {
        long now = System.currentTimeMillis();
        LocalCounter counter = localCounters.compute(key, (k, previous) -> {
            if (previous == null || previous.expireAtMs <= now) {
                return LocalCounter.counter(window);
            }
            previous.count.incrementAndGet();
            return previous;
        });
        return counter.count.get();
    }

    private static final class LocalCounter {
        private final AtomicLong count;
        private final long expireAtMs;

        private LocalCounter(AtomicLong count, long expireAtMs) {
            this.count = count;
            this.expireAtMs = expireAtMs;
        }

        static LocalCounter counter(Duration window) {
            return new LocalCounter(new AtomicLong(1L), System.currentTimeMillis() + window.toMillis());
        }

        static LocalCounter blocked(Duration window) {
            return new LocalCounter(new AtomicLong(1L), System.currentTimeMillis() + window.toMillis());
        }

        boolean isExpired() {
            return expireAtMs <= System.currentTimeMillis();
        }
    }
}
