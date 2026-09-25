package dev.lucas.user_microservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    record Rule(String name, int limit, Duration window) {
    }

    static final Map<String, Rule> RULES = Map.of(
            "/auth/users/login", new Rule("login", 5, Duration.ofMinutes(1)),
            "/users/register", new Rule("register", 10, Duration.ofHours(1)),
            "/auth/password/forgot", new Rule("forgot", 3, Duration.ofMinutes(15)),
            "/auth/password/reset", new Rule("reset", 5, Duration.ofMinutes(15)));

    private final RateLimiter rateLimiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !RULES.containsKey(path(request));
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Rule rule = RULES.get(path(request));
        String key = rule.name() + ":" + request.getRemoteAddr();
        long retryAfter = rateLimiter.retryAfterSeconds(key, rule.limit(), rule.window());

        if (retryAfter > 0) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\":429,\"message\":\"Muitas tentativas. Tente novamente em "
                    + retryAfter + " segundos.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
