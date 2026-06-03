package pl.tomaszosuch.trainingplatform_backend.dto.request;

import org.hibernate.validator.constraints.EAN;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank(message = "Imię jest wymagane")
    @Size(max = 100, message = "Imię może mieć maksymalnie 100 znaków")
    String firstName,
    @NotBlank(message = "Nazwisko jest wymagane")
    @Size(max = 100, message = "Nazwisko może mieć maksymalnie 100 znaków")
    String lastName,
    @NotBlank(message = "Email jest wymagany")
    @EAN(message = "Nieprawidłowy format email")
    String email,
    @NotBlank(message = "Hasło jest wymagane")
    @Size(min = 8, message = "Hasło musi mieć co najmniej 8 znaków")
    String password
) {

}
