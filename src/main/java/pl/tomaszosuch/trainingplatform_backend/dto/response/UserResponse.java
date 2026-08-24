package pl.tomaszosuch.trainingplatform_backend.dto.response;

import pl.tomaszosuch.trainingplatform_backend.enums.Role;

import java.time.LocalDate;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        LocalDate birthDate,
        Role role
) {

}
