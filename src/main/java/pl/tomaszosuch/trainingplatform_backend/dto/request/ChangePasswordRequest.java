package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Aktualne hasło jest wymagane")
        String currentPassword,
        @NotBlank(message = "Nowe hasło jest wymagane")
        @Size(min = 8, message = "Nowe hasło musi mieć co najmniej 8 znaków")
        String newPassword,
        @NotBlank(message = "Potwierdzenie hasła jest wymagane")
        String confirmPassword
) {}