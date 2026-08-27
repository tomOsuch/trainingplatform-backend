package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest (

        @NotBlank(message = "Adres e-mail jest wymagany")
        @Email(message = "Niepoprawny format adresu e-mail")
        String email
) {

}
