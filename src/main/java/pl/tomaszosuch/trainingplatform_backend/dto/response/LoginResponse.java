package pl.tomaszosuch.trainingplatform_backend.dto.response;

import pl.tomaszosuch.trainingplatform_backend.enums.Role;

public record LoginResponse(
    String token,
    String type,
    Long userId,
    String email,
    Role role
) {
    public LoginResponse(String token, Long userId, String email, Role role) {
        this(token, "Bearer", userId, email, role);
    }
}
