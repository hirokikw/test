package com.example.greeting.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Bean Validationから呼ばれ、名前の必須と文字数を判定する。
 */
public class GreetingNameValidator implements ConstraintValidator<GreetingName, String> {

    public static final int MAX_LENGTH = 50;

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        if (value == null || value.strip().isEmpty()) {
            return reject(context, "名前を入力してください。");
        }
        if (value.strip().length() > MAX_LENGTH) {
            return reject(context, "名前は50文字以内で入力してください。");
        }
        return true;
    }

    private boolean reject(final ConstraintValidatorContext context, final String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
