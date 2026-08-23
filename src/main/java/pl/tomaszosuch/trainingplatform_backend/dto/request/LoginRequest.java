package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Adres e-mail jest wymagany")
        @Email(message = "Nieprawidłowy format adresu e-mail")
        String email,
        @NotBlank(message = "Hasło jest wymagane")
        String password
) {

}