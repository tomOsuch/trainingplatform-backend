package pl.tomaszosuch.trainingplatform_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = IconNameValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidIconName {

    String message() default "Nieznana ikona kategorii";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
