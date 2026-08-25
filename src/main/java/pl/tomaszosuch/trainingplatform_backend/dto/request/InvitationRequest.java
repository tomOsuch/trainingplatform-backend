package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

public record InvitationRequest(

        @NotBlank(message = "Adres e-mail jest wymagany")
        @Email(message = "Niepoprawny format adresu e-mail")
        @Size(max = 255, message = "Adres e-mail może mieć maksymalnie 255 znaków")
        String email,

        Role role
) {

    public Role roleOrDefault() {
        return role != null ? role : Role.USER;
    }
}