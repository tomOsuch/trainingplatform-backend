package pl.tomaszosuch.trainingplatform_backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.tomaszosuch.trainingplatform_backend.enums.CategoryIcon;

public class IconNameValidator implements ConstraintValidator<ValidIconName, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (value == null || CategoryIcon.isAllowed(value)) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        "Nieznana ikona kategorii. Dozwolone: " + CategoryIcon.allowedAsText())
                .addConstraintViolation();
        return false;
    }
}
