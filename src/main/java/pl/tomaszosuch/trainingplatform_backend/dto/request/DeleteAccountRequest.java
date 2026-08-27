package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(

        @NotBlank(message = "Hasło jest wymagane")
        String password
) {

}
