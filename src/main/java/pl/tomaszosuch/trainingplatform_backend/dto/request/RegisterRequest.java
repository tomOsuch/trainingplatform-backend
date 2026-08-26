package pl.tomaszosuch.trainingplatform_backend.dto.request;

import jakarta.validation.constraints.Email;
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
    @Email(message = "Nieprawidłowy format email")
    String email,
    @NotBlank(message = "Hasło jest wymagane")
    @Size(min = 8, message = "Hasło musi mieć co najmniej 8 znaków")
    String password,
    @NotBlank(message = "Token zaproszenia jest wymagany")
    String token
) {

}
