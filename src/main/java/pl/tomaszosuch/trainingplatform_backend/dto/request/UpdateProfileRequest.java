package pl.tomaszosuch.trainingplatform_backend.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "First name cannot be blank")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,
        @NotBlank(message = "Last name cannot be blank")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate
) {

}