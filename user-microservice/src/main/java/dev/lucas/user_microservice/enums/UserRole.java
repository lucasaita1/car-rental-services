package dev.lucas.user_microservice.enums;

public enum UserRole {

    USER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
