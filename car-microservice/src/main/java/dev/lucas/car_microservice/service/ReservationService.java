package dev.lucas.car_microservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservationService {

    public static final Duration HOLD_TTL = Duration.ofMinutes(10);
    static final String CAR_PREFIX = "car:hold:";
    static final String USER_PREFIX = "car:hold-user:";

    private static final RedisScript<Long> DELETE_IF_EQUALS = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;

    public Optional<Long> hold(Long carId, Long userId) {
        String carKey = CAR_PREFIX + carId;
        String me = userId.toString();
        try {
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(carKey, me, HOLD_TTL))) {
                String holder = redis.opsForValue().get(carKey);
                if (holder == null && Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(carKey, me, HOLD_TTL))) {
                    holder = me;
                }
                if (holder == null || !holder.equals(me)) {
                    return Optional.of(holder == null ? -1L : Long.parseLong(holder));
                }
                redis.expire(carKey, HOLD_TTL);
            }

            String previous = redis.opsForValue().getAndSet(USER_PREFIX + userId, carId.toString());
            redis.expire(USER_PREFIX + userId, HOLD_TTL);
            if (previous != null && !previous.equals(carId.toString())) {
                redis.execute(DELETE_IF_EQUALS, List.of(CAR_PREFIX + previous), me);
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de reservas indisponível.");
        }
    }

    public void release(Long carId, Long userId) {
        try {
            redis.execute(DELETE_IF_EQUALS, List.of(CAR_PREFIX + carId), userId.toString());
            redis.execute(DELETE_IF_EQUALS, List.of(USER_PREFIX + userId), carId.toString());
        } catch (RuntimeException ignored) {
        }
    }

    public Instant expiresAt(Long carId) {
        Long seconds = redis.getExpire(CAR_PREFIX + carId);
        return Instant.now().plusSeconds(seconds == null || seconds < 0 ? HOLD_TTL.toSeconds() : seconds);
    }

    public Set<Long> heldCarIds(Collection<Long> carIds) {
        Set<Long> held = new HashSet<>();
        if (carIds.isEmpty()) {
            return held;
        }
        try {
            List<Long> ids = List.copyOf(carIds);
            List<String> holders = redis.opsForValue().multiGet(ids.stream().map(id -> CAR_PREFIX + id).toList());
            for (int i = 0; holders != null && i < ids.size(); i++) {
                if (holders.get(i) != null) {
                    held.add(ids.get(i));
                }
            }
        } catch (RuntimeException ignored) {
        }
        return held;
    }
}
