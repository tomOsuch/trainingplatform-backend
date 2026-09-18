package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CooperationInviteRequest(

        @NotBlank(message = "Adres e-mail jest wymagany")
        @Email(message = "Niepoprawny adres e-mail")
        @Size(max = 255, message = "Adres e-mail może mieć maksymalnie 255 znaków")
        String email
) {
}
