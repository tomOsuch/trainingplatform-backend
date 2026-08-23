package pl.tomaszosuch.trainingplatform_backend.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Imię jest wymagane")
        @Size(max = 100, message = "Imię może mieć maksymalnie 100 znaków")
        String firstName,
        @NotBlank(message = "Nazwisko jest wymagane")
        @Size(max = 100, message = "Nazwisko może mieć maksymalnie 100 znaków")
        String lastName,
        @Past(message = "Data urodzenia musi być z przeszłości")
        LocalDate birthDate
) {

}