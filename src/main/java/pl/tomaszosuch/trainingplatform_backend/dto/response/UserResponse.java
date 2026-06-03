package pl.tomaszosuch.trainingplatform_backend.dto.response;

import pl.tomaszosuch.trainingplatform_backend.enums.Role;

public record UserResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    Role role
) {

}
