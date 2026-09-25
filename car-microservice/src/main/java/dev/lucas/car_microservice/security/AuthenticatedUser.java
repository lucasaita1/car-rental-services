package dev.lucas.car_microservice.security;

public record AuthenticatedUser(Long id, String name, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public boolean canActFor(Long userId) {
        return isAdmin() || id.equals(userId);
    }
}
